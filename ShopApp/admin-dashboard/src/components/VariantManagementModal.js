// src/components/VariantManagementModal.js
import React, { useState, useMemo, useRef, useCallback } from 'react'; 
import { doc, updateDoc } from 'firebase/firestore';
import { db } from '../firebaseConfig';
import { formatCurrency } from '../utils/format';

// --- DARK/MINIMALIST STYLES CHO MODAL ---
const MODAL_STYLES = {
    overlay: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0, 0, 0, 0.9)', zIndex: 1000, display: 'flex', justifyContent: 'center', alignItems: 'center' },
    modalContent: { backgroundColor: '#1A1A1A', padding: '30px', borderRadius: '8px', width: '95%', maxWidth: '1200px', maxHeight: '90vh', overflowY: 'auto', color: '#E0E0E0' },
    
    // Tối ưu hóa Table
    table: { width: '100%', borderCollapse: 'separate', borderSpacing: 0, marginTop: '15px', fontSize: '13px' },
    th: { backgroundColor: '#C40000', color: '#FFFFFF', padding: '12px 15px', textAlign: 'left', textTransform: 'uppercase', fontWeight: 600, border: '1px solid #C40000' },
    td: { padding: '8px 15px', borderBottom: '1px solid #444', verticalAlign: 'middle', borderRight: '1px solid #444', backgroundColor: '#292929' },
    
    // Tối ưu hóa Input
    input: { padding: '5px', border: '1px solid #555', borderRadius: '3px', boxSizing: 'border-box', width: '90%', fontSize: '13px', backgroundColor: '#333', color: '#E0E0E0' },
    select: { padding: '5px', border: '1px solid #555', borderRadius: '3px', boxSizing: 'border-box', width: '100%', fontSize: '13px', backgroundColor: '#333', color: '#E0E0E0' }, 
    
    saveButton: { backgroundColor: '#C40000', color: 'white', padding: '10px 20px', border: 'none', borderRadius: '4px', cursor: 'pointer', marginTop: '20px', marginRight: '10px' },
    variantImage: { width: '40px', height: '40px', objectFit: 'cover', borderRadius: '2px' },
    
    groupRow: { cursor: 'pointer', transition: 'background-color 0.1s' },
    detailTable: { backgroundColor: '#292929', width: '98%', margin: '10px auto', borderCollapse: 'collapse' },
    lowStock: { color: '#FF4D4D', fontWeight: 'bold' },
    actionButton: { background: 'none', border: 'none', cursor: 'pointer', marginLeft: '5px', color: '#FF4D4D' },
    
    statusDropdown: (status) => ({
        padding: '5px 8px', 
        borderRadius: '4px', 
        border: '1px solid', 
        backgroundColor: status === 'Active' ? '#155724' : '#721c24', // Nền đậm
        color: '#FFFFFF', // Chữ trắng
        fontSize: '11px',
        width: '100%'
    }),
    
    autosuggestList: { border: '1px solid #555', maxHeight: '150px', overflowY: 'auto', position: 'absolute', zIndex: 10, backgroundColor: '#333', width: '200px' },
    suggestItem: { padding: '8px', cursor: 'pointer', color: '#C40000' }
};

const FIXED_SIZES = ['S', 'M', 'L', 'XL', 'XXL'];
const PRESET_COLORS = ['White', 'Black', 'Red', 'Blue', 'Green', 'Yellow', 'Grey', 'Pink', 'Brown', 'Purple']; 

// Hàm tạo Variant ID (Logic giữ nguyên)
const generateNewVariantId = (color, size) => {
    const colorCode = color.substring(0, 2).toUpperCase();
    const timestamp = Date.now().toString().substring(8); 
    return `SKU-${colorCode}-${size.toUpperCase()}-${timestamp}`;
};

const VariantManagementModal = ({ product, onClose, onSave }) => {
    const initialVariantsWithKeys = useMemo(() => {
        return (product.variants || []).map((v, index) => ({...v, index}));
    }, [product.variants]);

    const [variants, setVariants] = useState(initialVariantsWithKeys);
    const [saving, setSaving] = useState(false);
    const [openColor, setOpenColor] = useState(null); 
    
    const [newColorInput, setNewColorInput] = useState('');
    const [suggestedColors, setSuggestedColors] = useState([]);
    const newColorInputRef = useRef(null); 

    // ------------------------------------------------------------------
    // HÀM TÍNH TOÁN & NHÓM BIẾN THỂ (Logic giữ nguyên)
    // ------------------------------------------------------------------
    const groupedVariants = useMemo(() => {
        const groups = {};
        
        const existingMap = new Map();
        variants.forEach(v => {
            if (!existingMap.has(v.color)) existingMap.set(v.color, new Map());
            existingMap.get(v.color).set(v.size, v);
        });

        for (const [colorKey, sizeMap] of existingMap.entries()) {
            groups[colorKey] = {
                variants: [],
                totalStock: 0,
                displayImage: product.colorImages?.[colorKey]?.[0] || product.mainImage || 'https://via.placeholder.com/40'
            };

            FIXED_SIZES.forEach((fixedSize, index) => {
                const existingVariant = sizeMap.get(fixedSize);
                
                if (existingVariant) {
                    groups[colorKey].variants.push({ 
                        ...existingVariant, 
                        index, 
                        status: existingVariant.status || 'Inactive', 
                        isActive: (existingVariant.status || 'Inactive') === 'Active' 
                    });
                    groups[colorKey].totalStock += existingVariant.quantity || 0;
                } else {
                    groups[colorKey].variants.push({
                        variantId: generateNewVariantId(colorKey, fixedSize), 
                        color: colorKey,
                        size: fixedSize,
                        price: product.basePrice || 0,
                        quantity: 0,
                        status: 'Inactive',
                        isActive: false, 
                        index
                    });
                }
            });
        }
        return groups;
    }, [variants, product.colorImages, product.mainImage, product.basePrice]);


    // ------------------------------------------------------------------
    // LOGIC CHỈNH SỬA VÀ LƯU (Logic giữ nguyên)
    // ------------------------------------------------------------------

    const handleVariantChange = (uniqueKey, field, value) => {
        setVariants(prevVariants => prevVariants.map(v => {
            const key = v.variantId || `new_temp_${v.color}_${v.size}`; 
            
            if (key === uniqueKey) {
                const processedValue = (field === 'price' || field === 'quantity') ? (value === '' ? 0 : parseFloat(value)) : value;
                
                let updatedStatus = v.status;
                if (v.status === 'Inactive' && field !== 'status' && processedValue !== 0) {
                    updatedStatus = 'Active';
                }

                return { 
                    ...v, 
                    [field]: processedValue, 
                    status: updatedStatus,
                    isActive: updatedStatus === 'Active'
                }; 
            }
            return v;
        }));
    };

    const handleStatusChange = (uniqueKey, newStatus) => {
        setVariants(prevVariants => prevVariants.map(v => {
            const key = v.variantId || `new_temp_${v.color}_${v.size}`;
            if (key === uniqueKey) {
                
                const isActive = newStatus === 'Active';
                
                const currentPrice = v.price || product.basePrice || 0; 
                const currentQuantity = v.quantity || 1; 

                return { 
                    ...v, 
                    status: newStatus, 
                    isActive: isActive,
                    
                    price: isActive ? currentPrice : 0, 
                    quantity: isActive ? currentQuantity : 0 
                };
            }
            return v;
        }));
    };

    const handleDeleteColorGroupPermanently = (colorKey) => {
        if (!window.confirm(`CẢNH BÁO: Bạn có chắc chắn muốn XÓA VĨNH VIỄN toàn bộ biến thể của màu "${colorKey}" không? Thao tác này sẽ xóa chúng khỏi database khi bạn lưu!`)) {
            return;
        }

        setVariants(prevVariants => prevVariants.filter(v => v.color !== colorKey));

        if (openColor === colorKey) setOpenColor(null); 
        alert(`Tất cả biến thể của màu "${colorKey}" đã bị xóa khỏi bộ nhớ và sẽ bị xóa khỏi database sau khi lưu.`);
    };

    const addNewColorToProduct = useCallback((colorName) => {
        if (!colorName || colorName.trim() === '') {
            alert("Vui lòng nhập tên màu.");
            return;
        }
        if (Object.keys(groupedVariants).includes(colorName)) {
            alert(`Màu "${colorName}" đã tồn tại.`);
            return;
        }

        const newVariantsForColor = FIXED_SIZES.map((size, index) => ({
            variantId: generateNewVariantId(colorName, size), 
            color: colorName,
            size: size,
            price: product.basePrice || 0,
            quantity: 0,
            status: 'Inactive',
            isActive: false, 
            index: Date.now() + index,
        }));

        setVariants(prev => [...prev, ...newVariantsForColor]);
        setNewColorInput('');
        setOpenColor(colorName); 
    }, [groupedVariants, product.basePrice]);

    const handleNewColorInputChange = (e) => {
        const value = e.target.value;
        setNewColorInput(value);

        if (value.length > 2) {
            const suggestions = PRESET_COLORS.filter(color => 
                color.toLowerCase().startsWith(value.toLowerCase()) && 
                !Object.keys(groupedVariants).includes(color) 
            );
            setSuggestedColors(suggestions);
        } else {
            setSuggestedColors([]);
        }
    };

    const selectSuggestedColor = (color) => {
        setNewColorInput(color);
        setSuggestedColors([]);
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            const docRef = doc(db, 'products', product.id);
            
            const finalVariants = variants
                .map(v => {
                    const cleaned = { ...v };
                    if (typeof cleaned.variantId === 'string' && cleaned.variantId.startsWith('new_temp_')) {
                        delete cleaned.variantId; 
                    }
                    delete cleaned.isActive;
                    delete cleaned.index;
                    return cleaned;
                });

            await updateDoc(docRef, {
                variants: finalVariants
            });

            onSave(); 
            onClose();
            alert("Đã lưu Thay đổi trạng thái biến thể thành công! (Bao gồm cả Inactive).");
        } catch (error) {
            console.error("Lỗi khi lưu variants:", error);
            alert("LỖI LƯU: Không thể cập nhật biến thể.");
        } finally {
            setSaving(false);
        }
    };
    
    // ------------------------------------------------------------------
    // HÀM RENDER CHI TIẾT BIẾN THỂ CON 
    // ------------------------------------------------------------------
    const renderVariantDetails = (colorKey, variantsList) => (
        <tr key={`${colorKey}-details`}>
            <td colSpan={5} style={{ padding: 0, border: 'none' }}>
                <table style={MODAL_STYLES.detailTable}>
                    <thead>
                        <tr>
                            <th style={{...MODAL_STYLES.th, width: '10%'}}>Size</th>
                            <th style={{...MODAL_STYLES.th, width: '20%'}}>Mã SKU (VariantId)</th>
                            <th style={{...MODAL_STYLES.th, width: '20%'}}>Giá Bán Lẻ</th>
                            <th style={{...MODAL_STYLES.th, width: '20%'}}>Tồn kho</th>
                            <th style={{...MODAL_STYLES.th, width: '20%'}}>Trạng thái</th>
                        </tr>
                    </thead>
                    <tbody>
                        {variantsList.map(v => {
                            const uniqueKey = v.variantId || `new_temp_${v.color}_${v.size}`;
                            const isLowStock = v.quantity < 5 && v.quantity > 0 && v.isActive;
                            const isOutOfStock = v.quantity === 0 && v.isActive;
                            const isDisabled = !v.isActive;

                            return (
                                <tr key={uniqueKey} style={isDisabled ? { opacity: 0.5, backgroundColor: '#333' } : {}}>
                                    
                                    {/* Size (Cố định) */}
                                    <td style={{...MODAL_STYLES.td, borderLeft: 'none'}}>
                                        <span style={{ fontWeight: 'bold', color: isDisabled ? '#aaa' : '#E0E0E0' }}>{v.size}</span>
                                    </td>
                                    
                                    {/* Mã SKU (VariantId) - Inline Editing */}
                                    <td style={MODAL_STYLES.td}>
                                        <input 
                                            type="text" 
                                            value={v.variantId || ''} 
                                            onChange={(e) => handleVariantChange(uniqueKey, 'variantId', e.target.value)} 
                                            style={MODAL_STYLES.input} 
                                            disabled={isDisabled}
                                            title={isDisabled ? "Kích hoạt để chỉnh sửa" : "Sử dụng VariantId làm SKU"}
                                        />
                                    </td>

                                    {/* Giá Bán Lẻ - Inline Editing */}
                                    <td style={MODAL_STYLES.td}>
                                        <input 
                                            type="number" 
                                            value={v.price || 0} 
                                            onChange={(e) => handleVariantChange(uniqueKey, 'price', e.target.value)} 
                                            style={MODAL_STYLES.input} 
                                            min="0"
                                            disabled={isDisabled}
                                        />
                                        <small style={{display: 'block', color: '#888', marginTop: '4px'}}>{formatCurrency(v.price)}</small>
                                    </td>

                                    {/* Tồn kho - Inline Editing & Cảnh báo */}
                                    <td style={{...MODAL_STYLES.td, ...(isLowStock ? MODAL_STYLES.lowStock : isOutOfStock ? { backgroundColor: '#721c24' } : {})}}>
                                        <input 
                                            type="number" 
                                            value={v.quantity || 0} 
                                            onChange={(e) => handleVariantChange(uniqueKey, 'quantity', e.target.value)} 
                                            style={MODAL_STYLES.input} 
                                            min="0"
                                            disabled={isDisabled}
                                        />
                                        {(isOutOfStock || isLowStock) && <span style={{ marginLeft: '5px' }}>{isOutOfStock ? '🚫' : '⚠️'}</span>}
                                    </td>
                                    
                                    {/* Trạng thái (Dropdown Status) */}
                                    <td style={{...MODAL_STYLES.td, borderRight: 'none'}}>
                                        <select
                                            value={v.status || 'Inactive'}
                                            onChange={(e) => handleStatusChange(uniqueKey, e.target.value)}
                                            style={MODAL_STYLES.statusDropdown(v.status)}
                                        >
                                            <option value="Active">Active</option>
                                            <option value="Inactive">Inactive</option>
                                        </select>
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </td>
        </tr>
    );


    return (
        <div style={MODAL_STYLES.overlay}>
            <div style={MODAL_STYLES.modalContent}>
                <h2 style={{ borderBottom: '1px solid #555', paddingBottom: '10px', marginBottom: '10px', color: '#C40000' }}>
                    Quản Lý Biến Thể: {product.name}
                </h2>

                {/* --- KHU VỰC THÊM MÀU MỚI VÀ GỢI Ý --- */}
                <div style={{ marginBottom: '20px', display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
                    <div style={{ position: 'relative', flexGrow: 1, maxWidth: '300px' }}>
                        <input 
                            type="text" 
                            placeholder="Nhập/Gợi ý tên Màu mới (Whi -> White)" 
                            value={newColorInput}
                            onChange={handleNewColorInputChange}
                            ref={newColorInputRef}
                            style={{...MODAL_STYLES.input, width: '100%', border: '2px solid #C40000'}}
                        />
                        {suggestedColors.length > 0 && (
                            <div style={{...MODAL_STYLES.autosuggestList, top: '40px'}}>
                                {suggestedColors.map(color => (
                                    <div 
                                        key={color} 
                                        onClick={() => selectSuggestedColor(color)}
                                        style={{...MODAL_STYLES.suggestItem, ':hover': { backgroundColor: '#444' }}}
                                    >
                                        {color}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                    <button 
                        onClick={() => addNewColorToProduct(newColorInput)} 
                        style={{...MODAL_STYLES.saveButton, backgroundColor: '#007bff', margin: 0, padding: '10px 15px'}}
                    >
                        + Thêm Màu
                    </button>
                    <button 
                        onClick={() => alert("Chức năng Sao chép đang phát triển")} 
                        style={{...MODAL_STYLES.saveButton, backgroundColor: '#4a4a4a', margin: 0, padding: '10px 15px'}}
                    >
                        Sao Chép Giá
                    </button>
                </div>
                {/* -------------------------------------- */}


                <p style={{marginBottom: '15px', color: '#888'}}>
                    Click vào dòng Màu sắc để mở/đóng chi tiết các Size.
                </p>

                <table style={MODAL_STYLES.table}>
                    <thead>
                        <tr>
                            <th style={{...MODAL_STYLES.th, width: '5%', borderLeft: 'none'}}>Ảnh</th>
                            <th style={{...MODAL_STYLES.th, width: '20%'}}>Màu sắc</th>
                            <th style={{...MODAL_STYLES.th, width: '15%'}}>Số biến thể</th>
                            <th style={{...MODAL_STYLES.th, width: '15%'}}>Tổng Tồn kho</th>
                            <th style={{...MODAL_STYLES.th, width: '10%', borderRight: 'none'}}>Chi tiết</th>
                        </tr>
                    </thead>
                    <tbody>
                        {Object.entries(groupedVariants).map(([colorKey, group]) => {
                            const isOpen = openColor === colorKey;
                            const totalStockStyle = group.totalStock < 5 ? MODAL_STYLES.lowStock : {};

                            return (
                                <React.Fragment key={colorKey}>
                                    {/* DÒNG TỔNG HỢP MÀU (CÓ THỂ CLICK) */}
                                    <tr 
                                        style={{...MODAL_STYLES.groupRow, backgroundColor: isOpen ? '#333' : '#1A1A1A', border: isOpen ? '1px solid #C40000' : 'none'}}
                                        onClick={() => setOpenColor(isOpen ? null : colorKey)}
                                    >
                                        <td style={{...MODAL_STYLES.td, borderLeft: 'none'}}>
                                            <img 
                                                src={group.displayImage} 
                                                alt={colorKey} 
                                                style={MODAL_STYLES.variantImage}
                                            />
                                        </td>
                                        <td style={{...MODAL_STYLES.td, display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                                            <span style={{ fontWeight: 'bold' }}>{colorKey}</span>
                                            
                                            {/* Nút XÓA MÀU (ở dòng tổng hợp) */}
                                            <button 
                                                onClick={(e) => {
                                                    e.stopPropagation(); 
                                                    handleDeleteColorGroupPermanently(colorKey);
                                                }}
                                                style={MODAL_STYLES.actionButton}
                                                title={`Xóa VĨNH VIỄN toàn bộ biến thể của màu ${colorKey}`}
                                            >
                                                🗑️
                                            </button>
                                        </td>
                                        <td style={MODAL_STYLES.td}>{group.variants.filter(v => v.isActive).length} / {FIXED_SIZES.length}</td>
                                        <td style={{...MODAL_STYLES.td, ...totalStockStyle}}>{group.totalStock}</td>
                                        <td style={{...MODAL_STYLES.td, borderRight: 'none'}}>
                                            {isOpen ? '🔽 Đóng' : '▶️ Mở chi tiết'}
                                        </td>
                                    </tr>

                                    {/* DÒNG CHI TIẾT (DROPDOWN) */}
                                    {isOpen && renderVariantDetails(colorKey, group.variants)}
                                </React.Fragment>
                            );
                        })}
                    </tbody>
                </table>
                
                <div>
                    <button style={MODAL_STYLES.saveButton} onClick={handleSave} disabled={saving}>
                        {saving ? 'Đang lưu...' : 'Lưu Thay Đổi Biến Thể'}
                    </button>
                    <button style={{ ...MODAL_STYLES.saveButton, backgroundColor: '#666' }} onClick={onClose} disabled={saving}>
                        Đóng
                    </button>
                </div>
            </div>
        </div>
    );
};

export default VariantManagementModal;