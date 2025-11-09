// src/components/ProductList.js
import React, { useState, useEffect, useMemo } from 'react';
import { collection, getDocs, doc, deleteDoc, writeBatch } from 'firebase/firestore'; 
import { db } from '../firebaseConfig';
import { formatCurrency } from '../utils/format';
import ProductManagementModal from './ProductManagementModal';

// Hàm tính toán giá khuyến mãi
const calculateFinalPrice = (basePrice, offer) => {
    if (!offer || !offer.isOffer || !basePrice || basePrice <= 0) return basePrice;
    
    const value = parseFloat(offer.offerValue) || 0;
    const price = parseFloat(basePrice);
    
    let finalPrice = price;

    if (offer.offerType === 'Percentage') {
        finalPrice = price - (price * value / 100);
    } else if (offer.offerType === 'FlatAmount') {
        finalPrice = price - value;
    }
    
    finalPrice = Math.round(finalPrice * 100) / 100;
    
    return Math.max(0, finalPrice);
};

// === STATS STYLES ===
const statsStyles = {
    container: {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '15px',
        marginBottom: '30px',
    },
    card: {
        background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)',
        border: '1px solid #C40000',
        borderRadius: '8px',
        padding: '20px',
        boxShadow: '0 4px 15px rgba(196, 0, 0, 0.2)',
        transition: 'transform 0.2s ease, box-shadow 0.2s ease',
        cursor: 'pointer',
    },
    cardHover: {
        transform: 'translateY(-5px)',
        boxShadow: '0 8px 25px rgba(196, 0, 0, 0.4)',
    },
    value: {
        fontSize: '32px',
        fontWeight: 'bold',
        color: '#C40000',
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
    title: { color: '#E0E0E0', borderBottom: '3px solid #C40000', paddingBottom: '10px', marginBottom: '20px', fontWeight: 300, fontSize: '28px' }, 
    table: { width: '100%', borderCollapse: 'collapse', marginTop: '20px', fontSize: '14px', color: '#E0E0E0' },
    th: { backgroundColor: '#C40000', color: '#FFFFFF', padding: '12px 15px', textAlign: 'left', textTransform: 'uppercase', fontWeight: 600 }, 
    td: { padding: '10px 15px', borderBottom: '1px solid #444', verticalAlign: 'middle' },
    filterBar: { display: 'flex', gap: '15px', marginBottom: '20px', alignItems: 'center', flexWrap: 'wrap' },
    filterInput: { padding: '8px 10px', border: '1px solid #555', borderRadius: '4px', backgroundColor: '#333', color: '#E0E0E0' },
    statusTag: (status) => {
        let color = '#666';
        if (status === 'Active') color = '#28a745';
        if (status === 'Draft') color = '#ffc107';
        if (status === 'Archived') color = '#dc3545';
        return { backgroundColor: color, color: 'white', padding: '4px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold', display: 'inline-block' };
    }, 
    offerTag: { backgroundColor: '#007bff', color: 'white', padding: '4px 8px', borderRadius: '4px', fontSize: '12px', fontWeight: 'bold', display: 'inline-block' }, 
    productImage: { width: '50px', height: '50px', objectFit: 'cover', borderRadius: '2px' },
    actionButton: { border: '1px solid #C40000', background: 'none', color: '#C40000', cursor: 'pointer', padding: '5px 10px', borderRadius: '4px', marginRight: '5px', fontSize: '12px', transition: 'all 0.2s' },
    deleteButton: { backgroundColor: '#C40000', color: '#FFFFFF', border: 'none', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '12px', transition: 'background-color 0.2s' },
    createButton: { backgroundColor: '#007bff', color: 'white', border: 'none', padding: '10px 20px', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', transition: 'background-color 0.2s' }, 
    adminButton: { backgroundColor: '#6c757d', color: 'white', border: 'none', padding: '8px 15px', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', marginRight: '15px' },
    ratingStar: { color: '#ffc107', marginRight: '3px' },
    featuredIcon: { color: '#FFD700', fontSize: '16px', marginLeft: '5px' }, 
    barcodeCode: {
        backgroundColor: '#333',
        padding: '4px 8px',
        borderRadius: '3px',
        color: '#00FF00',
        fontFamily: 'monospace',
        fontSize: '12px',
        letterSpacing: '1px',
    },
    barcodeEmpty: {
        color: '#888',
        fontStyle: 'italic',
    }
};

const ProductList = () => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [stats, setStats] = useState({
        total: 0,
        active: 0,
        draft: 0,
        archived: 0,
        withOffer: 0,
        totalRevenue: 0,
        averagePrice: 0,
    });

    const [managementModalProduct, setManagementModalProduct] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterStatus, setFilterStatus] = useState('All');
    const [filterOffer, setFilterOffer] = useState(false);

    const availableStatuses = ['All', 'Active', 'Draft', 'Archived'];

    const clearAllOffers = async () => {
        const BATCH_SIZE = 400;
        const productsCollectionRef = collection(db, "products");
        let confirm = window.confirm("CẢNH BÁO: Bạn có chắc chắn muốn XÓA TOÀN BỘ thông tin khuyến mãi (Offer) khỏi database không?");
        if (!confirm) return;

        try {
            console.log("Bắt đầu xóa toàn bộ thông tin Offer...");
            const productSnapshot = await getDocs(productsCollectionRef);
            
            let batch = writeBatch(db);
            let count = 0;

            productSnapshot.docs.forEach((productDoc) => {
                const productRef = doc(db, 'products', productDoc.id);
                batch.update(productRef, {
                    isOffer: false,
                    offer: null, 
                    updatedAt: Date.now()
                });
                count++;

                if (count % BATCH_SIZE === 0) {
                    batch.commit();
                    batch = writeBatch(db);
                }
            });

            if (count > 0) {
                await batch.commit();
            }

            console.log(`✅ Hoàn tất xóa thông tin Offer khỏi ${count} sản phẩm.`);
            alert(`Hoàn tất xóa thông tin Offer khỏi ${count} sản phẩm!`);
            fetchProducts(); 

        } catch (err) {
            console.error("LỖI LỚN khi xóa Offer hàng loạt:", err);
            alert(`LỖI: Không thể xóa thông tin Offer.`);
            return 0;
        }
    };

    const fetchProducts = async () => {
        try {
            setLoading(true);
            const productsCollectionRef = collection(db, "products");
            const productSnapshot = await getDocs(productsCollectionRef);

            const productsList = productSnapshot.docs.map(doc => {
                const data = doc.data();
                const totalStock = data.variants ? data.variants.reduce((sum, v) => sum + (v.quantity || 0), 0) : 0;
                
                const offerDetails = { 
                    isOffer: data.isOffer, 
                    offerType: data.offer?.offerType, 
                    offerValue: data.offer?.offerValue 
                };

                const finalPrice = calculateFinalPrice(data.basePrice, offerDetails);

                return {
                    id: doc.id,
                    ...data,
                    totalStock,
                    listPrice: data.basePrice,
                    finalPrice 
                };
            });

            setProducts(productsList);
            
            // Tính stats
            const statsData = {
                total: productsList.length,
                active: productsList.filter(p => p.status === 'Active').length,
                draft: productsList.filter(p => p.status === 'Draft').length,
                archived: productsList.filter(p => p.status === 'Archived').length,
                withOffer: productsList.filter(p => p.isOffer === true).length,
                totalRevenue: productsList.reduce((sum, p) => sum + (p.finalPrice * (p.totalStock || 0)), 0),
                averagePrice: productsList.length > 0 ? (productsList.reduce((sum, p) => sum + p.basePrice, 0) / productsList.length) : 0,
            };
            setStats(statsData);

        } catch (err) {
            console.error("Lỗi khi tải sản phẩm:", err);
            setError("Không thể tải dữ liệu sản phẩm.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchProducts();
    }, []);

    const filteredProducts = useMemo(() => {
        let currentProducts = products;

        if (searchTerm) {
            currentProducts = currentProducts.filter(p =>
                p.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                p.id?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                p.barcode?.includes(searchTerm)
            );
        }

        if (filterStatus !== 'All') {
            currentProducts = currentProducts.filter(p => p.status === filterStatus);
        }

        if (filterOffer) {
            currentProducts = currentProducts.filter(p => p.isOffer === true);
        }

        return currentProducts;
    }, [products, searchTerm, filterStatus, filterOffer]);

    const handleDelete = async (productId) => {
        if (window.confirm(`Bạn có chắc chắn muốn xóa sản phẩm ID: ${productId}?`)) {
            try {
                await deleteDoc(doc(db, 'products', productId));
                setProducts(products.filter(p => p.id !== productId));
                alert("Đã xóa sản phẩm thành công!");
            } catch (err) {
                console.error("Lỗi khi xóa:", err);
                alert("LỖI: Không thể xóa sản phẩm.");
            }
        }
    };

    const handleOpenManagementModal = (product) => {
        setManagementModalProduct(product);
    };

    const handleCreate = () => {
        setManagementModalProduct({
            isOffer: false,
            isFeatured: false,
            basePrice: 0,
            status: 'Draft',
            barcode: '',
            variants: [],
            colorImages: {}
        });
    };

    if (loading) return <div style={{ color: '#E0E0E0', padding: '20px' }}>Đang tải dữ liệu...</div>;
    if (error) return <div style={{ color: '#FF4D4D', padding: '20px' }}>LỖI: {error}</div>;

    return (
        <div style={{ padding: '20px', backgroundColor: '#1A1A1A', minHeight: '100vh', color: '#E0E0E0' }}>
            <h1 style={styles.title}>📦 Quản Lý Sản Phẩm</h1>
            
            {/* STATS CARDS */}
            <div style={statsStyles.container}>
                <div style={{...statsStyles.card, ...statsStyles.cardHover}}>
                    <div style={statsStyles.value}>{stats.total}</div>
                    <div style={statsStyles.label}>📊 Tổng Sản Phẩm</div>
                    <div style={statsStyles.description}>{filteredProducts.length} hiển thị</div>
                </div>
                
                <div style={{...statsStyles.card, ...statsStyles.cardHover}}>
                    <div style={statsStyles.value}>{stats.active}</div>
                    <div style={statsStyles.label}>✅ Hoạt Động</div>
                    <div style={statsStyles.description}>{((stats.active / stats.total) * 100).toFixed(0)}% tổng số</div>
                </div>
                
                <div style={{...statsStyles.card, ...statsStyles.cardHover}}>
                    <div style={statsStyles.value}>{stats.withOffer}</div>
                    <div style={statsStyles.label}>🎁 Đang Khuyến Mãi</div>
                    <div style={statsStyles.description}>{((stats.withOffer / stats.total) * 100).toFixed(0)}% có offer</div>
                </div>
                
                <div style={{...statsStyles.card, ...statsStyles.cardHover}}>
                    <div style={statsStyles.value}>{formatCurrency(stats.averagePrice)}</div>
                    <div style={statsStyles.label}>💰 Giá Trung Bình</div>
                    <div style={statsStyles.description}>Across all products</div>
                </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
                <button 
                    onClick={clearAllOffers} 
                    style={styles.adminButton}
                    title="Xóa toàn bộ thông tin khuyến mãi"
                >
                    ADMIN: CLEAR ALL OFFERS
                </button>
            </div>

            <div style={styles.filterBar}>
                <input
                    type="text"
                    placeholder="Tìm kiếm theo Tên, ID hoặc Barcode..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    style={{...styles.filterInput, flexGrow: 1}}
                />

                <select
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                    style={styles.filterInput}
                >
                    {availableStatuses.map(s => <option key={s} value={s}>{s === 'All' ? 'Tất cả trạng thái' : `Status: ${s}`}</option>)}
                </select>

                <select
                    value={filterOffer ? 'Yes' : 'No'}
                    onChange={(e) => setFilterOffer(e.target.value === 'Yes')}
                    style={styles.filterInput}
                >
                    <option value="No">Tất cả Khuyến mãi</option>
                    <option value="Yes">Chỉ đang Khuyến mãi</option>
                </select>

                <button
                    style={styles.createButton}
                    onClick={handleCreate}
                >
                    + Thêm Sản Phẩm Mới
                </button>
            </div>

            <table style={styles.table}>
                <thead>
                    <tr>
                        <th style={styles.th}>Ảnh</th>
                        <th style={styles.th}>Tên Sản Phẩm</th>
                        <th style={styles.th}>Barcode</th>
                        <th style={styles.th}>Danh mục/Loại</th>
                        <th style={styles.th}>Giá Bán Lẻ</th>
                        <th style={styles.th}>Tồn kho</th>
                        <th style={styles.th}>Trạng thái</th>
                        <th style={styles.th}>KM/Nổi bật</th>
                        <th style={styles.th}>Đánh giá</th>
                        <th style={styles.th}>Hành động</th>
                    </tr>
                </thead>
                <tbody>
                    {filteredProducts.map((product) => (
                        <tr key={product.id}>
                            <td style={styles.td}>
                                <img
                                    src={product.mainImage || 'https://via.placeholder.com/50'}
                                    alt={product.name}
                                    style={styles.productImage}
                                />
                            </td>
                            <td style={styles.td}>
                                <strong>{product.name || 'Sản phẩm không tên'}</strong>
                                <br/><small style={{color: '#888'}}>ID: {product.id}</small>
                            </td>
                            <td style={styles.td}>
                                <code style={styles.barcodeCode}>
                                    {product.barcode ? product.barcode : <span style={styles.barcodeEmpty}>-</span>}
                                </code>
                            </td>
                            <td style={styles.td}>
                                {product.category || 'N/A'} ({product.type || 'N/A'})
                            </td>
                            <td style={styles.td}>
                                <span style={{ textDecoration: product.isOffer ? 'line-through' : 'none', color: product.isOffer ? '#999' : '#E0E0E0' }}>
                                    {formatCurrency(product.basePrice)}
                                </span>
                                {product.isOffer && (
                                    <div style={{ fontWeight: 'bold', color: '#007bff', marginTop: '4px' }}>
                                        {formatCurrency(product.finalPrice)}
                                    </div>
                                )}
                            </td>
                            <td style={{ ...styles.td, ...(product.totalStock < 10 ? { color: '#FF4D4D', fontWeight: 'bold' } : {}) }}>
                                {product.totalStock}
                            </td>
                            <td style={styles.td}>
                                <span style={styles.statusTag(product.status)}>{product.status || 'Draft'}</span>
                            </td>
                            <td style={styles.td}>
                                {product.isOffer && <span style={styles.offerTag}>SALE</span>}
                                {product.isFeatured && <span style={styles.featuredIcon}>★</span>}
                            </td>
                            <td style={styles.td}>
                                <span style={styles.ratingStar}>★</span>
                                {(product.averageRating || 0).toFixed(1)} ({product.totalReviews || 0})
                            </td>
                            <td style={styles.td}>
                                <button
                                    style={styles.actionButton}
                                    onClick={() => handleOpenManagementModal(product)}
                                >
                                    Sửa Chi Tiết
                                </button>

                                <button
                                    style={styles.deleteButton}
                                    onClick={() => handleDelete(product.id)}
                                >
                                    Xóa
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>

            {managementModalProduct && (
                <ProductManagementModal
                    product={managementModalProduct}
                    onClose={() => setManagementModalProduct(null)}
                    onSave={fetchProducts}
                />
            )}
        </div>
    );
};

export default ProductList;