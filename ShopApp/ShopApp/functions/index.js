const { onDocumentCreated, onDocumentUpdated } = require('firebase-functions/v2/firestore');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const { onRequest } = require('firebase-functions/v2/https');
const { setGlobalOptions } = require('firebase-functions/v2');
const { logger } = require('firebase-functions');
const admin = require('firebase-admin');

// --- KHỞI TẠO CHÍNH (Chỉ cần 1 lần) ---
admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

// --- CÀI ĐẶT TOÀN CỤC CHO FUNCTIONS ---
setGlobalOptions({
    region: 'asia-east1',
    maxInstances: 10,
    timeoutSeconds: 540,
    memory: '512MiB',
});

// --- TÊN CÁC COLLECTION ---
const SCHEDULED_NOTIFICATIONS_COLLECTION = 'scheduledNotifications';
const USER_NOTIFICATIONS_COLLECTION = 'notifications';
const ORDERS_COLLECTION = 'orders';

// ============================================================================
// 🔔 TRIGGER - KHI TRẠNG THÁI ĐƠN HÀNG THAY ĐỔI, GỬI THÔNG BÁO
// ============================================================================
exports.onOrderStatusChanged = onDocumentUpdated(ORDERS_COLLECTION + '/{orderId}', async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    const orderId = event.params.orderId;

    if (before.orderStatus === after.orderStatus) {
        logger.info(`Order ${orderId}: Status not changed, skipping.`);
        return null;
    }

    // ⭐️ FIX: Lấy userId của chủ đơn hàng từ dữ liệu TRƯỚC khi thay đổi.
    // Điều này đảm bảo thông báo luôn được gửi cho đúng khách hàng,
    // ngay cả khi trang admin có vô tình ghi đè userId.
    const customerId = before.userId;
    if (!customerId) {
        logger.error(`Order ${orderId}: Missing 'userId' in order data. Cannot send notification.`);
        return null;
    }

    logger.info(`Order ${orderId}: Status changed from '${before.orderStatus}' to '${after.orderStatus}'. Notifying user: ${customerId}`);

    const notificationContent = getOrderStatusNotification(after.orderStatus, orderId);
    if (!notificationContent) {
        logger.info(`No notification defined for status: ${after.orderStatus}`);
        return null;
    }

    try {
        // ⭐️ GỬI RIÊNG CHO CHỦ ĐƠN HÀNG - KHÔNG BROADCAST
        const topic = `user_${customerId}`;
        const message = {
            topic: topic,  // ✅ Gửi CHỈNH XÁC đến topic của user
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
                userId: String(customerId),  // ⭐️ THÊM DÒNG NÀY - gửi userId cho Android app lưu đúng user
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

        const response = await messaging.send(message);
        logger.info(`✅ Sent order notification ONLY to owner [${customerId}] for order [${orderId}]`, { messageId: response });

        // ⭐️ Chỉ lưu notification vào collection của CHỦĐƠN HÀNG
        await saveOrderNotificationToUser(customerId, orderId, notificationContent);

        return { success: true, messageId: response, notifiedUser: customerId };

    } catch (error) {
        logger.error(`❌ Error sending order notification for user ${customerId}:`, error);
        return { success: false, error: error.message };
    }
});


// ============================================================================
// 📝 HELPER - Lấy nội dung thông báo dựa trên trạng thái đơn hàng
// ============================================================================
function getOrderStatusNotification(status, orderId) {
    const orderShortId = orderId.substring(0, 8).toUpperCase();

    switch (status) {
        case 'CONFIRMED': return { title: '✅ Đơn hàng đã được xác nhận', body: `Đơn hàng #${orderShortId} đã được xác nhận và đang được chuẩn bị.`, priority: 2, color: '#2196F3' };
        case 'PROCESSING': return { title: '📦 Đơn hàng đang xử lý', body: `Đơn hàng #${orderShortId} đang được đóng gói.`, priority: 1, color: '#9C27B0' };
        case 'SHIPPING': return { title: '🚚 Đơn hàng đang được giao', body: `Đơn hàng #${orderShortId} đang trên đường giao đến bạn.`, priority: 2, color: '#00BCD4' };
        case 'DELIVERED': return { title: '✓ Đơn hàng đã giao thành công', body: `Đơn hàng #${orderShortId} đã được giao đến bạn. Cảm ơn bạn đã mua hàng!`, priority: 2, color: '#4CAF50' };
        case 'COMPLETED': return { title: '🎉 Đơn hàng đã hoàn tất', body: `Đơn hàng #${orderShortId} đã hoàn tất. Hãy đánh giá sản phẩm nhé!`, priority: 1, color: '#4CAF50' };
        case 'CANCELLED': return { title: '❌ Đơn hàng đã bị hủy', body: `Đơn hàng #${orderShortId} của bạn đã bị hủy.`, priority: 2, color: '#F44336' };
        case 'REFUNDED': return { title: '💰 Đơn hàng đã được hoàn tiền', body: `Đơn hàng #${orderShortId} đã được hoàn tiền.`, priority: 2, color: '#607D8B' };
        case 'PAID': return { title: '💳 Thanh toán thành công', body: `Đơn hàng #${orderShortId} đã được thanh toán thành công.`, priority: 2, color: '#4CAF50' };
        default: return null;
    }
}

// ============================================================================
// 💾 HELPER - Lưu thông báo đơn hàng vào collection của người dùng
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
        extraData: { orderId: orderId },
    });
    logger.info(`✅ Saved order notification to user's collection: ${userId}`);
}


// ============================================================================
//          CÁC HÀM GỬI THÔNG BÁO CHUNG (TỪ WEB ADMIN) - BROADCAST
// ============================================================================

function buildFCMMessage(notification) {
    const message = {
        topic: 'all_users',  // ✅ Gửi cho TẤT CẢ mọi người
        notification: {
            title: notification.title || 'Thông báo mới',
            body: notification.body || '',
        },
        data: {
            title: String(notification.title || ''),
            body: String(notification.body || ''),
            type: String(notification.type || 'SYSTEM'),
            actionType: String(notification.actionType || 'NONE'),
            actionData: String(notification.actionData || ''),
            icon: String(notification.icon || 'bell'),
            priority: String(notification.priority || 1),
            imageUrl: String(notification.imageUrl || ''),
            // ⭐️ Không gửi userId ở broadcast - vì là thông báo chung
        },
        android: {
            priority: (notification.priority || 1) >= 2 ? 'high' : 'normal',
            notification: {
                sound: 'default',
                channelId: 'fcm_default_channel',
                icon: 'ic_notification',
                color: '#667eea',
                priority: (notification.priority || 1) >= 2 ? 'high' : 'default',
            },
        },
    };
    if (notification.imageUrl) {
        message.notification.imageUrl = notification.imageUrl;
    }
    return message;
}

async function sendFCMNotification(notification) {
    try {
        const message = buildFCMMessage(notification);
        logger.info('Sending broadcast FCM with data:', { title: notification.title, type: notification.type });
        const response = await messaging.send(message);
        logger.info('✅ Broadcast FCM sent successfully:', response);
        return { success: true, messageId: response, error: null };
    } catch (error) {
        logger.error('❌ Broadcast FCM error:', error);
        return { success: false, messageId: null, error: error.message };
    }
}

async function saveNotificationToAllUsers(notification) {
    try {
        const usersSnapshot = await db.collection('users').limit(1000).get();
        const batch = db.batch();
        let count = 0;

        for (const userDoc of usersSnapshot.docs) {
            const notificationRef = db.collection(USER_NOTIFICATIONS_COLLECTION).doc();
            batch.set(notificationRef, {
                notificationId: notificationRef.id,
                userId: userDoc.id,
                title: notification.title || '',
                body: notification.body || '',
                imageUrl: notification.imageUrl || '',
                type: notification.type || 'SYSTEM',
                actionType: notification.actionType || 'NONE',
                actionData: notification.actionData || '',
                icon: notification.icon || 'bell',
                priority: notification.priority || 1,
                isRead: false,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                senderName: 'Hệ thống',
                extraData: {},
            });

            count++;
            if (count % 500 === 0) {
                await batch.commit();
            }
        }

        if (count % 500 > 0) {
            await batch.commit();
        }

        logger.info(`✅ Saved broadcast notification to ${usersSnapshot.size} users.`);
    } catch (error) {
        logger.error('❌ Error saving broadcast notification to all users:', error);
    }
}

async function updateScheduledNotificationStatus(docId, status, additionalData = {}) {
    await db.collection(SCHEDULED_NOTIFICATIONS_COLLECTION).doc(docId).update({
        status,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        ...additionalData,
    });
}

// ============================================================================
// ⏰ SCHEDULER - Kiểm tra thông báo đã lên lịch và gửi
// ============================================================================
exports.checkScheduledNotifications = onSchedule('every 5 minutes', async (event) => {
    const now = Date.now();
    const snapshot = await db.collection(SCHEDULED_NOTIFICATIONS_COLLECTION)
        .where('status', '==', 'PENDING').where('scheduleTime', '<=', now).limit(100).get();

    if (snapshot.empty) return null;

    logger.info(`Found ${snapshot.size} scheduled notifications to process.`);
    for (const doc of snapshot.docs) {
        const notification = { id: doc.id, ...doc.data() };
        const sendResult = await sendFCMNotification(notification);
        if (sendResult.success) {
            await updateScheduledNotificationStatus(doc.id, 'SENT', {
                sentTime: admin.firestore.FieldValue.serverTimestamp(),
                messageId: sendResult.messageId
            });
            await saveNotificationToAllUsers(notification);
        } else {
            await updateScheduledNotificationStatus(doc.id, 'FAILED', {
                failedTime: admin.firestore.FieldValue.serverTimestamp(),
                errorMessage: sendResult.error
            });
        }
    }
    return { processed: snapshot.size };
});