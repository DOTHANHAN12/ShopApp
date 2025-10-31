import React, { useState, useEffect } from 'react';
import { collection, addDoc, getDocs, orderBy, query, doc, updateDoc, deleteDoc, where } from 'firebase/firestore';
import { db } from '../firebaseConfig';

// ----------------------------------------------------------------------
// 🎨 ENHANCED CONFIGURATION
// ----------------------------------------------------------------------
const NOTIFICATION_TYPES = [
    { value: 'SYSTEM', label: '⚙️ Hệ thống', color: '#607D8B' },
    { value: 'ORDER', label: '🛒 Đơn hàng', color: '#2196F3' },
    { value: 'DELIVERY', label: '🚚 Giao hàng', color: '#FF9800' },
    { value: 'PROMOTION', label: '🎁 Khuyến mãi', color: '#E91E63' },
    { value: 'REVIEW', label: '⭐ Đánh giá', color: '#FFC107' },
];

const ACTION_TYPES = [
    { value: 'NONE', label: '🚫 Không có', desc: 'Chỉ hiển thị' },
    { value: 'OPEN_ORDER', label: '📦 Mở đơn hàng', desc: 'Mở chi tiết đơn hàng' },
    { value: 'OPEN_PRODUCT', label: '🏷️ Mở sản phẩm', desc: 'Mở chi tiết sản phẩm' },
    { value: 'OPEN_URL', label: '🔗 Mở URL', desc: 'Mở link bên ngoài' },
];

const ICON_TYPES = [
    { value: 'bell', label: '🔔 Chuông', emoji: '🔔' },
    { value: 'cart', label: '🛒 Giỏ hàng', emoji: '🛒' },
    { value: 'truck', label: '🚚 Xe tải', emoji: '🚚' },
    { value: 'gift', label: '🎁 Quà tặng', emoji: '🎁' },
    { value: 'star', label: '⭐ Sao', emoji: '⭐' },
];

const PRIORITY_LEVELS = [
    { value: 2, label: 'Cao', color: '#F44336', desc: 'Khẩn cấp' },
    { value: 1, label: 'Trung bình', color: '#FF9800', desc: 'Bình thường' },
    { value: 0, label: 'Thấp', color: '#4CAF50', desc: 'Không quan trọng' },
];

// ----------------------------------------------------------------------
// 🎨 MODERN STYLES WITH GRADIENT AND ANIMATIONS
// ----------------------------------------------------------------------
const styles = {
    container: {
        padding: '40px 20px',
        background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
        minHeight: '100vh',
        maxWidth: '1400px',
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
        textShadow: '2px 2px 4px rgba(0,0,0,0.2)',
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
        transition: 'transform 0.3s ease',
        cursor: 'pointer',
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
        boxShadow: '0 8px 32px rgba(0,0,0,0.2)',
    },
    
    sectionTitle: {
        color: '#ffffff',
        fontSize: '24px',
        fontWeight: '700',
        marginBottom: '25px',
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
    },
    
    formGroup: {
        marginBottom: '25px',
    },
    
    label: {
        display: 'block',
        fontWeight: '600',
        marginBottom: '10px',
        color: 'rgba(255,255,255,0.9)',
        fontSize: '14px',
        textTransform: 'uppercase',
        letterSpacing: '0.5px',
    },
    
    input: {
        padding: '14px 18px',
        border: '2px solid rgba(255,255,255,0.1)',
        borderRadius: '12px',
        width: '100%',
        backgroundColor: 'rgba(0,0,0,0.3)',
        color: '#ffffff',
        fontSize: '15px',
        transition: 'all 0.3s ease',
        boxSizing: 'border-box',
    },
    
    select: {
        padding: '14px 18px',
        border: '2px solid rgba(255,255,255,0.1)',
        borderRadius: '12px',
        width: '100%',
        backgroundColor: 'rgba(0,0,0,0.3)',
        color: '#ffffff',
        fontSize: '15px',
        cursor: 'pointer',
        boxSizing: 'border-box',
    },
    
    textArea: {
        padding: '14px 18px',
        border: '2px solid rgba(255,255,255,0.1)',
        borderRadius: '12px',
        width: '100%',
        minHeight: '120px',
        backgroundColor: 'rgba(0,0,0,0.3)',
        color: '#ffffff',
        fontSize: '15px',
        resize: 'vertical',
        fontFamily: 'inherit',
        boxSizing: 'border-box',
    },
    
    gridContainer: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
        gap: '20px',
        marginBottom: '20px',
    },
    
    buttonGroup: {
        display: 'flex',
        gap: '15px',
        marginTop: '30px',
        flexWrap: 'wrap',
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
            secondary: {
                background: 'rgba(255,255,255,0.1)',
                color: 'white',
                border: '2px solid rgba(255,255,255,0.2)',
            },
        };
        
        return {
            ...variants[variant],
            padding: '14px 32px',
            border: 'none',
            borderRadius: '12px',
            cursor: 'pointer',
            fontWeight: '600',
            fontSize: '15px',
            transition: 'all 0.3s ease',
            boxShadow: '0 4px 15px rgba(0,0,0,0.2)',
        };
    },
    
    table: {
        width: '100%',
        borderCollapse: 'separate',
        borderSpacing: '0',
        marginTop: '20px',
        background: 'rgba(255,255,255,0.02)',
        borderRadius: '15px',
        overflow: 'hidden',
    },
    
    th: {
        backgroundColor: 'rgba(102, 126, 234, 0.2)',
        color: '#ffffff',
        padding: '18px 20px',
        textAlign: 'left',
        textTransform: 'uppercase',
        fontWeight: '700',
        fontSize: '13px',
        letterSpacing: '1px',
    },
    
    td: {
        padding: '16px 20px',
        borderBottom: '1px solid rgba(255,255,255,0.05)',
        color: 'rgba(255,255,255,0.9)',
        fontSize: '14px',
    },
    
    statusBadge: (status) => {
        const statusColors = {
            SENT: { bg: '#4CAF50', text: '#fff' },
            PENDING: { bg: '#FF9800', text: '#fff' },
            FAILED: { bg: '#F44336', text: '#fff' },
        };
        const colors = statusColors[status] || { bg: '#607D8B', text: '#fff' };
        
        return {
            backgroundColor: colors.bg,
            color: colors.text,
            padding: '6px 14px',
            borderRadius: '20px',
            fontSize: '12px',
            fontWeight: '700',
            display: 'inline-block',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
        };
    },
    
    priorityBadge: (priority) => {
        const level = PRIORITY_LEVELS.find(p => p.value === priority);
        return {
            backgroundColor: level?.color || '#607D8B',
            color: 'white',
            padding: '4px 10px',
            borderRadius: '12px',
            fontSize: '11px',
            fontWeight: '600',
            display: 'inline-block',
        };
    },
    
    typeBadge: (type) => {
        const typeConfig = NOTIFICATION_TYPES.find(t => t.value === type);
        return {
            backgroundColor: typeConfig?.color || '#607D8B',
            color: 'white',
            padding: '6px 12px',
            borderRadius: '15px',
            fontSize: '12px',
            fontWeight: '600',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '5px',
        };
    },
    
    actionButton: {
        padding: '8px 16px',
        border: 'none',
        borderRadius: '8px',
        cursor: 'pointer',
        fontSize: '13px',
        fontWeight: '600',
        transition: 'all 0.2s ease',
        marginRight: '8px',
    },
    
    emptyState: {
        textAlign: 'center',
        padding: '60px 20px',
        color: 'rgba(255,255,255,0.5)',
    },
    
    emptyIcon: {
        fontSize: '64px',
        marginBottom: '20px',
        opacity: 0.3,
    },
    
    filterBar: {
        display: 'flex',
        gap: '15px',
        marginBottom: '25px',
        flexWrap: 'wrap',
        alignItems: 'center',
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
    
    previewCard: {
        background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%)',
        border: '2px solid rgba(102, 126, 234, 0.3)',
        borderRadius: '15px',
        padding: '20px',
        marginTop: '20px',
    },
    
    previewTitle: {
        color: '#ffffff',
        fontSize: '16px',
        fontWeight: '700',
        marginBottom: '15px',
    },
    
    notificationPreview: {
        background: 'rgba(0,0,0,0.3)',
        borderRadius: '12px',
        padding: '15px',
        display: 'flex',
        gap: '15px',
        alignItems: 'start',
    },
    
    previewIcon: {
        fontSize: '32px',
    },
    
    previewContent: {
        flex: 1,
    },
    
    previewNotifTitle: {
        color: '#ffffff',
        fontSize: '15px',
        fontWeight: '700',
        marginBottom: '5px',
    },
    
    previewNotifBody: {
        color: 'rgba(255,255,255,0.7)',
        fontSize: '13px',
        lineHeight: '1.5',
    },
};

const NotificationManager = () => {
    // ----------------------------------------------------------------------
    // STATE MANAGEMENT
    // ----------------------------------------------------------------------
    const [formData, setFormData] = useState({
        title: '',
        body: '',
        imageUrl: '',
        type: 'SYSTEM',
        actionType: 'NONE',
        actionData: '',
        icon: 'bell',
        priority: 1,
        scheduleTime: '',
    });
    
    const [loading, setLoading] = useState(false);
    const [history, setHistory] = useState([]);
    const [stats, setStats] = useState({ total: 0, sent: 0, pending: 0, failed: 0 });
    const [filter, setFilter] = useState({ status: 'ALL', type: 'ALL', search: '' });

    // ----------------------------------------------------------------------
    // FETCH HISTORY & STATS
    // ----------------------------------------------------------------------
    const fetchHistory = async () => {
        try {
            const notifCollectionRef = collection(db, "scheduledNotifications");
            const q = query(notifCollectionRef, orderBy("createdAt", "desc"));
            const snapshot = await getDocs(q);

            const historyList = snapshot.docs.map(doc => ({
                id: doc.id,
                ...doc.data(),
                createdAt: doc.data().createdAt?.toMillis ? doc.data().createdAt.toMillis() : doc.data().createdAt,
                scheduleTime: doc.data().scheduleTime?.toMillis ? doc.data().scheduleTime.toMillis() : doc.data().scheduleTime,
            }));

            setHistory(historyList);
            
            // Calculate stats
            const statsData = {
                total: historyList.length,
                sent: historyList.filter(n => n.status === 'SENT').length,
                pending: historyList.filter(n => n.status === 'PENDING').length,
                failed: historyList.filter(n => n.status === 'FAILED').length,
            };
            setStats(statsData);
        } catch (err) {
            console.error("Lỗi tải lịch sử thông báo:", err);
        }
    };

    useEffect(() => {
        fetchHistory();
    }, []);

    // ----------------------------------------------------------------------
    // FORM HANDLERS
    // ----------------------------------------------------------------------
    const handleInputChange = (field, value) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!formData.title || !formData.body) {
            alert('Vui lòng nhập đầy đủ tiêu đề và nội dung!');
            return;
        }

        setLoading(true);
        const isScheduled = !!formData.scheduleTime;
        
        let scheduledTimestamp = null;
        if (isScheduled) {
            scheduledTimestamp = new Date(formData.scheduleTime).getTime();
            if (scheduledTimestamp < Date.now()) {
                alert("Thời gian đặt lịch phải là tương lai!");
                setLoading(false);
                return;
            }
        }

        const notificationData = {
            title: formData.title,
            body: formData.body,
            type: formData.type,
            actionType: formData.actionType,
            actionData: formData.actionData || '',
            icon: formData.icon,
            priority: formData.priority,
            imageUrl: formData.imageUrl || '',
            scheduleTime: scheduledTimestamp || Date.now(),
            status: 'PENDING',
            createdAt: Date.now(),
        };

        try {
            const collectionRef = collection(db, "scheduledNotifications");
            await addDoc(collectionRef, notificationData);

            alert(isScheduled 
                ? `✅ Đã đặt lịch thành công: ${new Date(scheduledTimestamp).toLocaleString('vi-VN')}` 
                : '✅ Đã gửi yêu cầu thông báo ngay lập tức!');
            
            // Reset form
            setFormData({
                title: '',
                body: '',
                imageUrl: '',
                type: 'SYSTEM',
                actionType: 'NONE',
                actionData: '',
                icon: 'bell',
                priority: 1,
                scheduleTime: '',
            });

            fetchHistory();
        } catch (error) {
            console.error("Lỗi khi tạo thông báo:", error);
            alert("❌ Lỗi: " + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Bạn có chắc muốn xóa thông báo này?')) return;
        
        try {
            await deleteDoc(doc(db, "scheduledNotifications", id));
            alert('✅ Đã xóa thành công!');
            fetchHistory();
        } catch (error) {
            console.error("Lỗi xóa:", error);
            alert("❌ Lỗi khi xóa: " + error.message);
        }
    };

    const handleResend = async (notification) => {
        if (!window.confirm('Gửi lại thông báo này?')) return;
        
        try {
            const newNotif = {
                ...notification,
                status: 'PENDING',
                scheduleTime: Date.now(),
                createdAt: Date.now(),
            };
            delete newNotif.id;
            
            await addDoc(collection(db, "scheduledNotifications"), newNotif);
            alert('✅ Đã thêm vào hàng đợi gửi!');
            fetchHistory();
        } catch (error) {
            console.error("Lỗi gửi lại:", error);
            alert("❌ Lỗi: " + error.message);
        }
    };

    // ----------------------------------------------------------------------
    // FILTER LOGIC
    // ----------------------------------------------------------------------
    const filteredHistory = history.filter(item => {
        const matchStatus = filter.status === 'ALL' || item.status === filter.status;
        const matchType = filter.type === 'ALL' || item.type === filter.type;
        const matchSearch = !filter.search || 
            item.title.toLowerCase().includes(filter.search.toLowerCase()) ||
            item.body.toLowerCase().includes(filter.search.toLowerCase());
        
        return matchStatus && matchType && matchSearch;
    });

    // ----------------------------------------------------------------------
    // RENDER
    // ----------------------------------------------------------------------
    const selectedType = NOTIFICATION_TYPES.find(t => t.value === formData.type);
    const selectedIcon = ICON_TYPES.find(i => i.value === formData.icon);

    return (
        <div style={styles.container}>
            {/* HEADER */}
            <div style={styles.header}>
                <h1 style={styles.title}>📱 Quản Trị Thông Báo FCM</h1>
                <p style={styles.subtitle}>Tạo, lên lịch và quản lý push notifications cho ứng dụng mobile</p>
            </div>

            {/* STATISTICS */}
            <div style={styles.statsContainer}>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.total}</div>
                    <div style={styles.statLabel}>📊 Tổng số</div>
                </div>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.sent}</div>
                    <div style={styles.statLabel}>✅ Đã gửi</div>
                </div>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.pending}</div>
                    <div style={styles.statLabel}>⏳ Chờ gửi</div>
                </div>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.failed}</div>
                    <div style={styles.statLabel}>❌ Thất bại</div>
                </div>
            </div>

            {/* CREATE FORM */}
            <div style={styles.formSection}>
                <h2 style={styles.sectionTitle}>
                    <span>✍️</span>
                    <span>Tạo Thông Báo Mới</span>
                </h2>
                
                <form onSubmit={handleSubmit}>
                    {/* Basic Info */}
                    <div style={styles.gridContainer}>
                        <div style={styles.formGroup}>
                            <label style={styles.label}>📝 Tiêu đề</label>
                            <input
                                type="text"
                                value={formData.title}
                                onChange={(e) => handleInputChange('title', e.target.value)}
                                style={styles.input}
                                placeholder="Nhập tiêu đề thông báo..."
                                maxLength={50}
                                required
                            />
                        </div>
                    </div>

                    <div style={styles.formGroup}>
                        <label style={styles.label}>📄 Nội dung</label>
                        <textarea
                            value={formData.body}
                            onChange={(e) => handleInputChange('body', e.target.value)}
                            style={styles.textArea}
                            placeholder="Nhập nội dung chi tiết..."
                            required
                        />
                    </div>

                    {/* Configuration */}
                    <div style={styles.gridContainer}>
                        <div style={styles.formGroup}>
                            <label style={styles.label}>🏷️ Loại thông báo</label>
                            <select
                                value={formData.type}
                                onChange={(e) => handleInputChange('type', e.target.value)}
                                style={styles.select}
                            >
                                {NOTIFICATION_TYPES.map(type => (
                                    <option key={type.value} value={type.value}>
                                        {type.label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div style={styles.formGroup}>
                            <label style={styles.label}>🎯 Hành động</label>
                            <select
                                value={formData.actionType}
                                onChange={(e) => handleInputChange('actionType', e.target.value)}
                                style={styles.select}
                            >
                                {ACTION_TYPES.map(action => (
                                    <option key={action.value} value={action.value}>
                                        {action.label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div style={styles.formGroup}>
                            <label style={styles.label}>🎨 Icon</label>
                            <select
                                value={formData.icon}
                                onChange={(e) => handleInputChange('icon', e.target.value)}
                                style={styles.select}
                            >
                                {ICON_TYPES.map(icon => (
                                    <option key={icon.value} value={icon.value}>
                                        {icon.label}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div style={styles.gridContainer}>
                        <div style={styles.formGroup}>
                            <label style={styles.label}>🔗 Dữ liệu hành động</label>
                            <input
                                type="text"
                                value={formData.actionData}
                                onChange={(e) => handleInputChange('actionData', e.target.value)}
                                style={styles.input}
                                placeholder="ID đơn hàng, URL, v.v..."
                            />
                        </div>

                        <div style={styles.formGroup}>
                            <label style={styles.label}>⚡ Mức độ ưu tiên</label>
                            <select
                                value={formData.priority}
                                onChange={(e) => handleInputChange('priority', Number(e.target.value))}
                                style={styles.select}
                            >
                                {PRIORITY_LEVELS.map(level => (
                                    <option key={level.value} value={level.value}>
                                        {level.label} - {level.desc}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div style={styles.formGroup}>
                            <label style={styles.label}>🖼️ URL Hình ảnh</label>
                            <input
                                type="url"
                                value={formData.imageUrl}
                                onChange={(e) => handleInputChange('imageUrl', e.target.value)}
                                style={styles.input}
                                placeholder="https://example.com/image.jpg"
                            />
                        </div>
                    </div>

                    <div style={styles.formGroup}>
                        <label style={styles.label}>⏰ Đặt lịch gửi (Tùy chọn)</label>
                        <input
                            type="datetime-local"
                            value={formData.scheduleTime}
                            onChange={(e) => handleInputChange('scheduleTime', e.target.value)}
                            style={styles.input}
                            min={new Date(Date.now() - 60000).toISOString().slice(0, 16)}
                        />
                        <small style={{ color: 'rgba(255,255,255,0.6)', marginTop: '8px', display: 'block' }}>
                            💡 Bỏ trống để gửi ngay lập tức
                        </small>
                    </div>

                    {/* Preview */}
                    {formData.title && formData.body && (
                        <div style={styles.previewCard}>
                            <div style={styles.previewTitle}>👁️ Xem trước thông báo</div>
                            <div style={styles.notificationPreview}>
                                <div style={styles.previewIcon}>{selectedIcon?.emoji || '🔔'}</div>
                                <div style={styles.previewContent}>
                                    <div style={styles.previewNotifTitle}>{formData.title}</div>
                                    <div style={styles.previewNotifBody}>{formData.body}</div>
                                    {formData.imageUrl && (
                                        <img 
                                            src={formData.imageUrl} 
                                            alt="Preview" 
                                            style={{ 
                                                width: '100%', 
                                                maxHeight: '150px', 
                                                objectFit: 'cover', 
                                                borderRadius: '8px', 
                                                marginTop: '10px' 
                                            }}
                                            onError={(e) => e.target.style.display = 'none'}
                                        />
                                    )}
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Buttons */}
                    <div style={styles.buttonGroup}>
                        <button 
                            type="submit" 
                            style={styles.button('primary')} 
                            disabled={loading}
                        >
                            {loading ? '⏳ Đang xử lý...' : (formData.scheduleTime ? '📅 Đặt lịch' : '🚀 Gửi ngay')}
                        </button>
                        <button
                            type="button"
                            style={styles.button('secondary')}
                            onClick={() => setFormData({
                                title: '',
                                body: '',
                                imageUrl: '',
                                type: 'SYSTEM',
                                actionType: 'NONE',
                                actionData: '',
                                icon: 'bell',
                                priority: 1,
                                scheduleTime: '',
                            })}
                        >
                            🔄 Làm mới
                        </button>
                    </div>
                </form>
            </div>

            {/* HISTORY TABLE */}
            <div style={styles.formSection}>
                <h2 style={styles.sectionTitle}>
                    <span>📜</span>
                    <span>Lịch Sử Thông Báo</span>
                </h2>

                {/* Filters */}
                <div style={styles.filterBar}>
                    <input
                        type="text"
                        placeholder="🔍 Tìm kiếm..."
                        value={filter.search}
                        onChange={(e) => setFilter(prev => ({ ...prev, search: e.target.value }))}
                        style={styles.searchInput}
                    />
                    <select
                        value={filter.status}
                        onChange={(e) => setFilter(prev => ({ ...prev, status: e.target.value }))}
                        style={{ ...styles.select, flex: '0 0 150px' }}
                    >
                        <option value="ALL">Tất cả trạng thái</option>
                        <option value="SENT">✅ Đã gửi</option>
                        <option value="PENDING">⏳ Chờ gửi</option>
                        <option value="FAILED">❌ Thất bại</option>
                    </select>
                    <select
                        value={filter.type}
                        onChange={(e) => setFilter(prev => ({ ...prev, type: e.target.value }))}
                        style={{ ...styles.select, flex: '0 0 150px' }}
                    >
                        <option value="ALL">Tất cả loại</option>
                        {NOTIFICATION_TYPES.map(type => (
                            <option key={type.value} value={type.value}>{type.label}</option>
                        ))}
                    </select>
                </div>

                {filteredHistory.length === 0 ? (
                    <div style={styles.emptyState}>
                        <div style={styles.emptyIcon}>📭</div>
                        <div style={{ fontSize: '18px', marginBottom: '10px' }}>
                            Chưa có thông báo nào
                        </div>
                        <div style={{ fontSize: '14px' }}>
                            Tạo thông báo đầu tiên của bạn!
                        </div>
                    </div>
                ) : (
                    <div style={{ overflowX: 'auto' }}>
                        <table style={styles.table}>
                            <thead>
                                <tr>
                                    <th style={styles.th}>Thời gian</th>
                                    <th style={styles.th}>Tiêu đề & Nội dung</th>
                                    <th style={styles.th}>Loại</th>
                                    <th style={styles.th}>Hành động</th>
                                    <th style={styles.th}>Ưu tiên</th>
                                    <th style={styles.th}>Trạng thái</th>
                                    <th style={styles.th}>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredHistory.map(item => (
                                    <tr key={item.id} style={{ background: 'rgba(255,255,255,0.02)' }}>
                                        <td style={styles.td}>
                                            <div style={{ fontSize: '13px' }}>
                                                {new Date(item.scheduleTime).toLocaleString('vi-VN')}
                                            </div>
                                            <div style={{ fontSize: '11px', color: 'rgba(255,255,255,0.5)', marginTop: '4px' }}>
                                                {new Date(item.createdAt).toLocaleDateString('vi-VN')}
                                            </div>
                                        </td>
                                        <td style={styles.td}>
                                            <div style={{ fontWeight: '600', marginBottom: '5px' }}>
                                                {item.title}
                                            </div>
                                            <div style={{ fontSize: '12px', color: 'rgba(255,255,255,0.6)' }}>
                                                {item.body.substring(0, 60)}...
                                            </div>
                                        </td>
                                        <td style={styles.td}>
                                            <span style={styles.typeBadge(item.type)}>
                                                {NOTIFICATION_TYPES.find(t => t.value === item.type)?.label || item.type}
                                            </span>
                                        </td>
                                        <td style={styles.td}>
                                            <div style={{ fontSize: '13px', fontWeight: '600' }}>
                                                {item.actionType}
                                            </div>
                                            {item.actionData && (
                                                <div style={{ fontSize: '11px', color: 'rgba(255,255,255,0.5)', marginTop: '3px' }}>
                                                    {item.actionData}
                                                </div>
                                            )}
                                        </td>
                                        <td style={styles.td}>
                                            <span style={styles.priorityBadge(item.priority)}>
                                                {PRIORITY_LEVELS.find(p => p.value === item.priority)?.label || item.priority}
                                            </span>
                                        </td>
                                        <td style={styles.td}>
                                            <span style={styles.statusBadge(item.status)}>
                                                {item.status}
                                            </span>
                                        </td>
                                        <td style={styles.td}>
                                            <button
                                                onClick={() => handleResend(item)}
                                                style={{
                                                    ...styles.actionButton,
                                                    background: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
                                                    color: 'white',
                                                }}
                                                title="Gửi lại"
                                            >
                                                🔄
                                            </button>
                                            <button
                                                onClick={() => handleDelete(item.id)}
                                                style={{
                                                    ...styles.actionButton,
                                                    background: 'linear-gradient(135deg, #eb3349 0%, #f45c43 100%)',
                                                    color: 'white',
                                                }}
                                                title="Xóa"
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

export default NotificationManager;