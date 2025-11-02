// src/components/Dashboard.js
import React, { useState, useEffect } from 'react';
import { collection, getDocs } from 'firebase/firestore';
import { db } from '../firebaseConfig';
import { formatCurrency } from '../utils/format';
import { LineChart, Line, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const styles = {
    container: {
        padding: '30px',
        backgroundColor: '#1A1A1A',
        minHeight: '100vh',
        color: '#E0E0E0',
        fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    },
    
    header: {
        marginBottom: '40px',
        borderBottom: '3px solid #C40000',
        paddingBottom: '20px',
    },
    
    title: {
        fontSize: '42px',
        fontWeight: '800',
        color: '#ffffff',
        margin: '0 0 10px 0',
        textShadow: '2px 2px 4px rgba(0,0,0,0.2)',
    },
    
    subtitle: {
        fontSize: '16px',
        color: 'rgba(255,255,255,0.7)',
        margin: 0,
    },
    
    statsGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '20px',
        marginBottom: '40px',
    },
    
    statCard: {
        background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
        border: '1px solid rgba(196, 0, 0, 0.3)',
        borderRadius: '12px',
        padding: '25px',
        boxShadow: '0 4px 15px rgba(196, 0, 0, 0.1)',
        transition: 'all 0.3s ease',
    },
    
    statValue: {
        fontSize: '36px',
        fontWeight: 'bold',
        color: '#C40000',
        marginBottom: '10px',
    },
    
    statLabel: {
        fontSize: '14px',
        color: '#A0A0A0',
        textTransform: 'uppercase',
        letterSpacing: '1px',
    },
    
    statChange: (positive) => ({
        fontSize: '12px',
        color: positive ? '#28a745' : '#dc3545',
        marginTop: '8px',
        fontWeight: '600',
    }),
    
    chartsGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
        gap: '20px',
        marginBottom: '40px',
    },
    
    chartCard: {
        background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
        border: '1px solid rgba(196, 0, 0, 0.2)',
        borderRadius: '12px',
        padding: '25px',
        boxShadow: '0 4px 15px rgba(0,0,0,0.2)',
    },
    
    chartTitle: {
        fontSize: '18px',
        fontWeight: '700',
        color: '#ffffff',
        marginBottom: '20px',
        paddingBottom: '10px',
        borderBottom: '2px solid #C40000',
    },
    
    detailsGrid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
        gap: '20px',
    },
    
    detailCard: {
        background: 'rgba(255,255,255,0.02)',
        border: '1px solid rgba(255,255,255,0.1)',
        borderRadius: '12px',
        padding: '20px',
    },
    
    detailTitle: {
        fontSize: '16px',
        fontWeight: '700',
        color: '#C40000',
        marginBottom: '15px',
    },
    
    detailItem: {
        display: 'flex',
        justifyContent: 'space-between',
        padding: '10px 0',
        borderBottom: '1px solid rgba(255,255,255,0.05)',
        fontSize: '14px',
    },
    
    detailLabel: {
        color: '#A0A0A0',
    },
    
    detailValue: {
        color: '#ffffff',
        fontWeight: '600',
    },
};

const COLORS = ['#C40000', '#2196F3', '#4CAF50', '#FF9800', '#9C27B0'];

const Dashboard = () => {
    const [stats, setStats] = useState({
        products: { total: 0, active: 0, withOffer: 0 },
        users: { total: 0, active: 0, locked: 0 },
        orders: { total: 0, revenue: 0, average: 0, delivered: 0, pending: 0 },
        reviews: { total: 0, approved: 0, pending: 0, averageRating: 0 },
    });
    
    const [chartData, setChartData] = useState({
        ordersByStatus: [],
        ordersByDate: [],
        productsByStatus: [],
        usersByRole: [],
    });
    
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchAllData();
    }, []);

    const fetchAllData = async () => {
        try {
            setLoading(true);
            
            // Fetch Products
            const productsRef = collection(db, 'products');
            const productSnap = await getDocs(productsRef);
            const products = productSnap.docs.map(doc => doc.data());
            
            // Fetch Users
            const usersRef = collection(db, 'users');
            const userSnap = await getDocs(usersRef);
            const users = userSnap.docs.map(doc => ({
                id: doc.id,
                ...doc.data(),
            }));
            
            // Fetch Orders
            const ordersRef = collection(db, 'orders');
            const orderSnap = await getDocs(ordersRef);
            const orders = orderSnap.docs.map(doc => ({
                id: doc.id,
                ...doc.data(),
            }));
            
            // Fetch Reviews
            const reviewsList = [];
            const productsCollectionRef = collection(db, 'products');
            const productsSnap = await getDocs(productsCollectionRef);
            
            for (const productDoc of productsSnap.docs) {
                const reviewsRef = collection(db, 'products', productDoc.id, 'reviews');
                const reviewsSnap = await getDocs(reviewsRef);
                reviewsList.push(...reviewsSnap.docs.map(doc => ({
                    ...doc.data(),
                    productId: productDoc.id
                })));
            }
            
            // Calculate Product Stats
            const productStats = {
                total: products.length,
                active: products.filter(p => p.status === 'Active').length,
                withOffer: products.filter(p => p.isOffer === true).length,
            };
            
            // Calculate User Stats
            const userStats = {
                total: users.length,
                active: users.filter(u => !u.disabled).length,
                locked: users.filter(u => u.disabled).length,
            };
            
            // Calculate Order Stats
            const totalRevenue = orders.reduce((sum, o) => sum + (o.totalAmount || 0), 0);
            const orderStats = {
                total: orders.length,
                revenue: totalRevenue,
                average: orders.length > 0 ? totalRevenue / orders.length : 0,
                delivered: orders.filter(o => o.orderStatus === 'DELIVERED').length,
                pending: orders.filter(o => o.orderStatus === 'PENDING').length,
            };
            
            // Calculate Review Stats
            const reviewStats = {
                total: reviewsList.length,
                approved: reviewsList.filter(r => r.status === 'APPROVED').length,
                pending: reviewsList.filter(r => r.status === 'PENDING').length,
                averageRating: reviewsList.length > 0 
                    ? (reviewsList.reduce((sum, r) => sum + r.rating, 0) / reviewsList.length).toFixed(1)
                    : 0,
            };
            
            // Prepare Chart Data
            const ordersByStatus = [
                { name: 'Pending', value: orders.filter(o => o.orderStatus === 'PENDING').length, color: '#FF9800' },
                { name: 'Processing', value: orders.filter(o => o.orderStatus === 'PROCESSING').length, color: '#2196F3' },
                { name: 'Shipped', value: orders.filter(o => o.orderStatus === 'SHIPPED').length, color: '#9C27B0' },
                { name: 'Delivered', value: orders.filter(o => o.orderStatus === 'DELIVERED').length, color: '#4CAF50' },
                { name: 'Cancelled', value: orders.filter(o => o.orderStatus === 'CANCELLED').length, color: '#dc3545' },
            ];
            
            const productsByStatus = [
                { name: 'Active', value: productStats.active, color: '#4CAF50' },
                { name: 'Draft', value: products.filter(p => p.status === 'Draft').length, color: '#FF9800' },
                { name: 'Archived', value: products.filter(p => p.status === 'Archived').length, color: '#dc3545' },
            ];
            
            const usersByRole = [
                { name: 'Admin', value: users.filter(u => u.role === 'Admin').length },
                { name: 'Staff', value: users.filter(u => u.role === 'Staff').length },
                { name: 'Customer', value: users.filter(u => u.role === 'Customer').length },
            ];
            
            // Orders by Date (Last 7 days)
            const last7Days = [];
            for (let i = 6; i >= 0; i--) {
                const date = new Date();
                date.setDate(date.getDate() - i);
                const dateStr = date.toLocaleDateString('vi-VN');
                const count = orders.filter(o => {
                    const oDate = new Date(o.createdAt);
                    return oDate.toLocaleDateString('vi-VN') === dateStr;
                }).length;
                last7Days.push({ date: dateStr, orders: count });
            }
            
            setStats({
                products: productStats,
                users: userStats,
                orders: orderStats,
                reviews: reviewStats,
            });
            
            setChartData({
                ordersByStatus,
                ordersByDate: last7Days,
                productsByStatus,
                usersByRole,
            });
            
        } catch (error) {
            console.error('Error fetching dashboard data:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div style={styles.container}>
                <div style={{ textAlign: 'center', padding: '100px 20px' }}>
                    <div style={{ fontSize: '48px', marginBottom: '20px' }}>⏳</div>
                    <div style={{ fontSize: '20px', color: '#A0A0A0' }}>Đang tải Dashboard...</div>
                </div>
            </div>
        );
    }

    return (
        <div style={styles.container}>
            {/* HEADER */}
            <div style={styles.header}>
                <h1 style={styles.title}>📊 Admin Dashboard</h1>
                <p style={styles.subtitle}>Thống kê và phân tích toàn bộ hệ thống quản lý cửa hàng</p>
            </div>

            {/* MAIN STATS */}
            <div style={styles.statsGrid}>
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{formatCurrency(stats.orders.revenue)}</div>
                    <div style={styles.statLabel}>💰 Tổng Doanh Thu</div>
                    <div style={styles.statChange(true)}>↑ 12% vs tháng trước</div>
                </div>
                
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.orders.total}</div>
                    <div style={styles.statLabel}>📦 Tổng Đơn Hàng</div>
                    <div style={styles.statChange(true)}>↑ {stats.orders.delivered} đã giao</div>
                </div>
                
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.products.total}</div>
                    <div style={styles.statLabel}>🏷️ Tổng Sản Phẩm</div>
                    <div style={styles.statChange(true)}>{stats.products.active} đang hoạt động</div>
                </div>
                
                <div style={styles.statCard}>
                    <div style={styles.statValue}>{stats.users.total}</div>
                    <div style={styles.statLabel}>👥 Tổng Người Dùng</div>
                    <div style={styles.statChange(true)}>{stats.users.active} hoạt động</div>
                </div>
            </div>

            {/* CHARTS */}
            <div style={styles.chartsGrid}>
                {/* Orders by Status */}
                <div style={styles.chartCard}>
                    <div style={styles.chartTitle}>📊 Đơn Hàng Theo Trạng Thái</div>
                    <ResponsiveContainer width="100%" height={300}>
                        <PieChart>
                            <Pie
                                data={chartData.ordersByStatus}
                                cx="50%"
                                cy="50%"
                                labelLine={false}
                                label={({ name, value }) => `${name}: ${value}`}
                                outerRadius={100}
                                fill="#8884d8"
                                dataKey="value"
                            >
                                {chartData.ordersByStatus.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={entry.color} />
                                ))}
                            </Pie>
                            <Tooltip formatter={(value) => value} />
                        </PieChart>
                    </ResponsiveContainer>
                </div>

                {/* Orders by Date */}
                <div style={styles.chartCard}>
                    <div style={styles.chartTitle}>📈 Đơn Hàng 7 Ngày Gần Đây</div>
                    <ResponsiveContainer width="100%" height={300}>
                        <LineChart data={chartData.ordersByDate}>
                            <CartesianGrid strokeDasharray="3 3" stroke="#444" />
                            <XAxis dataKey="date" stroke="#A0A0A0" />
                            <YAxis stroke="#A0A0A0" />
                            <Tooltip 
                                contentStyle={{ backgroundColor: '#333', border: '1px solid #C40000' }}
                                labelStyle={{ color: '#E0E0E0' }}
                            />
                            <Line 
                                type="monotone" 
                                dataKey="orders" 
                                stroke="#C40000" 
                                strokeWidth={2}
                                dot={{ fill: '#C40000' }}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                </div>

                {/* Products by Status */}
                <div style={styles.chartCard}>
                    <div style={styles.chartTitle}>📦 Sản Phẩm Theo Trạng Thái</div>
                    <ResponsiveContainer width="100%" height={300}>
                        <BarChart data={chartData.productsByStatus}>
                            <CartesianGrid strokeDasharray="3 3" stroke="#444" />
                            <XAxis dataKey="name" stroke="#A0A0A0" />
                            <YAxis stroke="#A0A0A0" />
                            <Tooltip 
                                contentStyle={{ backgroundColor: '#333', border: '1px solid #C40000' }}
                                labelStyle={{ color: '#E0E0E0' }}
                            />
                            <Bar dataKey="value" fill="#C40000" />
                        </BarChart>
                    </ResponsiveContainer>
                </div>

                {/* Users by Role */}
                <div style={styles.chartCard}>
                    <div style={styles.chartTitle}>👥 Người Dùng Theo Vai Trò</div>
                    <ResponsiveContainer width="100%" height={300}>
                        <BarChart data={chartData.usersByRole}>
                            <CartesianGrid strokeDasharray="3 3" stroke="#444" />
                            <XAxis dataKey="name" stroke="#A0A0A0" />
                            <YAxis stroke="#A0A0A0" />
                            <Tooltip 
                                contentStyle={{ backgroundColor: '#333', border: '1px solid #2196F3' }}
                                labelStyle={{ color: '#E0E0E0' }}
                            />
                            <Bar dataKey="value" fill="#2196F3" />
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </div>

            {/* DETAILED STATS */}
            <h2 style={{ fontSize: '24px', fontWeight: '700', color: '#ffffff', marginBottom: '20px', paddingBottom: '10px', borderBottom: '2px solid #C40000' }}>
                📋 Chi Tiết Thống Kê
            </h2>
            
            <div style={styles.detailsGrid}>
                {/* Order Details */}
                <div style={styles.detailCard}>
                    <div style={styles.detailTitle}>🛒 Thống Kê Đơn Hàng</div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Tổng đơn hàng:</span>
                        <span style={styles.detailValue}>{stats.orders.total}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Đã giao:</span>
                        <span style={styles.detailValue}>{stats.orders.delivered}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Đang chờ:</span>
                        <span style={styles.detailValue}>{stats.orders.pending}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Tổng doanh thu:</span>
                        <span style={styles.detailValue}>{formatCurrency(stats.orders.revenue)}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Giá trung bình:</span>
                        <span style={styles.detailValue}>{formatCurrency(stats.orders.average)}</span>
                    </div>
                </div>

                {/* Product Details */}
                <div style={styles.detailCard}>
                    <div style={styles.detailTitle}>📦 Thống Kê Sản Phẩm</div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Tổng sản phẩm:</span>
                        <span style={styles.detailValue}>{stats.products.total}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Đang hoạt động:</span>
                        <span style={styles.detailValue}>{stats.products.active}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Đang khuyến mãi:</span>
                        <span style={styles.detailValue}>{stats.products.withOffer}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Tỷ lệ active:</span>
                        <span style={styles.detailValue}>{((stats.products.active / stats.products.total) * 100).toFixed(1)}%</span>
                    </div>
                </div>

                {/* User Details */}
                <div style={styles.detailCard}>
                    <div style={styles.detailTitle}>👥 Thống Kê Người Dùng</div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Tổng người dùng:</span>
                        <span style={styles.detailValue}>{stats.users.total}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Hoạt động:</span>
                        <span style={styles.detailValue}>{stats.users.active}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Đã khóa:</span>
                        <span style={styles.detailValue}>{stats.users.locked}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Tỷ lệ hoạt động:</span>
                        <span style={styles.detailValue}>{((stats.users.active / stats.users.total) * 100).toFixed(1)}%</span>
                    </div>
                </div>

                {/* Review Details */}
                <div style={styles.detailCard}>
                    <div style={styles.detailTitle}>⭐ Thống Kê Đánh Giá</div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Tổng review:</span>
                        <span style={styles.detailValue}>{stats.reviews.total}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Đã duyệt:</span>
                        <span style={styles.detailValue}>{stats.reviews.approved}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Chờ duyệt:</span>
                        <span style={styles.detailValue}>{stats.reviews.pending}</span>
                    </div>
                    <div style={styles.detailItem}>
                        <span style={styles.detailLabel}>Đánh giá TB:</span>
                        <span style={styles.detailValue}>⭐ {stats.reviews.averageRating}/5</span>
                    </div>
                </div>
            </div>

            {/* FOOTER */}
            <div style={{ marginTop: '40px', textAlign: 'center', color: '#888', fontSize: '12px' }}>
                <p>Dashboard được cập nhật lần cuối vào: {new Date().toLocaleString('vi-VN')}</p>
            </div>
        </div>
    );
};

export default Dashboard;