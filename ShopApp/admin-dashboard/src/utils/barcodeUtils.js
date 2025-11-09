// ============================================================================
// barcodeUtils.js - FIXED VERSION
// File: src/utils/barcodeUtils.js
// ============================================================================

import { collection, getDocs, doc, updateDoc, writeBatch } from 'firebase/firestore';
import { db } from '../firebaseConfig';

/**
 * HÀM TÍNH EAN-13 CHECKSUM
 */
const calculateEAN13Checksum = (digits12) => {
    let sum = 0;
    for (let i = 0; i < 12; i++) {
        const weight = i % 2 === 0 ? 1 : 3;
        sum += parseInt(digits12[i]) * weight;
    }
    const checksum = (10 - (sum % 10)) % 10;
    return String(checksum);
};

/**
 * HÀM GENERATE BARCODE TỪ PRODUCT ID
 */
export const generateBarcodeFromId = (productId) => {
    if (!productId) return null;
    
    let hash = 0;
    for (let i = 0; i < productId.length; i++) {
        const char = productId.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash;
    }

    hash = Math.abs(hash);
    const base12Digits = String(hash).padStart(12, '0').substring(0, 12);
    const checksum = calculateEAN13Checksum(base12Digits);

    return base12Digits + checksum;
};

/**
 * HÀM CHÍNH: INSERT BARCODE CHO TẤT CẢ SẢN PHẨM
 */
export const insertBarcodesForAllProducts = async () => {
    const BATCH_SIZE = 100;
    const result = {
        success: false,
        total: 0,
        updated: 0,
        skipped: 0,
        failed: 0,
        errors: []
    };

    try {
        console.log('🔄 Bắt đầu insert barcode cho tất cả sản phẩm...');

        const productsCollectionRef = collection(db, 'products');
        const productSnapshot = await getDocs(productsCollectionRef);

        if (productSnapshot.empty) {
            console.log('⚠️ Không có sản phẩm nào trong database!');
            result.success = true;
            return result;
        }

        result.total = productSnapshot.size;
        console.log(`📊 Tổng số sản phẩm: ${result.total}`);

        let batch = writeBatch(db);
        let batchCount = 0;

        for (const productDoc of productSnapshot.docs) {
            try {
                const product = productDoc.data();
                const productId = productDoc.id;

                // Kiểm tra xem đã có barcode chưa
                if (product.barcode && product.barcode.trim() !== '') {
                    console.log(`⏭️  Bỏ qua - ${productId} đã có barcode: ${product.barcode}`);
                    result.skipped++;
                    continue;
                }

                // Generate barcode mới
                const newBarcode = generateBarcodeFromId(productId);
                const docRef = doc(db, 'products', productId);

                // Thêm vào batch
                batch.update(docRef, {
                    barcode: newBarcode,
                    updatedAt: Date.now()
                });

                console.log(`✅ ${productId} → Barcode: ${newBarcode}`);
                result.updated++;
                batchCount++;

                // Commit batch khi đạt limit
                if (batchCount % BATCH_SIZE === 0) {
                    console.log(`💾 Committing batch (${batchCount} items)...`);
                    await batch.commit();
                    batch = writeBatch(db);
                    batchCount = 0;
                }

            } catch (err) {
                console.error(`❌ Lỗi xử lý sản phẩm ${productDoc.id}:`, err);
                result.failed++;
                result.errors.push({
                    productId: productDoc.id,
                    error: err.message
                });
            }
        }

        // Commit batch còn lại
        if (batchCount > 0) {
            console.log(`💾 Committing final batch (${batchCount} items)...`);
            await batch.commit();
        }

        result.success = true;
        console.log(`✅ Hoàn tất! Cập nhật: ${result.updated}, Bỏ qua: ${result.skipped}, Lỗi: ${result.failed}`);
        
        return result;

    } catch (err) {
        console.error('❌ LỖI LỚN khi insert barcode:', err);
        result.success = false;
        result.errors.push({
            global: err.message
        });
        return result;
    }
};

/**
 * HÀM INSERT BARCODE CHO MỘT SẢN PHẨM
 */
export const insertBarcodeForProduct = async (productId, customBarcode = null) => {
    try {
        const docRef = doc(db, 'products', productId);

        // Nếu có barcode tùy chỉnh, validate
        if (customBarcode) {
            if (!/^\d{12,13}$/.test(customBarcode)) {
                return {
                    success: false,
                    message: 'Barcode phải là 12 hoặc 13 chữ số'
                };
            }
            
            await updateDoc(docRef, {
                barcode: customBarcode,
                updatedAt: Date.now()
            });

            return {
                success: true,
                barcode: customBarcode,
                message: `✅ Cập nhật barcode thành công: ${customBarcode}`
            };
        }

        // Generate barcode tự động
        const generatedBarcode = generateBarcodeFromId(productId);
        await updateDoc(docRef, {
            barcode: generatedBarcode,
            updatedAt: Date.now()
        });

        return {
            success: true,
            barcode: generatedBarcode,
            message: `✅ Generate barcode thành công: ${generatedBarcode}`
        };

    } catch (err) {
        console.error(`❌ Lỗi insert barcode cho ${productId}:`, err);
        return {
            success: false,
            message: `❌ Lỗi: ${err.message}`
        };
    }
};

/**
 * HÀM XÓA TẤT CẢ BARCODE
 */
export const deleteAllBarcodes = async () => {
    const BATCH_SIZE = 100;
    const result = {
        success: false,
        deleted: 0,
        errors: []
    };

    try {
        console.log('🗑️  Xóa tất cả barcode từ database...');

        const productsCollectionRef = collection(db, 'products');
        const productSnapshot = await getDocs(productsCollectionRef);

        let batch = writeBatch(db);
        let batchCount = 0;

        for (const productDoc of productSnapshot.docs) {
            try {
                const docRef = doc(db, 'products', productDoc.id);
                batch.update(docRef, {
                    barcode: null,
                    updatedAt: Date.now()
                });

                result.deleted++;
                batchCount++;

                if (batchCount % BATCH_SIZE === 0) {
                    await batch.commit();
                    batch = writeBatch(db);
                    batchCount = 0;
                }
            } catch (err) {
                result.errors.push({
                    productId: productDoc.id,
                    error: err.message
                });
            }
        }

        if (batchCount > 0) {
            await batch.commit();
        }

        result.success = true;
        console.log(`✅ Đã xóa barcode từ ${result.deleted} sản phẩm`);
        return result;

    } catch (err) {
        console.error('❌ Lỗi xóa barcode:', err);
        result.success = false;
        result.errors.push({ global: err.message });
        return result;
    }
};

/**
 * HÀM EXPORT: Danh sách sản phẩm với barcode
 */
export const exportProductsWithBarcode = async () => {
    try {
        const productsCollectionRef = collection(db, 'products');
        const productSnapshot = await getDocs(productsCollectionRef);

        const data = productSnapshot.docs.map(doc => ({
            id: doc.id,
            name: doc.data().name,
            barcode: doc.data().barcode || 'N/A'
        }));

        return data;

    } catch (err) {
        console.error('❌ Lỗi export barcode:', err);
        return [];
    }
};

// ✅ EXPORT DEFAULT - CẦN CÓ CÁI NÀY
export default {
    generateBarcodeFromId,
    calculateEAN13Checksum,
    insertBarcodesForAllProducts,
    insertBarcodeForProduct,
    deleteAllBarcodes,
    exportProductsWithBarcode
};