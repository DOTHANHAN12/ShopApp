// src/components/UserList.js
import React, { useState, useEffect, useMemo } from 'react';
import { collection, getDocs, doc, updateDoc } from 'firebase/firestore';
import { db } from '../firebaseConfig'; 
import UserDetailModal from './UserDetailModal';

const USER_ROLES = ['Admin', 'Staff', 'Customer'];
const USERS_PER_PAGE = 10;

const statsStyles = {
    container: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '15px',
        marginBottom: '30px',
    },
    card: {
        background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
        border: '1px solid #4CAF50',
        borderRadius: '8px',
        padding: '20px',
        boxShadow: '0 4px 15px rgba(76, 175, 80, 0.2)',
        transition: 'transform 0.2s ease, box-shadow 0.2s ease',
        cursor: 'pointer',
    },
    cardHover: {
        transform: 'translateY(-5px)',
        boxShadow: '0 8px 25px rgba(76, 175, 80, 0.4)',
    },
    value: {
        fontSize: '32px',
        fontWeight: 'bold',
        color: '#4CAF50',
        marginBottom: '8px',
    },
    label: {
        fontSize: '13px',
        color: '#A0A0A0',
        textTransform: 'uppercase',
        letterSpacing: '1px',
    },
    description: {
        fontSize: '12px',
        color: '#888',
        marginTop: '8px',
    },
};

const styles = {
    title: { color: '#E0E0E0', borderBottom: '3px solid #4CAF50', paddingBottom: '10px', marginBottom: '20px', fontWeight: 300, fontSize: '28px' },
    table: { width: '100%', borderCollapse: 'collapse', marginTop: '20px', fontSize: '14px', color: '#E0E0E0' },
    th: { backgroundColor: '#000000', color: '#FFFFFF', padding: '12px 15px', textAlign: 'left', textTransform: 'uppercase', fontWeight: 600 },
    td: { padding: '10px 15px', borderBottom: '1px solid #444', verticalAlign: 'middle' },
    filterBar: { display: 'flex', gap: '15px', marginBottom: '20px', alignItems: 'center', flexWrap: 'wrap' },
    filterInput: { padding: '8px 10px', border: '1px solid #555', borderRadius: '4px', backgroundColor: '#333', color: '#E0E0E0' },
    
    statusTag: (status) => {
        let color = '#666';
        if (status === 'active') color = '#28a745';
        if (status === 'locked') color = '#dc3545';
        if (status === 'pending') color = '#ffc107';
        return { backgroundColor: color, color: 'white', padding: '4px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold', display: 'inline-block' };
    },
    
    actionButton: (isPrimary) => ({ 
        border: '1px solid #4CAF50', 
        background: isPrimary ? '#4CAF50' : 'none', 
        color: isPrimary ? '#fff' : '#4CAF50', 
        cursor: 'pointer', 
        padding: '5px 10px', 
        borderRadius: '4px', 
        marginRight: '5px', 
        fontSize: '12px' 
    }),
    
    lockButton: (isLocked) => ({ 
        backgroundColor: isLocked ? '#28a745' : '#dc3545', 
        color: 'white', 
        padding: '5px 10px', 
        borderRadius: '4px', 
        cursor: 'pointer', 
        marginRight: '5px'
    }),
    
    pagination: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '20px', color: '#E0E0E0' },
    pageButton: (disabled) => ({ 
        padding: '8px 15px', 
        border: '1px solid #4CAF50', 
        backgroundColor: disabled ? '#333' : '#4CAF50', 
        color: disabled ? '#888' : '#fff',
        cursor: disabled ? 'not-allowed' : 'pointer' 
    })
};

const UserList = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterRole, setFilterRole] = useState('All');
    const [filterStatus, setFilterStatus] = useState('All');
    const [currentPage, setCurrentPage] = useState(1);
    const [detailModalUser, setDetailModalUser] = useState(null);
    const [stats, setStats] = useState({
        total: 0,
        active: 0,
        locked: 0,
        pending: 0,
        admins: 0,
        staff: 0,
        customers: 0,
    });

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const usersCollectionRef = collection(db, "users");
            const userSnapshot = await getDocs(usersCollectionRef);

            const usersList = userSnapshot.docs.map(doc => {
                const data = doc.data();
                const status = data.disabled ? 'locked' : (data.emailVerified ? 'active' : 'pending'); 
                
                return { 
                    id: doc.id, 
                    ...data,
                    status: status,
                    fullName: `${data.firstName || ''} ${data.lastName || ''}`.trim(),
                    lastLoginDate: data.lastLogin ? new Date(Number(data.lastLogin)).toLocaleString() : 'N/A'
                };
            });

            setUsers(usersList);
            
            // Tính stats
            const statsData = {
                total: usersList.length,
                active: usersList.filter(u => u.status === 'active').length,
                locked: usersList.filter(u => u.status === 'locked').length,
                pending: usersList.filter(u => u.status === 'pending').length,
                admins: usersList.filter(u => u.role === 'Admin').length,
                staff: usersList.filter(u => u.role === 'Staff').length,
                customers: usersList.filter(u => u.role === 'Customer').length,
            };
            setStats(statsData);

        } catch (err) {
            console.error("Lỗi khi tải người dùng:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const filteredAndPaginatedUsers = useMemo(() => {
        let currentUsers = users;

        if (searchTerm) {
            const lowerSearchTerm = searchTerm.toLowerCase();
            currentUsers = currentUsers.filter(u =>
                u.fullName?.toLowerCase().includes(lowerSearchTerm) ||
                u.email?.toLowerCase().includes(lowerSearchTerm)
            );
        }

        if (filterRole !== 'All') {
            currentUsers = currentUsers.filter(u => u.role === filterRole);
        }
        
        if (filterStatus !== 'All') {
            currentUsers = currentUsers.filter(u => u.status === filterStatus);
        }

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
        const newStatus = user.status === 'locked' ? 'active' : 'locked';
        const confirmMsg = `Bạn có chắc chắn muốn ${newStatus === 'active' ? 'MỞ KHÓA' : 'KHÓA'} tài khoản của ${user.fullName} (${user.email})?`;
        
        if (window.confirm(confirmMsg)) {
            try {
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

    if (loading) return <div style={{ color: '#E0E0E0', padding: '20px' }}>Đang tải dữ liệu...</div>;

    return (
        <div style={{ padding: '20px', backgroundColor: '#1A1A1A', minHeight: '100vh', color: '#E0E0E0' }}>
            <h1 style={styles.title}>👥 Quản Lý Người Dùng & Quyền Hạn</h1>
            
            {/* STATS CARDS */}
            <div style={statsStyles.container}>
                <div style={{...statsStyles.card, ...statsStyles.cardHover}}>
                    <div style={statsStyles.value}>{stats.total}</div>
                    <div style={statsStyles.label}>📊 Tổng Người Dùng</div>
                    <div style={statsStyles.description}>{totalItems} hiển thị</div>
                </div>
                
                <div style={{...statsStyles.card, border: '1px solid #28a745', boxShadow: '0 4px 15px rgba(40, 167, 69, 0.2)', ...statsStyles.cardHover}}>
                    <div style={{...statsStyles.value, color: '#28a745'}}>{stats.active}</div>
                    <div style={statsStyles.label}>✅ Hoạt Động</div>
                    <div style={statsStyles.description}>{((stats.active / stats.total) * 100).toFixed(0)}% tổng số</div>
                </div>
                
                <div style={{...statsStyles.card, border: '1px solid #dc3545', boxShadow: '0 4px 15px rgba(220, 53, 69, 0.2)', ...statsStyles.cardHover}}>
                    <div style={{...statsStyles.value, color: '#dc3545'}}>{stats.locked}</div>
                    <div style={statsStyles.label}>🔒 Đã Khóa</div>
                    <div style={statsStyles.description}>{((stats.locked / stats.total) * 100).toFixed(0)}% tổng số</div>
                </div>
                
                <div style={{...statsStyles.card, border: '1px solid #FF9800', boxShadow: '0 4px 15px rgba(255, 152, 0, 0.2)', ...statsStyles.cardHover}}>
                    <div style={{...statsStyles.value, color: '#FF9800'}}>{stats.customers}</div>
                    <div style={statsStyles.label}>🛒 Khách Hàng</div>
                    <div style={statsStyles.description}>{((stats.customers / stats.total) * 100).toFixed(0)}% tổng số</div>
                </div>
            </div>

            <div style={styles.filterBar}>
                <input
                    type="text"
                    placeholder="Tìm kiếm theo Tên hoặc Email..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    style={{...styles.filterInput, flexGrow: 1}}
                />
                
                <select 
                    value={filterRole} 
                    onChange={(e) => setFilterRole(e.target.value)} 
                    style={styles.filterInput}
                >
                    <option value="All">Tất cả Vai trò</option>
                    {USER_ROLES.map(role => <option key={role} value={role}>{role}</option>)}
                </select>
                
                <select 
                    value={filterStatus} 
                    onChange={(e) => setFilterStatus(e.target.value)} 
                    style={styles.filterInput}
                >
                    <option value="All">Tất cả Trạng thái</option>
                    <option value="active">Hoạt động</option>
                    <option value="locked">Đã khóa</option>
                    <option value="pending">Chờ xác minh</option>
                </select>

                <button 
                    style={styles.actionButton(true)}
                    onClick={() => setDetailModalUser({ id: `temp_${Date.now()}` })}
                >
                    + Thêm Người Dùng
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
                                <small style={{color: '#888'}}>ID: {user.id}</small>
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
            
            <div style={styles.pagination}>
                <span>
                    Hiển thị {paginatedUsers.length} trên {totalItems} người dùng
                </span>
                <div>
                    <button 
                        onClick={() => setCurrentPage(currentPage - 1)}
                        disabled={currentPage === 1}
                        style={styles.pageButton(currentPage === 1)} 
                    >
                        &laquo; Trước
                    </button>
                    <span style={{ margin: '0 10px' }}>Trang {currentPage} / {totalPages}</span>
                    <button 
                        onClick={() => setCurrentPage(currentPage + 1)}
                        disabled={currentPage === totalPages || totalPages === 0}
                        style={styles.pageButton(currentPage === totalPages || totalPages === 0)} 
                    >
                        Sau &raquo;
                    </button>
                </div>
            </div>

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