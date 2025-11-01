const { onDocumentCreated, onDocumentUpdated } = require('firebase-functions/v2/firestore');
const { logger } = require('firebase-functions');
const admin = require('firebase-admin');

// --- KHỞI TẠO CHÍNH (Chỉ cần 1 lần) ---
admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

// Constants
const USER_NOTIFICATIONS_COLLECTION = 'notifications'; // Update this to your collection name

// -----------------------------------------------------------
// CLOUD FUNCTION: XỬ LÝ ĐẶT HÀNG (TRỪ TỒN KHO & VOUCHER)
// -----------------------------------------------------------
exports.handleNewOrder = onDocumentCreated('orders/{orderId}', async (event) => {
    const snap = event.data;
    const order = snap.data();
    const orderId = event.params.orderId;

    console.log(`Đơn hàng mới được tạo: ${orderId}. Bắt đầu Transaction.`);

    try {
        await db.runTransaction(async (transaction) => {

            // --- BƯỚC 1: THỰC HIỆN TẤT CẢ CÁC THAO TÁC ĐỌC (READS) ---

            const itemsToUpdate = [];

            // Đọc tất cả Product Documents cần thiết
            for (const item of order.items) {
                const productRef = db.collection('products').doc(item.productId);
                const productDoc = await transaction.get(productRef); // Đọc

                if (!productDoc.exists) {
                    throw new Error(`Sản phẩm không tồn tại: ${item.productId}`);
                }

                itemsToUpdate.push({
                    item: item,
                    docRef: productRef,
                    productData: productDoc.data()
                });
            }

            // Đọc Voucher Document (nếu có)
            let voucherData;
            let voucherRef;

            if (order.voucherCode && order.voucherCode !== "NULL" && order.voucherCode.length > 0) {
                const voucherCode = order.voucherCode;
                const voucherQuery = await transaction.get(
                    db.collection('vouchers').where('code', '==', voucherCode).limit(1)
                ); // Đọc

                if (!voucherQuery.empty) {
                    voucherData = voucherQuery.docs[0].data();
                    voucherRef = voucherQuery.docs[0].ref;
                }
            }

            // --- BƯỚC 2: XỬ LÝ LOGIC & TÍNH TOÁN (KHÔNG CÓ READ/WRITE DB) ---

            // Xử lý tồn kho và tạo bản cập nhật
            for (const updateEntry of itemsToUpdate) {
                const { item, productData } = updateEntry;
                const quantityToSubtract = item.quantity;
                const variantId = item.variantId;

                let variants = productData.variants || [];
                let variantFound = false;

                variants = variants.map(variant => {
                    if (variant.variantId === variantId) {
                        variantFound = true;
                        const currentStock = variant.quantity || 0;

                        if (currentStock < quantityToSubtract) {
                            throw new Error(`Over-selling: Tồn kho ${variantId} không đủ (${currentStock} < ${quantityToSubtract})`);
                        }
                        variant.quantity = currentStock - quantityToSubtract;
                    }
                    return variant;
                });

                if (!variantFound) {
                    throw new Error(`Biến thể không tồn tại: ${variantId}`);
                }

                updateEntry.newVariants = variants;
            }

            // Xử lý Voucher (tạo bản cập nhật)
            let newTimesUsed;
            if (voucherRef) {
                const currentTimesUsed = voucherData.timesUsed || 0;
                const maxUsageLimit = voucherData.maxUsageLimit || Infinity;

                if (currentTimesUsed >= maxUsageLimit) {
                    throw new Error(`Voucher đã hết lượt sử dụng.`);
                }
                newTimesUsed = admin.firestore.FieldValue.increment(1);
            }

            // --- BƯỚC 3: THỰC HIỆN TẤT CẢ CÁC THAO TÁC GHI (WRITES) ---

            // Ghi cập nhật Tồn kho
            for (const updateEntry of itemsToUpdate) {
                transaction.update(updateEntry.docRef, { variants: updateEntry.newVariants });
            }

            // Ghi cập nhật Voucher
            if (voucherRef) {
                transaction.update(voucherRef, { timesUsed: newTimesUsed });
            }

            // ✅ Cập nhật Trạng thái Order có điều kiện
            // Chỉ đổi thành 'PROCESSING' nếu trạng thái hiện tại là 'PENDING'.
            // Sẽ không ghi đè 'PAID' của VNPAY.
            if (order.orderStatus === "PENDING") {
                transaction.update(snap.ref, {
                    orderStatus: "PROCESSING",
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });
            }

            return "Transaction thành công: Tồn kho và Voucher đã được cập nhật.";
        });

        console.log("Transaction đã hoàn tất.");
        return null;

    } catch (error) {
        console.error("LỖI TRANSACTION (TỒN KHO/VOUCHER) cho đơn hàng:", orderId, error.message);

        // Cập nhật Order để ghi nhận lỗi
        return db.collection('orders').doc(orderId).update({
            orderStatus: "FAILED_INVENTORY_UPDATE",
            errorMessage: `Transaction thất bại: ${error.message}`
        });
    }
});

// ============================================================================
// 🔔 TRIGGER - When Order Status Changes, Send Notification
// ============================================================================
exports.onOrderStatusChanged = onDocumentUpdated(
    {
        document: 'orders/{orderId}',
        region: 'asia-east1',
    },
    async (event) => {
        const before = event.data.before.data();
        const after = event.data.after.data();
        const orderId = event.params.orderId;

        // Check if status actually changed
        if (before.orderStatus === after.orderStatus) {
            logger.info('Status not changed, skipping notification');
            return null;
        }

        const oldStatus = before.orderStatus;
        const newStatus = after.orderStatus;
        const userId = after.userId;

        logger.info(`Order ${orderId} status changed: ${oldStatus} -> ${newStatus}`);

        // Get notification content based on status
        const notificationContent = getOrderStatusNotification(newStatus, orderId);

        if (!notificationContent) {
            logger.info(`No notification needed for status: ${newStatus}`);
            return null;
        }

        try {
            // Build FCM message
            const message = {
                topic: `user_${userId}`, // Or use direct token
                notification: {
                    title: notificationContent.title,
                    body: notificationContent.body,
                },
                data: {
                    title: String(notificationContent.title),
                    body: String(notificationContent.body),
                    type: 'ORDER',
                    actionType: 'OPEN_ORDER',
                    actionData: String(orderId),
                    icon: 'cart',
                    priority: String(notificationContent.priority),
                    imageUrl: '',
                },
                android: {
                    priority: 'high',
                    notification: {
                        sound: 'default',
                        channelId: 'fcm_default_channel',
                        icon: 'ic_notification',
                        color: notificationContent.color,
                    },
                },
            };

            // Send FCM
            const response = await messaging.send(message);
            logger.info(`Sent order status notification: ${response}`);

            // Save to user's notification collection
            await saveOrderNotificationToUser(userId, orderId, notificationContent);

            return { success: true, messageId: response };
        } catch (error) {
            logger.error('Error sending order notification:', error);
            return { success: false, error: error.message };
        }
    }
);

// ============================================================================
// 📝 HELPER - Get notification content based on order status
// ============================================================================
function getOrderStatusNotification(status, orderId) {
    const orderShortId = orderId.substring(0, 8).toUpperCase();

    switch (status) {
        case 'CONFIRMED':
            return {
                title: '✅ Đơn hàng đã được xác nhận',
                body: `Đơn hàng #${orderShortId} đã được xác nhận và đang được chuẩn bị`,
                priority: 2,
                color: '#2196F3',
            };

        case 'PROCESSING':
            return {
                title: '📦 Đơn hàng đang xử lý',
                body: `Đơn hàng #${orderShortId} đang được đóng gói`,
                priority: 1,
                color: '#9C27B0',
            };

        case 'SHIPPING':
            return {
                title: '🚚 Đơn hàng đang giao',
                body: `Đơn hàng #${orderShortId} đang trên đường giao đến bạn`,
                priority: 2,
                color: '#00BCD4',
            };

        case 'DELIVERED':
            return {
                title: '✓ Đơn hàng đã giao thành công',
                body: `Đơn hàng #${orderShortId} đã được giao đến bạn. Cảm ơn bạn đã mua hàng!`,
                priority: 2,
                color: '#4CAF50',
            };

        case 'COMPLETED':
            return {
                title: '🎉 Đơn hàng hoàn tất',
                body: `Đơn hàng #${orderShortId} đã hoàn tất. Hãy đánh giá sản phẩm nhé!`,
                priority: 1,
                color: '#4CAF50',
            };

        case 'CANCELLED':
            return {
                title: '❌ Đơn hàng đã bị hủy',
                body: `Đơn hàng #${orderShortId} đã bị hủy`,
                priority: 2,
                color: '#F44336',
            };

        case 'REFUNDED':
            return {
                title: '💰 Đơn hàng đã hoàn tiền',
                body: `Đơn hàng #${orderShortId} đã được hoàn tiền`,
                priority: 2,
                color: '#607D8B',
            };

        case 'PAID':
            return {
                title: '💳 Thanh toán thành công',
                body: `Đơn hàng #${orderShortId} đã được thanh toán thành công`,
                priority: 2,
                color: '#4CAF50',
            };

        default:
            return null; // Don't send notification for other statuses
    }
}

// ============================================================================
// 💾 HELPER - Save order notification to user collection
// ============================================================================
async function saveOrderNotificationToUser(userId, orderId, content) {
    const notificationRef = db.collection(USER_NOTIFICATIONS_COLLECTION).doc();

    await notificationRef.set({
        notificationId: notificationRef.id,
        userId: userId,
        title: content.title,
        body: content.body,
        imageUrl: '',
        type: 'ORDER',
        actionType: 'OPEN_ORDER',
        actionData: orderId,
        icon: 'cart',
        priority: content.priority,
        isRead: false,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        senderName: 'Hệ thống',
        extraData: {
            orderId: orderId,
        },
    });

    logger.info(`Saved order notification to user ${userId}`);
}