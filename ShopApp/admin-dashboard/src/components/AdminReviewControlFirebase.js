import React, { useState, useEffect } from 'react';
import { collection, addDoc, getDocs, orderBy, query, doc, updateDoc, deleteDoc, where } from 'firebase/firestore';
import { db } from '../firebaseConfig';

// ======================================================================
// 🎨 NOTIFICATION TYPES & CONFIGURATIONS
// ======================================================================
const REVIEW_STATUS = [
    { value: 'APPROVED', label: '✅ Đã duyệt', color: '#4CAF50' },
    { value: 'PENDING', label: '⏳ Chờ duyệt', color: '#FF9800' },
    { value: 'REJECTED', label: '❌ Từ chối', color: '#F44336' },
];

const RATING_LEVELS = [
    { value: 5, label: '⭐⭐⭐⭐⭐ 5 Sao - Tuyệt vời', color: '#FFC107' },
    { value: 4, label: '⭐⭐⭐⭐ 4 Sao - Tốt', color: '#8BC34A' },
    { value: 3, label: '⭐⭐⭐ 3 Sao - Bình thường', color: '#FF9800' },
    { value: 2, label: '⭐⭐ 2 Sao - Kém', color: '#FF6F00' },
    { value: 1, label: '⭐ 1 Sao - Tệ', color: '#F44336' },
];

// ======================================================================
// 🎨 MODERN STYLES WITH GRADIENT
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
        boxSizing: 'border-box',
    },

    select: {
        padding: '12px 18px',
        border: '2px solid rgba(255,255,255,0.1)',
        borderRadius: '12px',
        backgroundColor: 'rgba(0,0,0,0.3)',
        color: '#ffffff',
        fontSize: '14px',
        cursor: 'pointer',
        boxSizing: 'border-box',
        flex: '0 0 auto',
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
        const statusConfig = REVIEW_STATUS.find(s => s.value === status);
        return {
            backgroundColor: statusConfig?.color || '#607D8B',
            color: 'white',
            padding: '6px 14px',
            borderRadius: '20px',
            fontSize: '12px',
            fontWeight: '700',
            display: 'inline-block',
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
        };
    },

    ratingBadge: (rating) => {
        const ratingConfig = RATING_LEVELS.find(r => r.value === rating);
        return {
            backgroundColor: ratingConfig?.color || '#607D8B',
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
        marginBottom: '8px',
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
            padding: '8px 16px',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer',
            fontWeight: '600',
            fontSize: '13px',
            transition: 'all 0.2s ease',
        };
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

    modalOverlay: {
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.7)',
        backdropFilter: 'blur(5px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '20px',
    },

    modal: {
        background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
        borderRadius: '20px',
        maxWidth: '600px',
        width: '100%',
        maxHeight: '85vh',
        overflowY: 'auto',
        border: '1px solid rgba(255,255,255,0.1)',
        boxShadow: '0 20px 60px rgba(0,0,0,0.4)',
    },

    modalHeader: {
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: '25px 30px',
        borderBottom: '1px solid rgba(255,255,255,0.1)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderRadius: '20px 20px 0 0',
    },

    modalTitle: {
        color: '#ffffff',
        fontSize: '22px',
        fontWeight: '700',
        margin: 0,
    },

    closeButton: {
        background: 'rgba(255,255,255,0.2)',
        border: 'none',
        color: '#ffffff',
        fontSize: '24px',
        cursor: 'pointer',
        padding: '0 10px',
        borderRadius: '8px',
    },

    modalBody: {
        padding: '30px',
    },

    infoRow: {
        marginBottom: '20px',
        paddingBottom: '20px',
        borderBottom: '1px solid rgba(255,255,255,0.05)',
    },

    infoLabel: {
        color: 'rgba(255,255,255,0.6)',
        fontSize: '12px',
        textTransform: 'uppercase',
        fontWeight: '600',
        letterSpacing: '1px',
        marginBottom: '8px',
    },

    infoValue: {
        color: '#ffffff',
        fontSize: '15px',
        fontWeight: '500',
    },

    modalActions: {
        display: 'flex',
        gap: '10px',
        marginTop: '25px',
        padding: '20px 30px 30px',
        borderTop: '1px solid rgba(255,255,255,0.1)',
        flexWrap: 'wrap',
    },
};

// ======================================================================
// 🎯 MAIN COMPONENT
// ======================================================================
const AdminReviewControl = () => {
    // STATE
    const [reviews, setReviews] = useState([]);
    const [filteredReviews, setFilteredReviews] = useState([]);
    const [loading, setLoading] = useState(false);
    const [stats, setStats] = useState({
        total: 0,
        approved: 0,
        pending: 0,
        rejected: 0,
        avgRating: 0,
        fiveStars: 0,
        oneStar: 0,
    });

    const [filter, setFilter] = useState({
        status: 'ALL',
        rating: 'ALL',
        search: '',
    });

    const [selectedReview, setSelectedReview] = useState(null);
    const [showModal, setShowModal] = useState(false);

    // ======================================================================
    // FETCH REVIEWS FROM NESTED COLLECTION
    // ======================================================================
    const fetchReviews = async () => {
        setLoading(true);
        try {
            const productsRef = collection(db, 'products');
            const productsSnap = await getDocs(productsRef);
            
            let allReviews = [];
            
            for (const productDoc of productsSnap.docs) {
                const reviewsRef = collection(db, 'products', productDoc.id, 'reviews');
                const q = query(reviewsRef, orderBy('timestamp', 'desc'));
                const reviewsSnap = await getDocs(q);
                
                const reviews = reviewsSnap.docs.map(doc => ({
                    id: doc.id,
                    ...doc.data(),
                    productId: productDoc.id,
                    timestamp: doc.data().timestamp?.toMillis ? doc.data().timestamp.toMillis() : doc.data().timestamp,
                    updatedAt: doc.data().updatedAt?.toMillis ? doc.data().updatedAt.toMillis() : doc.data().updatedAt,
                }));
                
                allReviews = [...allReviews, ...reviews];
            }
            
            // Sort all reviews by timestamp
            allReviews.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));

            setReviews(allReviews);
            calculateStats(allReviews);
        } catch (err) {
            console.error('Lỗi tải review:', err);
            alert('❌ Lỗi khi tải dữ liệu: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    // Calculate statistics
    const calculateStats = (reviewsList) => {
        const statsData = {
            total: reviewsList.length,
            approved: reviewsList.filter(r => r.status === 'APPROVED').length,
            pending: reviewsList.filter(r => r.status === 'PENDING').length,
            rejected: reviewsList.filter(r => r.status === 'REJECTED').length,
            avgRating: reviewsList.length > 0 
                ? (reviewsList.reduce((sum, r) => sum + r.rating, 0) / reviewsList.length).toFixed(1)
                : 0,
            fiveStars: reviewsList.filter(r => r.rating === 5).length,
            oneStar: reviewsList.filter(r => r.rating === 1).length,
        };
        setStats(statsData);
    };

    // Load reviews on mount
    useEffect(() => {
        fetchReviews();
    }, []);

    // ======================================================================
    // FILTER LOGIC
    // ======================================================================
    useEffect(() => {
        let filtered = reviews;

        if (filter.status !== 'ALL') {
            filtered = filtered.filter(r => r.status === filter.status);
        }

        if (filter.rating !== 'ALL') {
            filtered = filtered.filter(r => r.rating === parseInt(filter.rating));
        }

        if (filter.search) {
            filtered = filtered.filter(r =>
                r.userName?.toLowerCase().includes(filter.search.toLowerCase()) ||
                r.comment?.toLowerCase().includes(filter.search.toLowerCase()) ||
                r.productId?.toLowerCase().includes(filter.search.toLowerCase())
            );
        }

        setFilteredReviews(filtered);
    }, [reviews, filter]);

    // ======================================================================
    // ACTIONS
    // ======================================================================
    const handleApprove = async (review) => {
        try {
            await updateDoc(doc(db, 'products', review.productId, 'reviews', review.id), {
                status: 'APPROVED',
                updatedAt: Date.now(),
            });
            alert('✅ Đã duyệt review!');
            setShowModal(false);
            fetchReviews();
        } catch (err) {
            console.error('Lỗi duyệt review:', err);
            alert('❌ Lỗi: ' + err.message);
        }
    };

    const handleReject = async (review) => {
        try {
            await updateDoc(doc(db, 'products', review.productId, 'reviews', review.id), {
                status: 'REJECTED',
                updatedAt: Date.now(),
            });
            alert('✅ Đã từ chối review!');
            setShowModal(false);
            fetchReviews();
        } catch (err) {
            console.error('Lỗi từ chối review:', err);
            alert('❌ Lỗi: ' + err.message);
        }
    };

    const handleDelete = async (review) => {
        if (!window.confirm('Bạn chắc chắn muốn xóa review này?')) return;

        try {
            await deleteDoc(doc(db, 'products', review.productId, 'reviews', review.id));
            alert('✅ Đã xóa review!');
            setShowModal(false);
            fetchReviews();
        } catch (err) {
            console.error('Lỗi xóa review:', err);
            alert('❌ Lỗi: ' + err.message);
        }
    };

    // ======================================================================
    // RENDER FUNCTIONS
    // ======================================================================
    const formatDate = (timestamp) => {
        return new Date(timestamp).toLocaleString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    const renderStars = (rating) => {
        return '⭐'.repeat(Math.floor(rating));
    };

    return (
        <div style={styles.container}>
            {/* HEADER */}
            <div style={styles.header}>
                <h1 style={styles.title}>⭐ Quản Lý Review Sản Phẩm</h1>
                <p style={styles.subtitle}>Kiểm duyệt và quản lý đánh giá từ khách hàng</p>
            </div>

            {/* STATISTICS */}
            <div style={styles.statsContainer}>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.total}</div>
                    <div style={styles.statLabel}>📊 Tổng Review</div>
                </div>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.approved}</div>
                    <div style={styles.statLabel}>✅ Đã Duyệt</div>
                </div>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.pending}</div>
                    <div style={styles.statLabel}>⏳ Chờ Duyệt</div>
                </div>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.rejected}</div>
                    <div style={styles.statLabel}>❌ Từ Chối</div>
                </div>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.avgRating}</div>
                    <div style={styles.statLabel}>⭐ Đánh Giá TB</div>
                </div>
            </div>

            {/* REVIEWS SECTION */}
            <div style={styles.formSection}>
                <h2 style={styles.sectionTitle}>
                    <span>📜</span>
                    <span>Danh Sách Review ({filteredReviews.length})</span>
                </h2>

                {/* FILTERS */}
                <div style={styles.filterBar}>
                    <input
                        type="text"
                        placeholder="🔍 Tìm kiếm theo tên, sản phẩm..."
                        value={filter.search}
                        onChange={(e) => setFilter(prev => ({ ...prev, search: e.target.value }))}
                        style={styles.searchInput}
                    />
                    <select
                        value={filter.status}
                        onChange={(e) => setFilter(prev => ({ ...prev, status: e.target.value }))}
                        style={styles.select}
                    >
                        <option value="ALL">Tất cả trạng thái</option>
                        {REVIEW_STATUS.map(status => (
                            <option key={status.value} value={status.value}>{status.label}</option>
                        ))}
                    </select>
                    <select
                        value={filter.rating}
                        onChange={(e) => setFilter(prev => ({ ...prev, rating: e.target.value }))}
                        style={styles.select}
                    >
                        <option value="ALL">Tất cả đánh giá</option>
                        {RATING_LEVELS.map(level => (
                            <option key={level.value} value={level.value}>{level.label}</option>
                        ))}
                    </select>
                </div>

                {/* TABLE */}
                {loading ? (
                    <div style={styles.emptyState}>
                        <div style={styles.emptyIcon}>⏳</div>
                        <div>Đang tải dữ liệu...</div>
                    </div>
                ) : filteredReviews.length === 0 ? (
                    <div style={styles.emptyState}>
                        <div style={styles.emptyIcon}>📭</div>
                        <div>Không có review nào</div>
                    </div>
                ) : (
                    <div style={{ overflowX: 'auto' }}>
                        <table style={styles.table}>
                            <thead>
                                <tr>
                                    <th style={styles.th}>Người Dùng</th>
                                    <th style={styles.th}>Sản Phẩm</th>
                                    <th style={styles.th}>Đánh Giá</th>
                                    <th style={styles.th}>Nội Dung</th>
                                    <th style={styles.th}>Trạng Thái</th>
                                    <th style={styles.th}>Ngày Tạo</th>
                                    <th style={styles.th}>Thao Tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredReviews.map(review => (
                                    <tr key={review.id} style={{ background: 'rgba(255,255,255,0.02)' }}>
                                        <td style={styles.td}>
                                            <div style={{ fontWeight: '600' }}>{review.userName}</div>
                                            <div style={{ fontSize: '12px', color: 'rgba(255,255,255,0.5)' }}>{review.userId}</div>
                                        </td>
                                        <td style={styles.td}>
                                            <div style={{ fontWeight: '600' }}>{review.productId}</div>
                                        </td>
                                        <td style={styles.td}>
                                            <span style={styles.ratingBadge(review.rating)}>
                                                {renderStars(review.rating)}
                                            </span>
                                        </td>
                                        <td style={styles.td}>
                                            <div style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                                {review.comment}
                                            </div>
                                        </td>
                                        <td style={styles.td}>
                                            <span style={styles.statusBadge(review.status)}>
                                                {REVIEW_STATUS.find(s => s.value === review.status)?.label || review.status}
                                            </span>
                                        </td>
                                        <td style={styles.td}>
                                            <div style={{ fontSize: '13px' }}>{formatDate(review.timestamp)}</div>
                                        </td>
                                        <td style={styles.td}>
                                            <button
                                                onClick={() => {
                                                    setSelectedReview(review);
                                                    setShowModal(true);
                                                }}
                                                style={styles.button('primary')}
                                            >
                                                👁️ Xem
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* MODAL - DETAIL VIEW */}
            {showModal && selectedReview && (
                <div style={styles.modalOverlay} onClick={() => setShowModal(false)}>
                    <div style={styles.modal} onClick={(e) => e.stopPropagation()}>
                        <div style={styles.modalHeader}>
                            <h2 style={styles.modalTitle}>Chi Tiết Review</h2>
                            <button style={styles.closeButton} onClick={() => setShowModal(false)}>✕</button>
                        </div>

                        <div style={styles.modalBody}>
                            <div style={styles.infoRow}>
                                <div style={styles.infoLabel}>👤 Người Dùng</div>
                                <div style={styles.infoValue}>{selectedReview.userName}</div>
                                <div style={{ fontSize: '12px', color: 'rgba(255,255,255,0.5)', marginTop: '5px' }}>
                                    ID: {selectedReview.userId}
                                </div>
                            </div>

                            <div style={styles.infoRow}>
                                <div style={styles.infoLabel}>📦 Sản Phẩm</div>
                                <div style={styles.infoValue}>{selectedReview.productId}</div>
                            </div>

                            <div style={styles.infoRow}>
                                <div style={styles.infoLabel}>⭐ Đánh Giá</div>
                                <span style={styles.ratingBadge(selectedReview.rating)}>
                                    {renderStars(selectedReview.rating)} {selectedReview.rating}/5
                                </span>
                            </div>

                            <div style={styles.infoRow}>
                                <div style={styles.infoLabel}>💬 Nội Dung</div>
                                <div style={styles.infoValue}>{selectedReview.comment}</div>
                            </div>

                            <div style={styles.infoRow}>
                                <div style={styles.infoLabel}>📊 Trạng Thái Hiện Tại</div>
                                <span style={styles.statusBadge(selectedReview.status)}>
                                    {REVIEW_STATUS.find(s => s.value === selectedReview.status)?.label || selectedReview.status}
                                </span>
                            </div>

                            <div style={styles.infoRow}>
                                <div style={styles.infoLabel}>📅 Thời Gian</div>
                                <div style={styles.infoValue}>{formatDate(selectedReview.timestamp)}</div>
                                {selectedReview.updatedAt && (
                                    <div style={{ fontSize: '12px', color: 'rgba(255,255,255,0.5)', marginTop: '8px' }}>
                                        Cập nhật: {formatDate(selectedReview.updatedAt)}
                                    </div>
                                )}
                            </div>

                            <div style={styles.infoRow} style={{ borderBottom: 'none' }}>
                                <div style={styles.infoLabel}>🔐 Mã Đơn Hàng</div>
                                <div style={styles.infoValue}>{selectedReview.orderId}</div>
                            </div>
                        </div>

                        <div style={styles.modalActions}>
                            {selectedReview.status !== 'APPROVED' && (
                                <button
                                    onClick={() => handleApprove(selectedReview)}
                                    style={styles.button('success')}
                                >
                                    ✓ Duyệt
                                </button>
                            )}
                            {selectedReview.status !== 'REJECTED' && (
                                <button
                                    onClick={() => handleReject(selectedReview)}
                                    style={styles.button('danger')}
                                >
                                    ✕ Từ Chối
                                </button>
                            )}
                            <button
                                onClick={() => handleDelete(selectedReview)}
                                style={{
                                    ...styles.button('danger'),
                                    flex: 1,
                                }}
                            >
                                🗑️ Xóa
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminReviewControl;