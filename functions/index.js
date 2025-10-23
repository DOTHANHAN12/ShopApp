const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');

// --- KHỞI TẠO CHÍNH (Chỉ cần 1 lần) ---
admin.initializeApp();
const db = admin.firestore();

// -----------------------------------------------------------
// CLOUD FUNCTION: XỬ LÝ ĐẶT HÀNG COD (TRỪ TỒN KHO & VOUCHER)
// -----------------------------------------------------------
exports.handleNewOrder = onDocumentCreated('orders/{orderId}', async (event) => {
    const snap = event.data;
    const order = snap.data();
    const orderId = event.params.orderId;

    console.log(`Đơn hàng mới được tạo: ${orderId}. Bắt đầu Transaction.`);

    try {
        await db.runTransaction(async (transaction) => {

            // --- BƯỚC 1: THỰC HIỆN TẤT CẢ CÁC THAO TÁC ĐỌC (READS) ---

            // 1.1. Khởi tạo mảng lưu trữ các Document cần đọc và các bản cập nhật
            const itemsToUpdate = [];

            // Đọc tất cả Product Documents cần thiết
            for (const item of order.items) {
                const productRef = db.collection('products').doc(item.productId);
                const productDoc = await transaction.get(productRef); // Đọc

                if (!productDoc.exists) {
                    throw new Error(`Sản phẩm không tồn tại: ${item.productId}`);
                }

                // Lưu trữ dữ liệu và tham chiếu để xử lý sau
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

            // 2.1. Xử lý tồn kho và tạo bản cập nhật
            for (const updateEntry of itemsToUpdate) {
                const { item, productData } = updateEntry;
                const quantityToSubtract = item.quantity;
                const variantId = item.variantId;

                let variants = productData.variants || [];
                let variantFound = false;

                // Lặp và cập nhật số lượng
                variants = variants.map(variant => {
                    if (variant.variantId === variantId) {
                        variantFound = true;
                        const currentStock = variant.quantity || 0;

                        if (currentStock < quantityToSubtract) {
                            throw new Error(`Over-selling: Tồn kho ${variantId} không đủ (${currentStock} < ${quantityToSubtract})`);
                        }
                        variant.quantity = currentStock - quantityToSubtract; // Cập nhật số lượng trong mảng
                    }
                    return variant;
                });

                if (!variantFound) {
                    throw new Error(`Biến thể không tồn tại: ${variantId}`);
                }

                // Lưu bản cập nhật vào đối tượng updateEntry
                updateEntry.newVariants = variants;
            }

            // 2.2. Xử lý Voucher (tạo bản cập nhật)
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

            // 3.1. Ghi cập nhật Tồn kho
            for (const updateEntry of itemsToUpdate) {
                transaction.update(updateEntry.docRef, { variants: updateEntry.newVariants });
            }

            // 3.2. Ghi cập nhật Voucher
            if (voucherRef) {
                transaction.update(voucherRef, { timesUsed: newTimesUsed });
            }

            // 3.3. Cập nhật Trạng thái Order (Quan trọng: Phải là thao tác cuối cùng)
            transaction.update(snap.ref, {
                orderStatus: "PROCESSING",
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });

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