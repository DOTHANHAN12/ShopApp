import React, { useState, useEffect } from 'react';
import { collection, addDoc, getDocs, orderBy, query, doc, updateDoc, deleteDoc } from 'firebase/firestore';
import { db } from '../firebaseConfig';

// ======================================================================
// 🎨 CONFIGURATIONS
// ======================================================================
const DISCOUNT_TYPES = [
    { value: 'PERCENT', label: '📊 Phần trăm (%)', icon: '%' },
    { value: 'FIXED_AMOUNT', label: '💰 Số tiền cố định', icon: '₫' },
    { value: 'SHIPPING', label: '🚚 Miễn phí vận chuyển', icon: '🚚' },
];

const VOUCHER_TYPES = [
    { value: 'PUBLIC', label: '👥 Công khai', color: '#4CAF50' },
    { value: 'HIDDEN', label: '🔒 Ẩn (nhập mã)', color: '#FF9800' },
    { value: 'USER_SPECIFIC', label: '👤 Riêng từng user', color: '#2196F3' },
];

// ======================================================================
// 🎨 STYLES
// ======================================================================
const styles = {
    container: {
        padding: '40px 20px',
        background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
        minHeight: '100vh',
        maxWidth: '1600px',
        margin: '0 auto',
        fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    },

    header: {
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: '40px',
        borderRadius: '20px',
        marginBottom: '40px',
        boxShadow: '0 20px 60px rgba(102, 126, 234, 0.4)',
    },

    title: {
        color: '#ffffff',
        fontSize: '42px',
        fontWeight: '800',
        margin: '0 0 10px 0',
    },

    subtitle: {
        color: 'rgba(255,255,255,0.9)',
        fontSize: '16px',
        margin: 0,
    },

    statsContainer: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '20px',
        marginBottom: '40px',
    },

    statCard: {
        background: 'rgba(255,255,255,0.05)',
        backdropFilter: 'blur(10px)',
        padding: '25px',
        borderRadius: '15px',
        border: '1px solid rgba(255,255,255,0.1)',
    },

    statValue: {
        fontSize: '36px',
        fontWeight: 'bold',
        color: '#ffffff',
        marginBottom: '5px',
    },

    statLabel: {
        fontSize: '14px',
        color: 'rgba(255,255,255,0.7)',
        textTransform: 'uppercase',
        letterSpacing: '1px',
    },

    formSection: {
        background: 'rgba(255,255,255,0.05)',
        backdropFilter: 'blur(10px)',
        padding: '35px',
        borderRadius: '20px',
        marginBottom: '40px',
        border: '1px solid rgba(255,255,255,0.1)',
    },

    sectionTitle: {
        color: '#ffffff',
        fontSize: '24px',
        fontWeight: '700',
        marginBottom: '25px',
    },

    formGroup: {
        marginBottom: '25px',
    },

    label: {
        display: 'block',
        color: 'rgba(255,255,255,0.9)',
        fontWeight: '600',
        marginBottom: '10px',
        fontSize: '14px',
    },

    input: {
        width: '100%',
        padding: '12px 18px',
        border: '2px solid rgba(255,255,255,0.1)',
        borderRadius: '12px',
        backgroundColor: 'rgba(0,0,0,0.3)',
        color: '#ffffff',
        fontSize: '14px',
        boxSizing: 'border-box',
    },

    select: {
        width: '100%',
        padding: '12px 18px',
        border: '2px solid rgba(255,255,255,0.1)',
        borderRadius: '12px',
        backgroundColor: 'rgba(0,0,0,0.3)',
        color: '#ffffff',
        fontSize: '14px',
        boxSizing: 'border-box',
        cursor: 'pointer',
    },

    gridContainer: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
        gap: '20px',
        marginBottom: '20px',
    },

    button: (variant = 'primary') => {
        const variants = {
            primary: {
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                color: 'white',
            },
            success: {
                background: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
                color: 'white',
            },
            danger: {
                background: 'linear-gradient(135deg, #eb3349 0%, #f45c43 100%)',
                color: 'white',
            },
        };

        return {
            ...variants[variant],
            padding: '12px 32px',
            border: 'none',
            borderRadius: '12px',
            cursor: 'pointer',
            fontWeight: '600',
            fontSize: '15px',
            transition: 'all 0.2s ease',
        };
    },

    table: {
        width: '100%',
        borderCollapse: 'separate',
        borderSpacing: '0',
        marginTop: '20px',
    },

    th: {
        backgroundColor: 'rgba(102, 126, 234, 0.2)',
        color: '#ffffff',
        padding: '18px 20px',
        textAlign: 'left',
        fontWeight: '700',
        fontSize: '13px',
    },

    td: {
        padding: '16px 20px',
        borderBottom: '1px solid rgba(255,255,255,0.05)',
        color: 'rgba(255,255,255,0.9)',
        fontSize: '14px',
    },

    badge: (color) => ({
        backgroundColor: color,
        color: 'white',
        padding: '6px 12px',
        borderRadius: '12px',
        fontSize: '12px',
        fontWeight: '600',
        display: 'inline-block',
    }),

    actionButton: {
        padding: '6px 12px',
        border: 'none',
        borderRadius: '6px',
        cursor: 'pointer',
        fontSize: '12px',
        fontWeight: '600',
        marginRight: '8px',
        marginBottom: '8px',
    },

    emptyState: {
        textAlign: 'center',
        padding: '60px 20px',
        color: 'rgba(255,255,255,0.5)',
    },

    filterBar: {
        display: 'flex',
        gap: '15px',
        marginBottom: '25px',
        flexWrap: 'wrap',
    },

    searchInput: {
        flex: '1',
        minWidth: '250px',
        padding: '12px 18px',
        border: '2px solid rgba(255,255,255,0.1)',
        borderRadius: '12px',
        backgroundColor: 'rgba(0,0,0,0.3)',
        color: '#ffffff',
        fontSize: '14px',
    },
};

// ======================================================================
// 🎯 MAIN COMPONENT
// ======================================================================
const AdminVoucherManagement = () => {
    const [vouchers, setVouchers] = useState([]);
    const [filteredVouchers, setFilteredVouchers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [stats, setStats] = useState({
        total: 0,
        active: 0,
        inactive: 0,
        used: 0,
    });

    const [formData, setFormData] = useState({
        code: '',
        description: '',
        discountType: 'PERCENT',
        discountValue: 0,
        voucherType: 'PUBLIC',
        minOrderValue: 0,
        maxUsageLimit: 0,
        startDate: '',
        endDate: '',
    });

    const [filter, setFilter] = useState({
        search: '',
        type: 'ALL',
        status: 'ALL',
    });

    const [editingId, setEditingId] = useState(null);

    // ======================================================================
    // FETCH VOUCHERS
    // ======================================================================
    const fetchVouchers = async () => {
        setLoading(true);
        try {
            const q = query(collection(db, 'vouchers'), orderBy('code', 'asc'));
            const snapshot = await getDocs(q);

            const vouchersList = snapshot.docs.map(doc => ({
                id: doc.id,
                ...doc.data(),
                startDate: doc.data().startDate?.toDate?.() || new Date(doc.data().startDate),
                endDate: doc.data().endDate?.toDate?.() || new Date(doc.data().endDate),
            }));

            setVouchers(vouchersList);
            calculateStats(vouchersList);
        } catch (err) {
            console.error('Lỗi tải vouchers:', err);
            alert('❌ Lỗi: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchVouchers();
    }, []);

    // ======================================================================
    // CALCULATE STATS
    // ======================================================================
    const calculateStats = (list) => {
        const now = new Date();
        const statsData = {
            total: list.length,
            active: list.filter(v => new Date(v.startDate) <= now && new Date(v.endDate) >= now).length,
            inactive: list.filter(v => new Date(v.endDate) < now).length,
            used: list.reduce((sum, v) => sum + v.timesUsed, 0),
        };
        setStats(statsData);
    };

    // ======================================================================
    // FILTER LOGIC
    // ======================================================================
    useEffect(() => {
        let filtered = vouchers;

        if (filter.search) {
            filtered = filtered.filter(v =>
                v.code.toLowerCase().includes(filter.search.toLowerCase()) ||
                v.description.toLowerCase().includes(filter.search.toLowerCase())
            );
        }

        if (filter.type !== 'ALL') {
            filtered = filtered.filter(v => v.voucherType === filter.type);
        }

        if (filter.status !== 'ALL') {
            const now = new Date();
            if (filter.status === 'ACTIVE') {
                filtered = filtered.filter(v => 
                    new Date(v.startDate) <= now && new Date(v.endDate) >= now && v.timesUsed < v.maxUsageLimit
                );
            } else if (filter.status === 'INACTIVE') {
                filtered = filtered.filter(v => 
                    new Date(v.endDate) < now || v.timesUsed >= v.maxUsageLimit
                );
            }
        }

        setFilteredVouchers(filtered);
    }, [vouchers, filter]);

    // ======================================================================
    // CRUD OPERATIONS
    // ======================================================================
    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.code || !formData.description) {
            alert('Vui lòng điền đầy đủ thông tin');
            return;
        }

        try {
            if (editingId) {
                await updateDoc(doc(db, 'vouchers', editingId), {
                    ...formData,
                    startDate: new Date(formData.startDate),
                    endDate: new Date(formData.endDate),
                });
                alert('✅ Cập nhật voucher thành công!');
                setEditingId(null);
            } else {
                await addDoc(collection(db, 'vouchers'), {
                    ...formData,
                    startDate: new Date(formData.startDate),
                    endDate: new Date(formData.endDate),
                    timesUsed: 0,
                    type: formData.voucherType
                });
                alert('✅ Tạo voucher thành công!');
            }

            setFormData({
                code: '',
                description: '',
                discountType: 'PERCENT',
                discountValue: 0,
                voucherType: 'PUBLIC',
                minOrderValue: 0,
                maxUsageLimit: 0,
                startDate: '',
                endDate: '',
            });

            fetchVouchers();
        } catch (err) {
            console.error('Lỗi:', err);
            alert('❌ Lỗi: ' + err.message);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Bạn chắc chắn muốn xóa?')) return;

        try {
            await deleteDoc(doc(db, 'vouchers', id));
            alert('✅ Xóa thành công!');
            fetchVouchers();
        } catch (err) {
            alert('❌ Lỗi: ' + err.message);
        }
    };

    const handleEdit = (voucher) => {
        setFormData({
            code: voucher.code,
            description: voucher.description,
            discountType: voucher.discountType,
            discountValue: voucher.discountValue,
            voucherType: voucher.voucherType || 'PUBLIC',
            minOrderValue: voucher.minOrderValue,
            maxUsageLimit: voucher.maxUsageLimit,
            startDate: voucher.startDate.toISOString().split('T')[0],
            endDate: voucher.endDate.toISOString().split('T')[0],
        });
        setEditingId(voucher.id);
    };

    // ======================================================================
    // FORMAT FUNCTIONS
    // ======================================================================
    const formatCurrency = (value) => {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
    };

    const formatDate = (date) => {
        return date.toLocaleDateString('vi-VN');
    };

    const getVoucherTypeColor = (type) => {
        const config = VOUCHER_TYPES.find(t => t.value === type);
        return config?.color || '#607D8B';
    };

    const getDiscountTypeLabel = (type) => {
        const config = DISCOUNT_TYPES.find(t => t.value === type);
        return config?.label || type;
    };

    const getStatusLabel = (voucher) => {
        const now = new Date();
        if (new Date(voucher.endDate) < now) return '❌ Hết hạn';
        if (voucher.timesUsed >= voucher.maxUsageLimit) return '❌ Hết lượt';
        if (new Date(voucher.startDate) > now) return '⏳ Chưa bắt đầu';
        return '✅ Hoạt động';
    };

    return (
        <div style={styles.container}>
            {/* HEADER */}
            <div style={styles.header}>
                <h1 style={styles.title}>🎁 Quản Lý Voucher</h1>
                <p style={styles.subtitle}>Tạo, chỉnh sửa và quản lý voucher khuyến mãi</p>
            </div>

            {/* STATISTICS */}
            <div style={styles.statsContainer}>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.total}</div>
                    <div style={styles.statLabel}>📊 Tổng Voucher</div>
                </div>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.active}</div>
                    <div style={styles.statLabel}>✅ Hoạt Động</div>
                </div>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.inactive}</div>
                    <div style={styles.statLabel}>❌ Hết Hạn</div>
                </div>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.used}</div>
                    <div style={styles.statLabel}>🎯 Đã Dùng</div>
                </div>
            </div>

            {/* CREATE/EDIT FORM */}
            <div style={styles.formSection}>
                <h2 style={styles.sectionTitle}>
                    {editingId ? '✏️ Chỉnh Sửa Voucher' : '➕ Tạo Voucher Mới'}
                </h2>

                <form onSubmit={handleSubmit}>
                    <div style={styles.gridContainer}>
                        <div style={styles.formGroup}>
                            <label style={styles.label}>Mã Voucher</label>
                            <input
                                type="text"
                                style={styles.input}
                                value={formData.code}
                                onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                                placeholder="VD: SUMMER2024"
                                required
                            />
                        </div>

                        <div style={styles.formGroup}>
                            <label style={styles.label}>Loại Voucher</label>
                            <select
                                style={styles.select}
                                value={formData.voucherType}
                                onChange={(e) => setFormData({ ...formData, voucherType: e.target.value })}
                            >
                                {VOUCHER_TYPES.map(t => (
                                    <option key={t.value} value={t.value}>{t.label}</option>
                                ))}
                            </select>
                        </div>

                        <div style={styles.formGroup}>
                            <label style={styles.label}>Kiểu Giảm Giá</label>
                            <select
                                style={styles.select}
                                value={formData.discountType}
                                onChange={(e) => setFormData({ ...formData, discountType: e.target.value })}
                            >
                                {DISCOUNT_TYPES.map(t => (
                                    <option key={t.value} value={t.value}>{t.label}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div style={styles.gridContainer}>
                        <div style={styles.formGroup}>
                            <label style={styles.label}>Giá Trị Giảm {formData.discountType === 'PERCENT' ? '(%)' : '(₫)'}</label>
                            <input
                                type="number"
                                style={styles.input}
                                value={formData.discountValue}
                                onChange={(e) => setFormData({ ...formData, discountValue: parseFloat(e.target.value) })}
                                placeholder="0"
                                required
                            />
                        </div>

                        <div style={styles.formGroup}>
                            <label style={styles.label}>Giá Trị Đơn Tối Thiểu (₫)</label>
                            <input
                                type="number"
                                style={styles.input}
                                value={formData.minOrderValue}
                                onChange={(e) => setFormData({ ...formData, minOrderValue: parseFloat(e.target.value) })}
                                placeholder="0"
                                required
                            />
                        </div>

                        <div style={styles.formGroup}>
                            <label style={styles.label}>Số Lượng Tối Đa</label>
                            <input
                                type="number"
                                style={styles.input}
                                value={formData.maxUsageLimit}
                                onChange={(e) => setFormData({ ...formData, maxUsageLimit: parseInt(e.target.value) })}
                                placeholder="0"
                                required
                            />
                        </div>
                    </div>

                    <div style={styles.formGroup}>
                        <label style={styles.label}>Mô Tả</label>
                        <input
                            type="text"
                            style={styles.input}
                            value={formData.description}
                            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                            placeholder="Mô tả voucher..."
                            required
                        />
                    </div>

                    <div style={styles.gridContainer}>
                        <div style={styles.formGroup}>
                            <label style={styles.label}>Ngày Bắt Đầu</label>
                            <input
                                type="date"
                                style={styles.input}
                                value={formData.startDate}
                                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                                required
                            />
                        </div>

                        <div style={styles.formGroup}>
                            <label style={styles.label}>Ngày Kết Thúc</label>
                            <input
                                type="date"
                                style={styles.input}
                                value={formData.endDate}
                                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                                required
                            />
                        </div>
                    </div>

                    <div style={{ display: 'flex', gap: '15px', marginTop: '30px' }}>
                        <button type="submit" style={styles.button('primary')}>
                            {editingId ? '💾 Cập Nhật' : '➕ Tạo Mới'}
                        </button>
                        {editingId && (
                            <button
                                type="button"
                                onClick={() => {
                                    setEditingId(null);
                                    setFormData({
                                        code: '',
                                        description: '',
                                        discountType: 'PERCENT',
                                        discountValue: 0,
                                        voucherType: 'PUBLIC',
                                        minOrderValue: 0,
                                        maxUsageLimit: 0,
                                        startDate: '',
                                        endDate: '',
                                    });
                                }}
                                style={styles.button('success')}
                            >
                                🔄 Làm Mới
                            </button>
                        )}
                    </div>
                </form>
            </div>

            {/* FILTER & LIST */}
            <div style={styles.formSection}>
                <h2 style={styles.sectionTitle}>📋 Danh Sách Voucher ({filteredVouchers.length})</h2>

                <div style={styles.filterBar}>
                    <input
                        type="text"
                        placeholder="🔍 Tìm kiếm mã hoặc mô tả..."
                        value={filter.search}
                        onChange={(e) => setFilter({ ...filter, search: e.target.value })}
                        style={styles.searchInput}
                    />
                    <select
                        value={filter.type}
                        onChange={(e) => setFilter({ ...filter, type: e.target.value })}
                        style={{ ...styles.select, flex: '0 0 150px' }}
                    >
                        <option value="ALL">Tất cả loại</option>
                        {VOUCHER_TYPES.map(t => (
                            <option key={t.value} value={t.value}>{t.label}</option>
                        ))}
                    </select>
                    <select
                        value={filter.status}
                        onChange={(e) => setFilter({ ...filter, status: e.target.value })}
                        style={{ ...styles.select, flex: '0 0 150px' }}
                    >
                        <option value="ALL">Tất cả trạng thái</option>
                        <option value="ACTIVE">✅ Hoạt động</option>
                        <option value="INACTIVE">❌ Không hoạt động</option>
                    </select>
                </div>

                {loading ? (
                    <div style={styles.emptyState}>
                        <div style={{ fontSize: '64px', marginBottom: '20px' }}>⏳</div>
                        <div>Đang tải...</div>
                    </div>
                ) : filteredVouchers.length === 0 ? (
                    <div style={styles.emptyState}>
                        <div style={{ fontSize: '64px', marginBottom: '20px' }}>📭</div>
                        <div>Không có voucher nào</div>
                    </div>
                ) : (
                    <div style={{ overflowX: 'auto' }}>
                        <table style={styles.table}>
                            <thead>
                                <tr>
                                    <th style={styles.th}>Mã</th>
                                    <th style={styles.th}>Mô Tả</th>
                                    <th style={styles.th}>Loại</th>
                                    <th style={styles.th}>Giảm</th>
                                    <th style={styles.th}>Đơn Tối Thiểu</th>
                                    <th style={styles.th}>Lượt / Tối Đa</th>
                                    <th style={styles.th}>Ngày Kết Thúc</th>
                                    <th style={styles.th}>Trạng Thái</th>
                                    <th style={styles.th}>Thao Tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredVouchers.map(voucher => (
                                    <tr key={voucher.id} style={{ background: 'rgba(255,255,255,0.02)' }}>
                                        <td style={styles.td}>
                                            <strong>{voucher.code}</strong>
                                        </td>
                                        <td style={styles.td}>{voucher.description}</td>
                                        <td style={styles.td}>
                                            <span style={styles.badge(getVoucherTypeColor(voucher.voucherType))}>
                                                {VOUCHER_TYPES.find(t => t.value === voucher.voucherType)?.label}
                                            </span>
                                        </td>
                                        <td style={styles.td}>
                                            {voucher.discountType === 'PERCENT' 
                                                ? `${voucher.discountValue}%`
                                                : formatCurrency(voucher.discountValue)
                                            }
                                        </td>
                                        <td style={styles.td}>{formatCurrency(voucher.minOrderValue)}</td>
                                        <td style={styles.td}>
                                            {voucher.timesUsed} / {voucher.maxUsageLimit}
                                        </td>
                                        <td style={styles.td}>{formatDate(new Date(voucher.endDate))}</td>
                                        <td style={styles.td}>{getStatusLabel(voucher)}</td>
                                        <td style={styles.td}>
                                            <button
                                                onClick={() => handleEdit(voucher)}
                                                style={{
                                                    ...styles.actionButton,
                                                    background: '#2196F3',
                                                    color: 'white',
                                                }}
                                            >
                                                ✏️
                                            </button>
                                            <button
                                                onClick={() => handleDelete(voucher.id)}
                                                style={{
                                                    ...styles.actionButton,
                                                    background: '#F44336',
                                                    color: 'white',
                                                }}
                                            >
                                                🗑️
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
};

export default AdminVoucherManagement;
