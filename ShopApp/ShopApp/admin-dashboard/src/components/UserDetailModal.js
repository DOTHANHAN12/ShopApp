// src/components/UserDetailModal.js

import React, { useState } from 'react';
import { doc, updateDoc, setDoc } from 'firebase/firestore';
import { db } from '../firebaseConfig'; // Đảm bảo db được import

const USER_ROLES = ['Admin', 'Staff', 'Customer'];
const USER_STATUSES = ['Active', 'Locked', 'Pending']; 

// --- DARK/MINIMALIST STYLES CHO MODAL ---
const modalStyles = {
    overlay: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0, 0, 0, 0.9)', zIndex: 1000, display: 'flex', justifyContent: 'center', alignItems: 'center' },
    modalContent: { backgroundColor: '#1A1A1A', padding: '30px', borderRadius: '8px', width: '90%', maxWidth: '700px', maxHeight: '90vh', overflowY: 'auto', color: '#E0E0E0' },
    formGroup: { marginBottom: '15px' },
    label: { display: 'block', fontWeight: 'bold', marginBottom: '5px' },
    input: { width: '100%', padding: '10px', border: '1px solid #444', borderRadius: '4px', boxSizing: 'border-box', backgroundColor: '#333', color: '#E0E0E0' },
    saveButton: { backgroundColor: '#C40000', color: 'white', padding: '10px 15px', border: 'none', borderRadius: '4px', cursor: 'pointer', marginRight: '10px' },
    roleSelect: { padding: '8px', border: '1px solid #C40000', borderRadius: '4px', backgroundColor: '#333', color: '#E0E0E0' }
};

const UserDetailModal = ({ user: initialUser, onClose, onSave }) => {
    const isEditing = !!initialUser.email; // Sử dụng email để kiểm tra tính chỉnh sửa thực tế
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
            disabled: user.status === 'Locked', 
            firstName: user.firstName || '',
            lastName: user.lastName || '',
            phoneNumber: user.phoneNumber || '',
            role: user.role || 'Customer'
        };

        try {
            const userRef = doc(db, 'users', user.id);

            if (isEditing) {
                await updateDoc(userRef, dataToSave);
                alert("Cập nhật thông tin người dùng thành công!");
            } else {
                alert("Tính năng Thêm mới chỉ lưu vào Firestore. Cần thêm logic Auth.");
                // Thay setDoc nếu cần tạo mới
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
                <h2 style={{ borderBottom: '1px solid #555', paddingBottom: '10px', marginBottom: '20px', color: '#C40000' }}>
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

                    <hr style={{margin: '20px 0', borderTop: '1px solid #555'}}/>

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
                        <div style={{ marginTop: '15px', padding: '10px', border: '1px dashed #555' }}>
                            <p><strong>Ngày tạo:</strong> {user.createdAt ? new Date(Number(user.createdAt)).toLocaleString() : 'N/A'}</p>
                            <p><strong>Đăng nhập cuối:</strong> {user.lastLoginDate || 'N/A'}</p>
                        </div>
                    )}
                    
                    <div style={{ marginTop: '30px', borderTop: '1px solid #555', paddingTop: '20px' }}>
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