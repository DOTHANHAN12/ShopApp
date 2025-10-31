// src/components/OrderList.js

import React, { useState, useEffect, useMemo } from 'react';
import { collection, getDocs, doc, updateDoc, writeBatch } from 'firebase/firestore';
import { db } from '../firebaseConfig'; 
import { formatCurrency } from '../utils/format'; 
import OrderDetailModal from './OrderDetailModal'; 

// Định nghĩa trạng thái cho bộ lọc
const ORDER_STATUSES = ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'PAID'];
const ORDERS_PER_PAGE = 10; 

// ----------------------------------------------------------------------
// THIẾT KẾ STYLES (DARK/MINIMALIST)
// ----------------------------------------------------------------------
const styles = {
    // container: { padding: '20px', backgroundColor: '#FFFFFF', minHeight: '80vh' }, 
    title: { color: '#E0E0E0', borderBottom: '3px solid #C40000', paddingBottom: '10px', marginBottom: '20px', fontWeight: 300, fontSize: '28px' },
    
    // Tối ưu hóa Table
    table: { width: '100%', borderCollapse: 'collapse', marginTop: '20px', fontSize: '14px', color: '#E0E0E0' },
    th: { backgroundColor: '#C40000', color: '#FFFFFF', padding: '12px 15px', textAlign: 'left', textTransform: 'uppercase', fontWeight: 600 },
    td: { padding: '10px 15px', borderBottom: '1px solid #444', verticalAlign: 'middle' },
    
    // Tối ưu hóa Filter Bar
    filterBar: { display: 'flex', gap: '15px', marginBottom: '20px', alignItems: 'center', flexWrap: 'wrap' },
    filterInput: { padding: '8px 10px', border: '1px solid #555', borderRadius: '4px', backgroundColor: '#333', color: '#E0E0E0' },
    
    // Status Tags (Logic màu giữ nguyên)
    statusTag: (status) => {
        let color = '#666';
        if (status === 'PAID') color = '#8A2BE2'; 
        if (status === 'DELIVERED') color = '#28a745'; 
        if (status === 'PENDING') color = '#ffc107'; 
        if (status === 'PROCESSING') color = '#007bff'; 
        if (status === 'SHIPPED') color = '#007bff';
        if (status === 'CANCELLED' || status === 'FAILED_PAYMENT') color = '#dc3545'; 
        return { backgroundColor: color, color: 'white', padding: '4px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold', display: 'inline-block' };
    },
    
    actionButton: { border: '1px solid #C40000', background: 'none', color: '#C40000', cursor: 'pointer', padding: '5px 10px', borderRadius: '4px', marginRight: '5px', fontSize: '12px', transition: 'all 0.2s' },
    
    pagination: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '20px', color: '#E0E0E0' },
    pageButton: (disabled) => ({ 
        padding: '8px 15px', 
        border: '1px solid #C40000', 
        backgroundColor: disabled ? '#333' : '#C40000', 
        color: disabled ? '#888' : '#fff',
        cursor: disabled ? 'not-allowed' : 'pointer',
        transition: 'background-color 0.2s'
    })
};

const OrderList = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterStatus, setFilterStatus] = useState('All');
    const [detailModalOrder, setDetailModalOrder] = useState(null);

    const [filterMinPrice, setFilterMinPrice] = useState('');
    const [filterMaxPrice, setFilterMaxPrice] = useState('');
    const [filterStartDate, setFilterStartDate] = useState('');
    const [filterEndDate, setFilterEndDate] = useState('');

    const [currentPage, setCurrentPage] = useState(1);


    // ----------------------------------------------------------------------
    // HÀM FETCH DỮ LIỆU CHÍNH (Logic giữ nguyên)
    // ----------------------------------------------------------------------
    const fetchOrders = async () => {
        setLoading(true);
        try {
            const ordersCollectionRef = collection(db, "orders");
            const orderSnapshot = await getDocs(ordersCollectionRef);

            const ordersList = orderSnapshot.docs.map(doc => {
                const data = doc.data();
                const orderDate = data.createdAt ? new Date(Number(data.createdAt)) : null;

                const shippingAddress = data.shippingAddress || {};
                const shippingName = shippingAddress.name || shippingAddress.fullName || 'N/A';
                const shippingCity = shippingAddress.city || shippingAddress.province || 'N/A';
                
                return { 
                    id: doc.id, 
                    ...data, 
                    orderDate, 
                    shippingName,
                    shippingCity
                };
            });

            ordersList.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));

            setOrders(ordersList);

        } catch (err) {
            console.error("Lỗi khi tải đơn hàng:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchOrders();
    }, []);

    // ----------------------------------------------------------------------
    // HÀM TÍNH TOÁN DỮ LIỆU ĐÃ LỌC VÀ PHÂN TRANG (Logic giữ nguyên)
    // ----------------------------------------------------------------------
    const filteredAndPaginatedOrders = useMemo(() => {
        let currentOrders = orders;

        if (searchTerm) {
            const lowerSearchTerm = searchTerm.toLowerCase();
            currentOrders = currentOrders.filter(o =>
                o.orderId?.toLowerCase().includes(lowerSearchTerm) ||
                o.userId?.toLowerCase().includes(lowerSearchTerm)
            );
        }

        if (filterStatus !== 'All') {
            currentOrders = currentOrders.filter(o => o.orderStatus === filterStatus);
        }
        
        const minPrice = parseFloat(filterMinPrice);
        const maxPrice = parseFloat(filterMaxPrice);

        if (!isNaN(minPrice)) {
            currentOrders = currentOrders.filter(o => o.totalAmount >= minPrice);
        }
        if (!isNaN(maxPrice)) {
            currentOrders = currentOrders.filter(o => o.totalAmount <= maxPrice);
        }

        const startTimestamp = filterStartDate ? new Date(filterStartDate).getTime() : 0;
        const endTimestamp = filterEndDate ? new Date(filterEndDate).getTime() : Infinity;

        if (filterStartDate || filterEndDate) {
            currentOrders = currentOrders.filter(o => {
                const orderTimestamp = Number(o.createdAt);
                
                const isAfterStart = startTimestamp === 0 || orderTimestamp >= startTimestamp;
                const isBeforeEnd = endTimestamp === Infinity || orderTimestamp < endTimestamp + (24 * 60 * 60 * 1000); 

                return isAfterStart && isBeforeEnd;
            });
        }
        
        // --- PHÂN TRANG ---
        const totalItems = currentOrders.length;
        const totalPages = Math.ceil(totalItems / ORDERS_PER_PAGE);

        const startIndex = (currentPage - 1) * ORDERS_PER_PAGE;
        const endIndex = startIndex + ORDERS_PER_PAGE;
        
        const paginatedOrders = currentOrders.slice(startIndex, endIndex);

        return {
            paginatedOrders,
            totalItems,
            totalPages
        };
    }, [orders, searchTerm, filterStatus, filterMinPrice, filterMaxPrice, filterStartDate, filterEndDate, currentPage]);

    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm, filterStatus, filterMinPrice, filterMaxPrice, filterStartDate, filterEndDate]);


    // ----------------------------------------------------------------------
    // RENDER
    // ----------------------------------------------------------------------

    if (loading) return <div>Đang tải dữ liệu đơn hàng...</div>;
    
    const { paginatedOrders, totalItems, totalPages } = filteredAndPaginatedOrders;

    return (
        <div style={styles.container}>
            <h1 style={styles.title}>Quản Lý Đơn Hàng ({totalItems} items)</h1>
            
            {/* Thanh Bộ lọc */}
            <div style={styles.filterBar}>
                {/* 1. Tìm kiếm và Trạng thái */}
                <input
                    type="text"
                    placeholder="Tìm kiếm ID Đơn hàng / Khách hàng..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    style={{...styles.filterInput, flexGrow: 1}}
                />
                <select 
                    value={filterStatus} 
                    onChange={(e) => setFilterStatus(e.target.value)} 
                    style={styles.filterInput}
                >
                    <option value="All">Tất cả Trạng thái</option>
                    {ORDER_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
                
                {/* 2. Lọc theo Giá */}
                <input
                    type="number"
                    placeholder="Giá Min"
                    value={filterMinPrice}
                    onChange={(e) => setFilterMinPrice(e.target.value)}
                    style={{...styles.filterInput, width: '100px'}}
                />
                <input
                    type="number"
                    placeholder="Giá Max"
                    value={filterMaxPrice}
                    onChange={(e) => setFilterMaxPrice(e.target.value)}
                    style={{...styles.filterInput, width: '100px'}}
                />

                {/* 3. Lọc theo Ngày */}
                <span style={{color: '#E0E0E0'}}>Từ:</span>
                <input
                    type="date"
                    title="Ngày Bắt đầu"
                    value={filterStartDate}
                    onChange={(e) => setFilterStartDate(e.target.value)}
                    style={styles.filterInput}
                />
                <span style={{color: '#E0E0E0'}}>Đến:</span>
                <input
                    type="date"
                    title="Ngày Kết thúc"
                    value={filterEndDate}
                    onChange={(e) => setFilterEndDate(e.target.value)}
                    style={styles.filterInput}
                />
            </div>

            <table style={styles.table}>
                <thead>
                    <tr>
                        <th style={styles.th}>Mã Đơn hàng</th>
                        <th style={styles.th}>Ngày Đặt</th>
                        <th style={styles.th}>Khách hàng</th>
                        <th style={styles.th}>Tổng Tiền</th>
                        <th style={styles.th}>Địa chỉ Giao</th>
                        <th style={styles.th}>Phương thức TT</th>
                        <th style={styles.th}>Trạng thái</th>
                        <th style={styles.th}>Hành động</th>
                    </tr>
                </thead>
                <tbody>
                    {paginatedOrders.map((order) => (
                        <tr key={order.id}>
                            <td style={styles.td}><small style={{color: '#A0A0A0'}}>{order.orderId || order.id}</small></td>
                            <td style={styles.td}>
                                {order.orderDate ? order.orderDate.toLocaleString() : 'N/A'}
                            </td>
                            <td style={styles.td}>{order.userId || 'Guest'}</td>
                            <td style={styles.td}>
                                **{formatCurrency(order.totalAmount)}**
                            </td>
                            <td style={styles.td}>
                                {order.shippingName} ({order.shippingCity})
                            </td>
                            <td style={styles.td}>{order.paymentMethod || 'N/A'}</td>
                            <td style={styles.td}>
                                <span style={styles.statusTag(order.orderStatus)}>
                                    {order.orderStatus || 'PENDING'}
                                </span>
                            </td>
                            <td style={styles.td}>
                                <button 
                                    style={styles.actionButton} 
                                    onClick={() => setDetailModalOrder(order)}
                                >
                                    Xem Chi tiết
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            
            {/* Phân trang */}
            <div style={styles.pagination}>
                <span>
                    Hiển thị {paginatedOrders.length} trên {totalItems} đơn hàng
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

            {/* Modal chi tiết đơn hàng */}
            {detailModalOrder && (
                <OrderDetailModal 
                    order={detailModalOrder} 
                    onClose={() => setDetailModalOrder(null)} 
                    onSave={fetchOrders}
                />
            )}
        </div>
    );
};

export default OrderList;