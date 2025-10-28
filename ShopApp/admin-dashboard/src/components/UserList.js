// src/components/UserList.js

import React, { useState, useEffect, useMemo } from 'react';
import { collection, getDocs, doc, updateDoc } from 'firebase/firestore';
import { db } from '../firebaseConfig'; 
import UserDetailModal from './UserDetailModal'; // Modal chi tiết/chỉnh sửa

// ----------------------------------------------------------------------
// CẤU HÌNH VÀ STYLES
// ----------------------------------------------------------------------
const USER_ROLES = ['Admin', 'Staff', 'Customer'];
const USERS_PER_PAGE = 10;

const styles = {
    container: { padding: '20px', backgroundColor: '#FFFFFF', minHeight: '80vh' },
    title: { color: '#000000', borderBottom: '3px solid #C40000', paddingBottom: '10px', marginBottom: '20px', fontWeight: 300, fontSize: '28px' },
    table: { width: '100%', borderCollapse: 'collapse', marginTop: '20px', fontSize: '14px' },
    th: { backgroundColor: '#000000', color: '#FFFFFF', padding: '12px 15px', textAlign: 'left', textTransform: 'uppercase', fontWeight: 500 },
    td: { padding: '10px 15px', borderBottom: '1px solid #EEEEEE', verticalAlign: 'middle' },
    filterBar: { display: 'flex', gap: '15px', marginBottom: '20px', alignItems: 'center', flexWrap: 'wrap' },
    filterInput: { padding: '8px 10px', border: '1px solid #ccc', borderRadius: '4px' },
    statusTag: (status) => {
        let color = '#666';
        if (status === 'Active') color = '#28a745';
        if (status === 'Locked') color = '#dc3545';
        if (status === 'Pending') color = '#ffc107';
        return { backgroundColor: color, color: 'white', padding: '4px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold', display: 'inline-block' };
    },
    actionButton: (isPrimary) => ({ border: '1px solid #000', background: isPrimary ? '#000' : 'none', color: isPrimary ? '#fff' : '#000', cursor: 'pointer', padding: '5px 10px', borderRadius: '4px', marginRight: '5px', fontSize: '12px' }),
    lockButton: (isLocked) => ({ backgroundColor: isLocked ? '#28a745' : '#dc3545', color: 'white', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer' }),
    pagination: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '20px' },
    // ĐỊNH NGHĨA HÀM STYLE
    pageButton: (disabled) => ({ 
        padding: '8px 15px', 
        border: '1px solid #000', 
        backgroundColor: disabled ? '#f0f0f0' : '#fff', 
        cursor: disabled ? 'not-allowed' : 'pointer' 
    })
};

// ----------------------------------------------------------------------
// COMPONENT CHÍNH
// ----------------------------------------------------------------------

const UserList = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterRole, setFilterRole] = useState('All');
    const [filterStatus, setFilterStatus] = useState('All');
    const [currentPage, setCurrentPage] = useState(1);
    const [detailModalUser, setDetailModalUser] = useState(null);

    // MOCK: Giả định hàm fetchUsers lấy dữ liệu từ Firestore (hoặc Auth Admin SDK)
    const fetchUsers = async () => {
        setLoading(true);
        try {
            // Lấy dữ liệu mở rộng từ Firestore
            const usersCollectionRef = collection(db, "users");
            const userSnapshot = await getDocs(usersCollectionRef);

            const usersList = userSnapshot.docs.map(doc => {
                const data = doc.data();
                // Giả định trạng thái khóa (disabled) đến từ Firestore hoặc Auth
                const status = data.disabled ? 'Locked' : (data.emailVerified ? 'Active' : 'Pending'); 
                
                return { 
                    id: doc.id, 
                    ...data,
                    status: status,
                    fullName: `${data.firstName || ''} ${data.lastName || ''}`.trim(),
                    lastLoginDate: data.lastLogin ? new Date(Number(data.lastLogin)).toLocaleString() : 'N/A'
                };
            });

            setUsers(usersList);

        } catch (err) {
            console.error("Lỗi khi tải người dùng:", err);
            // ... (xử lý lỗi)
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    // ----------------------------------------------------------------------
    // LỌC VÀ PHÂN TRANG
    // ----------------------------------------------------------------------
    const filteredAndPaginatedUsers = useMemo(() => {
        let currentUsers = users;

        // 1. Tìm kiếm (Tên, Email)
        if (searchTerm) {
            const lowerSearchTerm = searchTerm.toLowerCase();
            currentUsers = currentUsers.filter(u =>
                u.fullName?.toLowerCase().includes(lowerSearchTerm) ||
                u.email?.toLowerCase().includes(lowerSearchTerm)
            );
        }

        // 2. Lọc theo Vai trò
        if (filterRole !== 'All') {
            currentUsers = currentUsers.filter(u => u.role === filterRole);
        }
        
        // 3. Lọc theo Trạng thái
        if (filterStatus !== 'All') {
            currentUsers = currentUsers.filter(u => u.status === filterStatus);
        }

        // --- PHÂN TRANG ---
        const totalItems = currentUsers.length;
        const totalPages = Math.ceil(totalItems / USERS_PER_PAGE);

        const startIndex = (currentPage - 1) * USERS_PER_PAGE;
        const paginatedUsers = currentUsers.slice(startIndex, startIndex + USERS_PER_PAGE);

        return {
            paginatedUsers,
            totalItems,
            totalPages
        };
    }, [users, searchTerm, filterRole, filterStatus, currentPage]);

    const handleLockUnlock = async (user) => {
        const newStatus = user.status === 'Locked' ? 'Active' : 'Locked';
        const confirmMsg = `Bạn có chắc chắn muốn ${newStatus === 'Active' ? 'MỞ KHÓA' : 'KHÓA'} tài khoản của ${user.fullName} (${user.email})?`;
        
        if (window.confirm(confirmMsg)) {
            try {
                // Cập nhật trạng thái khóa trong Firestore
                await updateDoc(doc(db, 'users', user.id), { disabled: newStatus === 'Locked', status: newStatus });
                alert(`Đã cập nhật trạng thái thành: ${newStatus}`);
                fetchUsers();
            } catch (error) {
                console.error("Lỗi khóa/mở khóa:", error);
                alert("LỖI: Không thể cập nhật trạng thái tài khoản.");
            }
        }
    };
    
    const { paginatedUsers, totalItems, totalPages } = filteredAndPaginatedUsers;

    // ----------------------------------------------------------------------
    // RENDER
    // ----------------------------------------------------------------------

    if (loading) return <div style={styles.container}>Đang tải dữ liệu người dùng...</div>;

    return (
        <div style={styles.container}>
            <h1 style={styles.title}>Quản Lý Người Dùng & Quyền Hạn</h1>
            
            {/* Thanh Bộ lọc và Tìm kiếm */}
            <div style={styles.filterBar}>
                <input
                    type="text"
                    placeholder="Tìm kiếm theo Tên hoặc Email..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    style={{...styles.filterInput, flexGrow: 1}}
                />
                
                {/* Lọc theo Vai trò */}
                <select 
                    value={filterRole} 
                    onChange={(e) => setFilterRole(e.target.value)} 
                    style={styles.filterInput}
                >
                    <option value="All">Tất cả Vai trò</option>
                    {USER_ROLES.map(role => <option key={role} value={role}>{role}</option>)}
                </select>
                
                {/* Lọc theo Trạng thái */}
                <select 
                    value={filterStatus} 
                    onChange={(e) => setFilterStatus(e.target.value)} 
                    style={styles.filterInput}
                >
                    <option value="All">Tất cả Trạng thái</option>
                    <option value="Active">Hoạt động</option>
                    <option value="Locked">Đã khóa</option>
                    <option value="Pending">Chờ xác minh</option>
                </select>

                <button 
                    style={styles.actionButton(true)}
                    onClick={() => setDetailModalUser({})} // Mở modal để thêm mới
                >
                    + Thêm Người Dùng Mới
                </button>
            </div>

            <table style={styles.table}>
                <thead>
                    <tr>
                        <th style={styles.th}>ID / Email</th>
                        <th style={styles.th}>Họ Tên</th>
                        <th style={styles.th}>Vai trò</th>
                        <th style={styles.th}>Ngày Tạo</th>
                        <th style={styles.th}>Đăng nhập cuối</th>
                        <th style={styles.th}>Trạng thái</th>
                        <th style={styles.th}>Hành động</th>
                    </tr>
                </thead>
                <tbody>
                    {paginatedUsers.map((user) => (
                        <tr key={user.id}>
                            <td style={styles.td}>
                                <strong>{user.email}</strong><br/>
                                <small style={{color: '#666'}}>ID: {user.id}</small>
                            </td>
                            <td style={styles.td}>{user.fullName}</td>
                            <td style={styles.td}>{user.role || 'Customer'}</td>
                            <td style={styles.td}>{user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}</td>
                            <td style={styles.td}>{user.lastLoginDate}</td>
                            <td style={styles.td}>
                                <span style={styles.statusTag(user.status)}>
                                    {user.status}
                                </span>
                            </td>
                            <td style={styles.td}>
                                <button 
                                    style={styles.lockButton(user.status === 'Locked')}
                                    onClick={() => handleLockUnlock(user)}
                                >
                                    {user.status === 'Locked' ? 'Mở Khóa' : 'Khóa TK'}
                                </button>
                                <button 
                                    style={styles.actionButton(false)}
                                    onClick={() => setDetailModalUser(user)}
                                >
                                    Xem/Sửa
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            
            {/* Phân trang */}
            <div style={styles.pagination}>
                <span>
                    Hiển thị {paginatedUsers.length} trên {totalItems} người dùng
                </span>
                <div>
                    <button 
                        onClick={() => setCurrentPage(currentPage - 1)}
                        disabled={currentPage === 1}
                        // FIX: Gọi hàm styles.pageButton(disabled)
                        style={styles.pageButton(currentPage === 1)} 
                    >
                        &laquo; Trước
                    </button>
                    <span style={{ margin: '0 10px' }}>Trang {currentPage} / {totalPages}</span>
                    <button 
                        onClick={() => setCurrentPage(currentPage + 1)}
                        disabled={currentPage === totalPages || totalPages === 0}
                        // FIX: Gọi hàm styles.pageButton(disabled)
                        style={styles.pageButton(currentPage === totalPages || totalPages === 0)} 
                    >
                        Sau &raquo;
                    </button>
                </div>
            </div>

            {/* Modal chi tiết/chỉnh sửa người dùng */}
            {detailModalUser && (
                <UserDetailModal 
                    user={detailModalUser} 
                    onClose={() => setDetailModalUser(null)} 
                    onSave={fetchUsers}
                />
            )}
        </div>
    );
};

export default UserList;