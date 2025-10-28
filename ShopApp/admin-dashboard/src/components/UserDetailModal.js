// src/components/UserDetailModal.js

import React, { useState } from 'react';
import { doc, updateDoc, setDoc } from 'firebase/firestore';
import { db } from '../firebaseConfig'; // Đảm bảo db được import

const USER_ROLES = ['Admin', 'Staff', 'Customer'];
const USER_STATUSES = ['Active', 'Locked', 'Pending']; 

const modalStyles = {
    overlay: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0, 0, 0, 0.7)', zIndex: 1000, display: 'flex', justifyContent: 'center', alignItems: 'center' },
    modalContent: { backgroundColor: '#FFFFFF', padding: '30px', borderRadius: '8px', width: '90%', maxWidth: '700px', maxHeight: '90vh', overflowY: 'auto' },
    formGroup: { marginBottom: '15px' },
    label: { display: 'block', fontWeight: 'bold', marginBottom: '5px' },
    input: { width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' },
    saveButton: { backgroundColor: '#000000', color: 'white', padding: '10px 15px', border: 'none', borderRadius: '4px', cursor: 'pointer', marginRight: '10px' },
    roleSelect: { padding: '8px', border: '1px solid #C40000', borderRadius: '4px' }
};

const UserDetailModal = ({ user: initialUser, onClose, onSave }) => {
    // Nếu user rỗng, đây là thao tác Thêm mới
    const isEditing = !!initialUser.id;
    const [user, setUser] = useState(initialUser);
    const [saving, setSaving] = useState(false);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setUser(prev => ({ ...prev, [name]: value }));
    };

    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);

        const dataToSave = {
            ...user,
            // Đảm bảo trạng thái disabled đồng bộ với status (dùng cho Auth)
            disabled: user.status === 'Locked', 
            // Cập nhật các trường cá nhân
            firstName: user.firstName || '',
            lastName: user.lastName || '',
            phoneNumber: user.phoneNumber || '',
            role: user.role || 'Customer'
        };

        try {
            const userRef = doc(db, 'users', user.id || 'new_user_temp');

            if (isEditing) {
                // UPDATE (Chỉnh sửa thông tin mở rộng)
                await updateDoc(userRef, dataToSave);
                alert("Cập nhật thông tin người dùng thành công!");
            } else {
                // THÊM MỚI (Chỉ lưu vào Firestore. Cần thêm logic tạo Auth User nếu là sản phẩm thực tế)
                alert("Tính năng Thêm mới chỉ lưu vào Firestore. Cần thêm logic Auth.");
                // Dùng setDoc với ID tạm thời hoặc tự tạo ID Auth trước
                // await setDoc(doc(db, 'users', dataToSave.email), dataToSave);
            }

            onSave(); 
            onClose(); 
        } catch (error) {
            console.error("Lỗi khi lưu người dùng:", error);
            alert("LỖI LƯU DỮ LIỆU. Kiểm tra console.");
        } finally {
            setSaving(false);
        }
    };

    return (
        <div style={modalStyles.overlay} onClick={onClose}>
            <div style={modalStyles.modalContent} onClick={(e) => e.stopPropagation()}>
                <h2 style={{ borderBottom: '1px solid #eee', paddingBottom: '10px', marginBottom: '20px' }}>
                    {isEditing ? `Sửa Chi Tiết: ${user.fullName || user.email}` : 'Thêm Người Dùng Mới'}
                </h2>
                
                <form onSubmit={handleSave}>
                    {/* THÔNG TIN CÁ NHÂN */}
                    <div style={modalStyles.formGroup}>
                        <label style={modalStyles.label}>Email:</label>
                        <input type="email" name="email" value={user.email || ''} onChange={handleChange} style={modalStyles.input} required disabled={isEditing} />
                    </div>
                    
                    <div style={{ display: 'flex', gap: '20px' }}>
                        <div style={{ ...modalStyles.formGroup, flexGrow: 1 }}>
                            <label style={modalStyles.label}>Họ (First Name):</label>
                            <input type="text" name="firstName" value={user.firstName || ''} onChange={handleChange} style={modalStyles.input} />
                        </div>
                        <div style={{ ...modalStyles.formGroup, flexGrow: 1 }}>
                            <label style={modalStyles.label}>Tên (Last Name):</label>
                            <input type="text" name="lastName" value={user.lastName || ''} onChange={handleChange} style={modalStyles.input} />
                        </div>
                    </div>
                    
                    <div style={modalStyles.formGroup}>
                        <label style={modalStyles.label}>Số điện thoại:</label>
                        <input type="tel" name="phoneNumber" value={user.phoneNumber || ''} onChange={handleChange} style={modalStyles.input} />
                    </div>

                    <hr style={{margin: '20px 0'}}/>

                    {/* VAI TRÒ & TRẠNG THÁI */}
                    <div style={{ display: 'flex', gap: '20px' }}>
                        <div style={{ ...modalStyles.formGroup, flexGrow: 1 }}>
                            <label style={modalStyles.label}>Vai trò:</label>
                            <select name="role" value={user.role || 'Customer'} onChange={handleChange} style={modalStyles.roleSelect}>
                                {USER_ROLES.map(role => <option key={role} value={role}>{role}</option>)}
                            </select>
                        </div>
                        <div style={{ ...modalStyles.formGroup, flexGrow: 1 }}>
                            <label style={modalStyles.label}>Trạng thái TK:</label>
                            <select name="status" value={user.status || 'Active'} onChange={handleChange} style={modalStyles.roleSelect}>
                                {USER_STATUSES.map(status => <option key={status} value={status}>{status}</option>)}
                            </select>
                        </div>
                    </div>

                    {isEditing && (
                        <div style={{ marginTop: '15px', padding: '10px', border: '1px dashed #ccc' }}>
                            <p><strong>Ngày tạo:</strong> {user.createdAt ? new Date(Number(user.createdAt)).toLocaleString() : 'N/A'}</p>
                            <p><strong>Đăng nhập cuối:</strong> {user.lastLoginDate || 'N/A'}</p>
                        </div>
                    )}
                    
                    <div style={{ marginTop: '30px', borderTop: '1px solid #eee', paddingTop: '20px' }}>
                        <button type="submit" style={modalStyles.saveButton} disabled={saving}>
                            {saving ? 'Đang lưu...' : 'Lưu Thay Đổi'}
                        </button>
                        <button type="button" style={{ ...modalStyles.saveButton, backgroundColor: '#666' }} onClick={onClose} disabled={saving}>
                            Hủy
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default UserDetailModal;