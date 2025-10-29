// src/components/OrderDetailModal.js
import React, { useState } from 'react';
import { doc, updateDoc } from 'firebase/firestore';
import { db } from '../firebaseConfig';
import { formatCurrency } from '../utils/format';

// FIXED: Đã thêm PAID vào danh sách trạng thái
const ORDER_STATUSES = ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'PAID', 'FAILED_PAYMENT']; 

// --- DARK/MINIMALIST STYLES CHO MODAL ---
const modalStyles = {
    overlay: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0, 0, 0, 0.9)', zIndex: 1000, display: 'flex', justifyContent: 'center', alignItems: 'center' },
    modalContent: { backgroundColor: '#1A1A1A', padding: '30px', borderRadius: '8px', width: '90%', maxWidth: '800px', maxHeight: '90vh', overflowY: 'auto', color: '#E0E0E0' }, 
    header: { borderBottom: '2px solid #C40000', paddingBottom: '10px', marginBottom: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
    h2: { fontSize: '24px', fontWeight: 'bold', margin: 0, color: '#C40000' },
    section: { marginBottom: '20px', border: '1px solid #444', padding: '15px', borderRadius: '4px', backgroundColor: '#292929' }, 
    
    // Tối ưu hóa Table
    table: { width: '100%', borderCollapse: 'collapse', marginTop: '10px', fontSize: '13px' },
    th: { backgroundColor: '#333', color: '#E0E0E0', padding: '10px', textAlign: 'left', borderBottom: '1px solid #555' },
    td: { padding: '8px 10px', borderBottom: '1px solid #333', color: '#E0E0E0' },
    
    // Tối ưu hóa Select & Button
    statusSelect: { padding: '5px', border: '1px solid #C40000', borderRadius: '4px', fontWeight: 'bold', backgroundColor: '#333', color: '#E0E0E0' },
    saveButton: { backgroundColor: '#C40000', color: 'white', padding: '10px 20px', border: 'none', borderRadius: '4px', cursor: 'pointer', marginTop: '20px', marginRight: '10px', transition: 'background-color 0.2s' },
    imageThumb: { width: '40px', height: '40px', objectFit: 'cover', borderRadius: '3px', marginRight: '10px' }
};

const OrderDetailModal = ({ order, onClose, onSave }) => {
    const [status, setStatus] = useState(order.orderStatus || 'PENDING');
    const [saving, setSaving] = useState(false);

    const handleSaveStatus = async () => {
        setSaving(true);
        try {
            const docRef = doc(db, 'orders', order.id);
            await updateDoc(docRef, { orderStatus: status });
            
            onSave(); 
            onClose(); 
            alert(`Đã cập nhật trạng thái đơn hàng ID ${order.orderId} thành ${status}`);
        } catch (error) {
            console.error("Lỗi cập nhật trạng thái:", error);
            alert("LỖI: Không thể cập nhật trạng thái đơn hàng.");
        } finally {
            setSaving(false);
        }
    };

    // HÀM XÂY DỰNG ĐỊA CHỈ CHI TIẾT
    const getFullAddress = (addr) => {
        if (!addr) return "Không có địa chỉ";
        
        const parts = [
            addr.streetAddress || addr.street || '',
            addr.ward || '', 
            addr.district || '', 
            addr.city || addr.province || '', 
        ].filter(p => p); 

        return `${addr.name || 'N/A'} - ${parts.join(', ')}`;
    };
    
    // LẤY THÔNG TIN KHÁCH HÀNG TỪ SHIPPING ADDRESS
    const shipping = order.shippingAddress || {};
    const customerName = shipping.fullName || 'N/A';
    const customerPhone = shipping.phoneNumber || 'N/A';
    const customerEmail = order.email || 'N/A'; 


    return (
        <div style={modalStyles.overlay} onClick={onClose}>
            <div style={modalStyles.modalContent} onClick={(e) => e.stopPropagation()}>
                
                <div style={modalStyles.header}>
                    <h2 style={modalStyles.h2}>Chi tiết Đơn hàng: {order.orderId || order.id}</h2>
                    <div>
                        <span style={{ marginRight: '10px', color: '#A0A0A0' }}>Trạng thái:</span>
                        <select 
                            value={status} 
                            onChange={(e) => setStatus(e.target.value)} 
                            style={modalStyles.statusSelect}
                        >
                            {ORDER_STATUSES.map(s => <option key={s} value={s}>{s}</option>)} 
                        </select>
                    </div>
                </div>

                {/* THÔNG TIN KHÁCH HÀNG & VẬN CHUYỂN */}
                <div style={modalStyles.section}>
                    <h3 style={{ marginBottom: '10px', fontSize: '16px', fontWeight: 'bold', color: '#C40000' }}>Thông tin Khách hàng & Vận chuyển</h3>
                    <p><strong>Ngày đặt:</strong> {order.orderDate ? order.orderDate.toLocaleString() : 'N/A'}</p>
                    <p><strong>ID Khách hàng:</strong> {order.userId || 'Guest'}</p>
                    <hr style={{margin: '10px 0', border: '0', borderTop: '1px solid #555'}}/>
                    <p><strong>Tên người nhận:</strong> {customerName}</p>
                    <p><strong>SĐT:</strong> {customerPhone}</p>
                    <p><strong>Email:</strong> {customerEmail}</p>
                    <p><strong>Địa chỉ:</strong> {getFullAddress(order.shippingAddress)}</p>
                    <p><strong>Phương thức TT:</strong> {order.paymentMethod || 'COD'}</p>
                    <p><strong>Mã Voucher:</strong> {order.voucherCode || 'Không áp dụng'}</p>
                </div>

                {/* CHI TIẾT SẢN PHẨM */}
                <div style={modalStyles.section}>
                    <h3 style={{ marginBottom: '10px', fontSize: '16px', fontWeight: 'bold', color: '#C40000' }}>Các mặt hàng ({order.items?.length || 0})</h3>
                    <table style={modalStyles.table}>
                        <thead>
                            <tr>
                                <th style={modalStyles.th}>Ảnh</th>
                                <th style={modalStyles.th}>Sản phẩm</th>
                                <th style={modalStyles.th}>Màu/Size</th>
                                <th style={modalStyles.th}>SL</th>
                                <th style={modalStyles.th}>Giá</th>
                            </tr>
                        </thead>
                        <tbody>
                            {order.items?.map((item, index) => {
                                const variantInfo = item.variant?.split('/') || ['N/A', 'N/A'];
                                const color = variantInfo[0].trim();
                                const size = variantInfo[1]?.trim() || 'N/A';

                                return (
                                <tr key={index}>
                                    <td style={modalStyles.td}>
                                        <img src={item.productImage || 'https://via.placeholder.com/40'} alt={item.name} style={modalStyles.imageThumb} />
                                    </td>
                                    <td style={modalStyles.td}>{item.productName || item.name || 'Sản phẩm không tên'}</td>
                                    <td style={modalStyles.td}>{color} / {size}</td>
                                    <td style={modalStyles.td}>{item.quantity || 1}</td>
                                    <td style={modalStyles.td}>{formatCurrency(item.price || 0)}</td>
                                </tr>
                            );})}
                        </tbody>
                    </table>
                </div>

                {/* TỔNG KẾT THANH TOÁN */}
                <div style={modalStyles.section}>
                    <h3 style={{ marginBottom: '10px', fontSize: '16px', fontWeight: 'bold', color: '#C40000' }}>Tổng kết</h3>
                    <p><strong>Tổng phụ:</strong> {formatCurrency(order.subtotal)}</p>
                    <p><strong>Giảm giá Voucher:</strong> - {formatCurrency(order.discountAmount)}</p>
                    <p style={{ fontSize: '18px', fontWeight: 'bold', marginTop: '10px', color: '#007bff' }}>
                        Tổng cộng: {formatCurrency(order.totalAmount)}
                    </p>
                </div>

                <button style={modalStyles.saveButton} onClick={handleSaveStatus} disabled={saving}>
                    {saving ? 'Đang lưu...' : 'Lưu Trạng thái'}
                </button>
                <button style={{ ...modalStyles.saveButton, backgroundColor: '#666' }} onClick={onClose} disabled={saving}>
                    Đóng
                </button>
            </div>
        </div>
    );
};

export default OrderDetailModal;