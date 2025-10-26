import React, { useState, useEffect, useMemo } from 'react';
import { collection, getDocs, doc, deleteDoc } from 'firebase/firestore';
import { db } from '../firebaseConfig';
import { formatCurrency } from '../utils/format';
import VariantManagementModal from './VariantManagementModal';
import ProductDetailModal from './ProductDetailModal'; // *** ĐÃ SỬA: Đổi từ ProductFormModal sang ProductDetailModal ***

// ----------------------------------------------------------------------
// THIẾT KẾ STYLES (Uniqlo Style: Đen, Trắng, Đỏ tối giản)
// ----------------------------------------------------------------------
const styles = {
    container: { padding: '20px', backgroundColor: '#FFFFFF', minHeight: '80vh' },
    title: { color: '#000000', borderBottom: '3px solid #C40000', paddingBottom: '10px', marginBottom: '20px', fontWeight: 300, fontSize: '28px' },
    table: { width: '100%', borderCollapse: 'collapse', marginTop: '20px', fontSize: '14px' },
    th: { backgroundColor: '#000000', color: '#FFFFFF', padding: '12px 15px', textAlign: 'left', textTransform: 'uppercase', fontWeight: 500 },
    td: { padding: '10px 15px', borderBottom: '1px solid #EEEEEE', verticalAlign: 'middle' },
    productImage: { width: '50px', height: '50px', objectFit: 'cover', borderRadius: '2px' },
    actionButton: { border: '1px solid #000', background: 'none', color: '#000', cursor: 'pointer', padding: '5px 10px', borderRadius: '4px', marginRight: '5px', fontSize: '12px' },
    deleteButton: { backgroundColor: '#C40000', color: '#FFFFFF', border: 'none', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '12px' },
    createButton: { backgroundColor: '#000000', color: 'white', border: 'none', padding: '10px 20px', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' },
    filterBar: { display: 'flex', gap: '15px', marginBottom: '20px', alignItems: 'center', flexWrap: 'wrap' },
    filterInput: { padding: '8px 10px', border: '1px solid #ccc', borderRadius: '4px' },
    statusTag: (status) => {
        let color = '#666';
        if (status === 'Active') color = '#28a745';
        if (status === 'Draft') color = '#ffc107';
        if (status === 'Archived') color = '#dc3545';
        return { backgroundColor: color, color: 'white', padding: '4px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold', display: 'inline-block' };
    },
    offerTag: { backgroundColor: '#C40000', color: 'white', padding: '4px 8px', borderRadius: '4px', fontSize: '12px', fontWeight: 'bold', display: 'inline-block' },
    ratingStar: { color: '#ffc107', marginRight: '3px' },
    featuredIcon: { color: '#C40000', fontSize: '16px', marginLeft: '5px' }
};

const ProductList = () => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // States cho các modal
    const [variantModalProduct, setVariantModalProduct] = useState(null);
    const [detailModalProduct, setDetailModalProduct] = useState(null); // *** ĐÃ SỬA TÊN STATE ***

    // --- STATES CHO BỘ LỌC ---
    const [searchTerm, setSearchTerm] = useState('');
    const [filterStatus, setFilterStatus] = useState('All');
    const [filterOffer, setFilterOffer] = useState(false);

    // Dữ liệu tĩnh cho Dropdown
    const availableStatuses = ['All', 'Active', 'Draft', 'Archived'];
    // const availableCategories = ['All', 'Áo Nam', 'Quần Nam', 'Phụ kiện', 'Áo Nữ', 'Quần Nữ']; // Ví dụ

    // ----------------------------------------------------------------------
    // HÀM FETCH DỮ LIỆU CHÍNH
    // ----------------------------------------------------------------------
    const fetchProducts = async () => {
        try {
            setLoading(true);
            const productsCollectionRef = collection(db, "products");
            const productSnapshot = await getDocs(productsCollectionRef);

            const productsList = productSnapshot.docs.map(doc => {
                const data = doc.data();
                // Tính toán Tổng tồn kho
                const totalStock = data.variants ? data.variants.reduce((sum, v) => sum + (v.quantity || 0), 0) : 0;
                // Giá niêm yết là basePrice
                const listPrice = data.basePrice;

                return {
                    id: doc.id,
                    ...data,
                    totalStock,
                    listPrice
                };
            });

            setProducts(productsList);

        } catch (err) {
            console.error("Lỗi khi tải sản phẩm:", err);
            setError("Không thể tải dữ liệu sản phẩm. Kiểm tra quy tắc Firestore.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchProducts();
    }, []);

    // ----------------------------------------------------------------------
    // HÀM TÍNH TOÁN DỮ LIỆU ĐÃ LỌC
    // ----------------------------------------------------------------------
    const filteredProducts = useMemo(() => {
        let currentProducts = products;

        // 1. Lọc theo Tìm kiếm (Tên & ID)
        if (searchTerm) {
            currentProducts = currentProducts.filter(p =>
                p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                p.id.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        // 2. Lọc theo Trạng thái
        if (filterStatus !== 'All') {
            currentProducts = currentProducts.filter(p => p.status === filterStatus);
        }

        // 3. Lọc theo Khuyến mãi
        if (filterOffer) {
            currentProducts = currentProducts.filter(p => p.isOffer === true);
        }

        return currentProducts;
    }, [products, searchTerm, filterStatus, filterOffer]);


    // ----------------------------------------------------------------------
    // HÀM HÀNH ĐỘNG (CRUD)
    // ----------------------------------------------------------------------
    const handleDelete = async (productId) => {
        if (window.confirm(`Bạn có chắc chắn muốn xóa sản phẩm ID: ${productId}? Thao tác này không thể hoàn tác.`)) {
            try {
                await deleteDoc(doc(db, 'products', productId));
                setProducts(products.filter(p => p.id !== productId));
                alert("Đã xóa sản phẩm thành công!");
            } catch (err) {
                console.error("Lỗi khi xóa:", err);
                alert("LỖI: Không thể xóa sản phẩm. Kiểm tra quyền ghi/xóa.");
            }
        }
    };

    const handleOpenVariantModal = (product) => {
        setVariantModalProduct(product);
    };

    const handleOpenDetailModal = (product) => {
        setDetailModalProduct(product);
    };

    const handleCreate = () => {
        // Mở form với object rỗng cho chức năng Thêm mới, kèm các giá trị mặc định
        setDetailModalProduct({
            isOffer: false,
            isFeatured: false,
            basePrice: 0,
            status: 'Draft',
            variants: [],
            colorImages: {}
        });
    };


    if (loading) return <div style={styles.container}>Đang tải dữ liệu...</div>;
    if (error) return <div style={{ ...styles.container, color: styles.deleteButton.backgroundColor }}>LỖI: {error}</div>;

    return (
        <div style={styles.container}>
            <h1 style={styles.title}>Quản Lý Sản Phẩm</h1>

            <div style={styles.filterBar}>
                {/* Thanh Tìm kiếm */}
                <input
                    type="text"
                    placeholder="Tìm kiếm theo Tên hoặc ID..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    style={{...styles.filterInput, flexGrow: 1}}
                />

                {/* Bộ lọc Trạng thái */}
                <select
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                    style={styles.filterInput}
                >
                    {availableStatuses.map(s => <option key={s} value={s}>{s === 'All' ? 'Tất cả trạng thái' : `Status: ${s}`}</option>)}
                </select>

                {/* Bộ lọc Khuyến mãi */}
                <select
                    value={filterOffer ? 'Yes' : 'No'}
                    onChange={(e) => setFilterOffer(e.target.value === 'Yes')}
                    style={styles.filterInput}
                >
                    <option value="No">Tất cả Khuyến mãi</option>
                    <option value="Yes">Chỉ đang Khuyến mãi</option>
                </select>

                {/* Nút Thêm mới */}
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
                        <th style={styles.th}>Danh mục/Loại</th>
                        <th style={styles.th}>Giá Gốc</th>
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
                                **{product.name || 'Sản phẩm không tên'}**
                                <br/><small style={{color: '#666'}}>ID: {product.id}</small>
                            </td>
                            <td style={styles.td}>
                                {product.category || 'N/A'} ({product.type || 'N/A'})
                            </td>
                            <td style={styles.td}>{formatCurrency(product.basePrice)}</td>
                            <td style={{ ...styles.td, ...(product.totalStock < 10 ? { color: '#C40000', fontWeight: 'bold' } : {}) }}>
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
                                {/* Nút Chỉnh sửa chi tiết */}
                                <button
                                    style={styles.actionButton}
                                    onClick={() => handleOpenDetailModal(product)}
                                >
                                    Sửa chi tiết
                                </button>

                                {/* Nút QUẢN LÝ BIẾN THỂ */}
                                <button
                                    style={{...styles.actionButton, borderColor: '#C40000', color: '#C40000', fontWeight: 'bold'}}
                                    onClick={() => handleOpenVariantModal(product)}
                                >
                                    Variants ({product.variants ? product.variants.length : 0})
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

            {/* Modal Quản lý Biến thể */}
            {variantModalProduct && (
                <VariantManagementModal
                    product={variantModalProduct}
                    onClose={() => setVariantModalProduct(null)}
                    onSave={fetchProducts}
                />
            )}

            {/* Modal Chỉnh sửa chi tiết (Form đa năng) */}
            {detailModalProduct && (
                <ProductDetailModal
                    product={detailModalProduct}
                    onClose={() => setDetailModalProduct(null)}
                    onSave={fetchProducts}
                />
            )}
        </div>
    );
};

export default ProductList;
