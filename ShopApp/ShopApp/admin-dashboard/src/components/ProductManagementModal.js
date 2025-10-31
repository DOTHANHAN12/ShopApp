// src/components/ProductManagementModal.js
import React, { useState, useRef, useMemo, useCallback } from 'react'; 
import { doc, updateDoc, collection, setDoc } from 'firebase/firestore'; 
import { db } from '../firebaseConfig';
import { uploadFile, deleteFile } from '../firebaseConfig'; 
import { formatCurrency } from '../utils/format'; // Giả định tồn tại hàm formatCurrency

// --- CONFIG CỐ ĐỊNH TỪ VARIANT MODAL ---
const FIXED_SIZES = ['S', 'M', 'L', 'XL', 'XXL'];
const PRESET_COLORS = ['White', 'Black', 'Red', 'Blue', 'Green', 'Yellow', 'Grey', 'Pink', 'Brown', 'Purple']; 
const statusOptions = ['Active', 'Draft', 'Archived'];
const offerTypeOptions = ['Percentage', 'FlatAmount'];

// --- HÀM TIỆN ÍCH ID VÀ THỜI GIAN ---

// Hàm tạo ID theo format cũ: ten_san_pham_ngau_nhien
const createSlugId = (name, firestoreId) => {
    const slug = name
        .toLowerCase()
        .normalize("NFD") 
        .replace(/[\u0300-\u036f]/g, "") 
        .replace(/\s/g, '_') 
        .replace(/[^a-z0-9_&,-]/g, '')
        .substring(0, 40); 
    const uniqueSuffix = firestoreId.substring(firestoreId.length - 8);
    return `${slug}_${uniqueSuffix}`;
};

const formatTimestampToDateInput = (timestamp) => {
    if (!timestamp || typeof timestamp !== 'number') return '';
    const date = new Date(timestamp); 
    return date.toISOString().split('T')[0];
};

const dateInputToTimestamp = (dateString) => {
    if (!dateString) return null;
    return new Date(dateString).getTime();
};

const generateNewVariantId = (color, size) => {
    const colorCode = color.substring(0, 2).toUpperCase();
    const timestamp = Date.now().toString().substring(8); 
    return `SKU-${colorCode}-${size.toUpperCase()}-${timestamp}`;
};


// --- STYLES GỘP CHUNG (DARK/MINIMALIST) ---
const styles = {
    overlay: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0, 0, 0, 0.9)', zIndex: 1000, display: 'flex', justifyContent: 'center', alignItems: 'center' },
    modalContent: { backgroundColor: '#1A1A1A', padding: '30px', borderRadius: '8px', width: '90%', maxWidth: '1200px', maxHeight: '90vh', overflowY: 'auto', color: '#E0E0E0' },
    
    // Form & Input Styles
    sectionHeader: { color: '#C40000', borderBottom: '1px solid #C40000', paddingBottom: '10px', marginBottom: '20px', fontSize: '20px', fontWeight: 'bold', marginTop: '30px' },
    subHeader: { color: '#E0E0E0', fontSize: '16px', fontWeight: 'bold', marginBottom: '15px' },
    formGroup: { marginBottom: '15px' },
    label: { display: 'block', fontWeight: 'bold', marginBottom: '5px' },
    input: { width: '100%', padding: '10px', border: '1px solid #444', borderRadius: '4px', boxSizing: 'border-box', backgroundColor: '#333', color: '#E0E0E0' },
    
    // Image Upload Styles
    uploadBox: { border: '2px dashed #C40000', padding: '20px', textAlign: 'center', cursor: 'pointer', backgroundColor: '#292929', transition: 'background-color 0.2s' },
    imagePreview: { width: '80px', height: '80px', objectFit: 'cover', borderRadius: '4px', marginRight: '10px' },
    imageContainer: { position: 'relative', display: 'inline-block', margin: '5px' },
    removeImageButton: { position: 'absolute', top: '-8px', right: '-8px', background: '#FF4D4D', color: 'white', border: 'none', borderRadius: '50%', width: '20px', height: '20px', fontSize: '12px', cursor: 'pointer' },
    
    // Variant Table Styles
    table: { width: '100%', borderCollapse: 'collapse', marginTop: '15px', fontSize: '13px', color: '#E0E0E0' },
    th: { backgroundColor: '#C40000', color: '#FFFFFF', padding: '10px 15px', textAlign: 'left', textTransform: 'uppercase', fontWeight: 600, border: '1px solid #C40000' },
    td: { padding: '8px 15px', borderBottom: '1px solid #444', verticalAlign: 'middle', borderRight: '1px solid #444', backgroundColor: '#292929' },
    groupRow: (isOpen) => ({ cursor: 'pointer', backgroundColor: isOpen ? '#3a1a1a' : 'transparent', transition: 'background-color 0.2s', border: isOpen ? '1px solid #C40000' : '1px solid #292929' }),
    detailTable: { backgroundColor: '#292929', width: '98%', margin: '10px auto', borderCollapse: 'collapse' },
    lowStock: { color: '#FF4D4D', fontWeight: 'bold' },
    statusDropdown: (status) => ({
        padding: '5px 8px', 
        borderRadius: '4px', 
        border: '1px solid', 
        backgroundColor: status === 'Active' ? '#155724' : '#721c24', 
        color: '#FFFFFF', 
        fontSize: '11px',
        width: '100%'
    }),
    
    // Button Styles
    saveButton: { backgroundColor: '#C40000', color: 'white', padding: '10px 20px', border: 'none', borderRadius: '4px', cursor: 'pointer', marginTop: '20px', marginRight: '10px' },
    autosuggestList: { border: '1px solid #555', maxHeight: '150px', overflowY: 'auto', position: 'absolute', zIndex: 10, backgroundColor: '#333', width: '200px' },
    suggestItem: { padding: '8px', cursor: 'pointer', color: '#C40000' }
};


const ProductManagementModal = ({ product: initialProduct, onClose, onSave }) => {
    const [product, setProduct] = useState(initialProduct);
    const [saving, setSaving] = useState(false);
    const isEditing = !!initialProduct.id;
    const [uploading, setUploading] = useState({}); 

    const fileInputRefs = useRef({}); 
    
    // Variant States
    const initialVariantsWithKeys = useMemo(() => {
        return (product.variants || []).map((v, index) => ({...v, index}));
    }, [product.variants]);

    const [variants, setVariants] = useState(initialVariantsWithKeys);
    const [openColor, setOpenColor] = useState(null); 
    const [newColorInput, setNewColorInput] = useState('');
    const [suggestedColors, setSuggestedColors] = useState([]);


    const isAnyUploading = Object.values(uploading).some(Boolean);
    const isFirebaseStorageUrl = (url) => typeof url === 'string' && url.includes('firebasestorage.googleapis.com');
    
    // --- LOGIC CHUNG (Giữ nguyên) ---
    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setProduct(prev => ({ 
            ...prev, 
            [name]: type === 'checkbox' ? checked : (name === 'basePrice' ? parseFloat(value) : value) 
        }));
    };

    const handleOfferChange = (e) => {
        const { name, value, type } = e.target;
        let processedValue = value;
        if (type === 'date') { processedValue = dateInputToTimestamp(value); } 
        else if (type === 'number') { processedValue = parseFloat(value); }
        setProduct(prev => ({ ...prev, offer: { ...prev.offer, [name]: processedValue } }));
    };
    
    // --- LOGIC IMAGE UPLOAD (Giữ nguyên) ---
    const handleFileUpload = async (e, fieldName, colorKey = null, oldImageUrl = null) => {
        // ... (Logic upload giữ nguyên)
        const file = e.target.files[0];
        if (!file || !product.id) return;
        
        const uploadKey = colorKey ? `${fieldName}_${colorKey}_${file.name}` : `${fieldName}_${file.name}`;
        setUploading(prev => ({ ...prev, [uploadKey]: true }));
        
        try {
            if (oldImageUrl && isFirebaseStorageUrl(oldImageUrl)) await deleteFile(oldImageUrl); 
            const path = `products/${product.id}/${fieldName}${colorKey ? '/' + colorKey : ''}`;
            const url = await uploadFile(file, path); 

            setProduct(prev => {
                if (fieldName === 'mainImage') return { ...prev, mainImage: url };
                if (colorKey) {
                    const newColorImages = { ...prev.colorImages } || {};
                    const currentImages = newColorImages[colorKey] || [];
                    if (oldImageUrl && currentImages.includes(oldImageUrl)) {
                        const index = currentImages.indexOf(oldImageUrl);
                        if (index !== -1) currentImages[index] = url; 
                    } else {
                        if (!currentImages.includes(url)) newColorImages[colorKey] = [...currentImages, url];
                    }
                    return { ...prev, colorImages: newColorImages };
                }
                return prev;
            });
            alert(`Tải lên thành công: ${file.name}`);

        } catch (error) {
            console.error("[!!! LỖI UPLOAD CẤP ĐỘ CAO !!!]", error);
            alert(`LỖI TẢI ẢNH: ${error.code ? error.code : "Lỗi mạng không rõ."}`);
        } finally {
            setUploading(prev => ({ ...prev, [uploadKey]: false }));
            e.target.value = null; 
        }
    };

    const handleRemoveImage = async (imageUrl, color = null, urlIndex = -1) => {
        if (!imageUrl || !window.confirm("Bạn có chắc chắn muốn xóa ảnh này?")) return;
        const uploadIdKey = `delete_${imageUrl.substring(imageUrl.length - 20)}`;
        setUploading(prev => ({ ...prev, [uploadIdKey]: true }));
        if (isFirebaseStorageUrl(imageUrl)) await deleteFile(imageUrl); 
        setUploading(prev => ({ ...prev, [uploadIdKey]: false }));
        
        if (color && urlIndex !== -1) { 
            const newColorImages = { ...product.colorImages };
            newColorImages[color].splice(urlIndex, 1);
            setProduct(prev => ({ ...prev, colorImages: newColorImages }));
        } else { 
            setProduct(prev => ({ ...prev, mainImage: '' }));
        }
        alert("Đã xóa ảnh thành công!");
    };
    
    const triggerUpload = (refName, oldImageUrl = null) => {
        if (!product.id || isAnyUploading) return;
        const fileInput = fileInputRefs.current[refName];
        if (oldImageUrl) { fileInput.dataset.oldurl = oldImageUrl; } else { delete fileInput.dataset.oldurl; }
        fileInput.click();
    };

    // --- LOGIC VARIANT MANAGEMENT (Đã tích hợp) ---
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
                        status: existingVariant.status || 'Inactive', 
                        isActive: (existingVariant.status || 'Inactive') === 'Active' 
                    });
                    groups[colorKey].totalStock += existingVariant.quantity || 0;
                } else {
                    groups[colorKey].variants.push({
                        variantId: generateNewVariantId(colorKey, fixedSize), 
                        color: colorKey, size: fixedSize, price: product.basePrice || 0,
                        quantity: 0, status: 'Inactive', isActive: false, 
                    });
                }
            });
        }
        return groups;
    }, [variants, product.colorImages, product.mainImage, product.basePrice]);
    
    const handleVariantChange = (uniqueKey, field, value) => {
        setVariants(prevVariants => prevVariants.map(v => {
            const key = v.variantId || `new_temp_${v.color}_${v.size}`; 
            if (key === uniqueKey) {
                const processedValue = (field === 'price' || field === 'quantity') ? (value === '' ? 0 : parseFloat(value)) : value;
                let updatedStatus = v.status;
                if (v.status === 'Inactive' && field !== 'status' && processedValue !== 0 && field !== 'variantId') {
                    updatedStatus = 'Active';
                }
                return { ...v, [field]: processedValue, status: updatedStatus, isActive: updatedStatus === 'Active' }; 
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
                    ...v, status: newStatus, isActive: isActive,
                    price: isActive ? currentPrice : 0, 
                    quantity: isActive ? currentQuantity : 0 
                };
            }
            return v;
        }));
    };
    
    const handleDeleteColorGroupPermanently = (colorKey) => {
        if (!window.confirm(`CẢNH BÁO: Xóa vĩnh viễn toàn bộ biến thể của màu "${colorKey}"?`)) return;
        setVariants(prevVariants => prevVariants.filter(v => v.color !== colorKey));
        if (openColor === colorKey) setOpenColor(null); 
        alert(`Biến thể của màu "${colorKey}" đã được đánh dấu xóa khi lưu.`);
    };

    const addNewColorToProduct = (colorName) => {
        if (!colorName || colorName.trim() === '' || Object.keys(groupedVariants).includes(colorName)) {
            alert(colorName ? `Màu "${colorName}" đã tồn tại.` : "Vui lòng nhập tên màu.");
            return;
        }
        const newVariantsForColor = FIXED_SIZES.map((size, index) => ({
            variantId: generateNewVariantId(colorName, size), 
            color: colorName, size: size, price: product.basePrice || 0,
            quantity: 0, status: 'Inactive', isActive: false, index: Date.now() + index,
        }));
        setVariants(prev => [...prev, ...newVariantsForColor]);
        setNewColorInput('');
        setSuggestedColors([]);
        setOpenColor(colorName); 
    };

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

    // --- LOGIC LƯU (Gộp) ---
    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);
        if (isAnyUploading) { alert("Vui lòng đợi tải ảnh hoàn tất."); setSaving(false); return; }
        if (!product.name) { alert("Vui lòng nhập Tên Sản Phẩm."); setSaving(false); return; }

        const offerDataToSave = product.isOffer ? { ...product.offer, startDate: product.offer.startDate || null, endDate: product.offer.endDate || null, } : null;
        
        // Chuẩn bị variants để lưu: Lọc bỏ cờ UI và các biến thể Inactive/0 quantity (tùy theo logic backend)
        const finalVariants = variants.map(v => {
            const cleaned = { ...v };
            if (typeof cleaned.variantId === 'string' && cleaned.variantId.startsWith('new_temp_')) { delete cleaned.variantId; }
            delete cleaned.isActive;
            delete cleaned.index;
            return cleaned;
        });

        let currentProductId = product.id;

        try {
            if (isEditing) {
                // UPDATE
                const docRef = doc(db, 'products', product.id);
                await updateDoc(docRef, {
                    ...product, basePrice: parseFloat(product.basePrice || 0), isOffer: product.isOffer || false,
                    isFeatured: product.isFeatured || false, offer: offerDataToSave, updatedAt: Date.now(),
                    variants: finalVariants // LƯU CẢ VARIANT VÀO ĐÂY
                });
                alert("Cập nhật thành công!");
            } else {
                // TẠO MỚI (Sử dụng setDoc với Custom ID)
                const productsCollectionRef = collection(db, 'products');
                const newDocRef = doc(productsCollectionRef); 
                const finalCustomId = createSlugId(product.name, newDocRef.id);
                const finalDocRef = doc(db, 'products', finalCustomId);
                currentProductId = finalCustomId;

                await setDoc(finalDocRef, {
                    ...product,
                    productId: finalCustomId, 
                    basePrice: parseFloat(product.basePrice || 0), status: product.status || 'Draft',
                    isOffer: product.isOffer || false, isFeatured: product.isFeatured || false,
                    offer: offerDataToSave, createdAt: Date.now(), updatedAt: Date.now(),
                    variants: finalVariants
                }); 
                setProduct(prev => ({ ...prev, id: finalCustomId, productId: finalCustomId, createdAt: Date.now(), updatedAt: Date.now() })); 
                alert(`Thêm sản phẩm mới thành công! ID: ${finalCustomId}. Bạn có thể tải ảnh ngay bây giờ.`);
            }

            onSave();
            if (isEditing) onClose(); 

        } catch (error) {
            console.error("Lỗi khi lưu sản phẩm:", error);
            alert("LỖI LƯU DỮ LIỆU: Vui lòng kiểm tra console.");
        } finally {
            setSaving(false);
        }
    };
    
    // --- RENDER CÁC PHẦN (Sections) ---
    const renderImageSection = () => {
        let colorKeys = new Set(Object.keys(product.colorImages || {}));
        (product.variants || []).forEach(v => { if (v.color) colorKeys.add(v.color); });
        colorKeys = Array.from(colorKeys);

        return (
            <section>
                <h3 style={{...styles.sectionHeader, marginTop: '0px'}}>1. Thông tin Cơ bản & Media</h3>
                
                {/* Core Details (Row 1) */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                    <div style={styles.formGroup}>
                        <label style={styles.label}>Tên Sản Phẩm:</label>
                        <input type="text" name="name" value={product.name || ''} onChange={handleChange} style={styles.input} required />
                    </div>
                    <div style={styles.formGroup}>
                        <label style={styles.label}>Giá Gốc (Base Price):</label>
                        <input type="number" name="basePrice" value={product.basePrice || 0} onChange={handleChange} style={styles.input} required min="0"/>
                    </div>
                </div>
                
                {/* Core Details (Row 2) */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '20px' }}>
                    <div style={styles.formGroup}>
                        <label style={styles.label}>Danh Mục:</label>
                        <input type="text" name="category" value={product.category || ''} onChange={handleChange} style={styles.input} />
                    </div>
                    <div style={styles.formGroup}>
                        <label style={styles.label}>Loại Sản Phẩm:</label>
                        <input type="text" name="type" value={product.type || ''} onChange={handleChange} style={styles.input} />
                    </div>
                    <div style={styles.formGroup}>
                        <label style={styles.label}>Trạng thái:</label>
                        <select name="status" value={product.status || 'Draft'} onChange={handleChange} style={styles.input}>
                            {statusOptions.map(s => <option key={s} value={s}>{s}</option>)}
                        </select>
                    </div>
                </div>
                
                <div style={styles.formGroup}>
                    <label style={styles.label}>Mô Tả:</label>
                    <textarea name="desc" value={product.desc || ''} onChange={handleChange} style={{ ...styles.input, height: '100px' }} />
                </div>

                {/* Main Image Upload */}
                <h4 style={styles.subHeader}>Ảnh Chính (Main Image)</h4>
                <div style={{...styles.uploadBox, backgroundColor: isAnyUploading ? '#444' : '#292929'}} onClick={() => triggerUpload('mainImageUpload', product.mainImage || null)}>
                    <input type="file" accept="image/*" onChange={(e) => handleFileUpload(e, 'mainImage', null, e.target.dataset.oldurl)} style={{ display: 'none' }} ref={el => fileInputRefs.current['mainImageUpload'] = el} disabled={!product.id || isAnyUploading} />
                    {product.mainImage && (
                        <div style={styles.imageContainer}>
                            <img src={product.mainImage} alt="Main Preview" style={styles.imagePreview}/>
                            <button type="button" onClick={(e) => {e.stopPropagation(); handleRemoveImage(product.mainImage)}} style={styles.removeImageButton}>x</button>
                        </div>
                    )}
                    <p style={{ marginTop: '10px' }}>{isAnyUploading ? 'Đang tải lên...' : (product.id ? (product.mainImage ? 'Click để THAY THẾ ảnh chính' : 'Click để TẢI ẢNH chính') : 'LƯU thông tin cơ bản để tải ảnh')}</p>
                </div>
                
                {/* Color Images Upload */}
                <h4 style={styles.subHeader}>Ảnh chi tiết theo Màu sắc</h4>
                <p style={{color: '#888'}}>Thư mục ảnh sẽ tự động tạo khi bạn thêm Màu sắc trong phần Biến thể.</p>
                {colorKeys.map(color => {
                    const isUploadingColor = Object.keys(uploading).some(key => key.includes(`colorImages_${color}_`));
                    const currentImages = product.colorImages?.[color] || [];

                    return (
                        <div key={color} style={{ border: '1px dashed #555', padding: '10px', marginTop: '15px', backgroundColor: '#333' }}>
                            <label style={{...styles.label, color: '#C40000'}}>{color}: ({currentImages.length} ảnh)</label>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', marginBottom: '10px' }}>
                                {currentImages.map((url, index) => (
                                    <div key={index} style={styles.imageContainer}>
                                        <img src={url} alt={`Color ${color} img ${index}`} style={styles.imagePreview} onClick={() => triggerUpload(`colorUpload_${color}_${index}`, url)}/>
                                        <button type="button" onClick={(e) => {e.stopPropagation(); handleRemoveImage(url, color, index)}} style={styles.removeImageButton}>x</button>
                                        <input type="file" accept="image/*" onChange={(e) => handleFileUpload(e, 'colorImages', color, e.target.dataset.oldurl)} style={{ display: 'none' }} ref={el => fileInputRefs.current[`colorUpload_${color}_${index}`] = el} disabled={!product.id || isAnyUploading}/>
                                    </div>
                                ))}
                            </div>
                            <div style={{...styles.uploadBox, border: '1px dashed #A0A0A0', padding: '10px', backgroundColor: isUploadingColor ? '#444' : '#292929'}} onClick={() => triggerUpload(`colorUpload_new_${color}`)}>
                                <input type="file" accept="image/*" onChange={(e) => handleFileUpload(e, 'colorImages', color, e.target.dataset.oldurl)} style={{ display: 'none' }} ref={el => fileInputRefs.current[`colorUpload_new_${color}`] = el} disabled={!product.id || isAnyUploading}/>
                                {isUploadingColor ? 'Đang tải lên...' : `+ TẢI THÊM ảnh mới cho màu ${color}`}
                            </div>
                        </div>
                    );
                })}
            </section>
        );
    };

    const renderOfferSection = () => (
        <section>
            <h3 style={styles.sectionHeader}>2. Quản lý Khuyến mãi & Meta</h3>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                <div style={styles.formGroup}>
                    <label style={styles.label}>
                        <input type="checkbox" name="isOffer" checked={product.isOffer || false} onChange={handleChange} style={{marginRight: '10px'}}/>
                        **BẬT KHUYẾN MÃI**
                    </label>
                </div>
                <div style={styles.formGroup}>
                    <label style={styles.label}>
                        <input type="checkbox" name="isFeatured" checked={product.isFeatured || false} onChange={handleChange} style={{marginRight: '10px'}}/>
                        **Đánh Dấu Nổi Bật**
                    </label>
                </div>
            </div>

            {(product.isOffer) && (
                <div style={{ padding: '15px', border: '1px solid #C40000', borderRadius: '4px', backgroundColor: '#292929' }}>
                    <h4 style={{...styles.subHeader, color: '#C40000'}}>Chi Tiết Khuyến Mãi</h4>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                        <div style={styles.formGroup}>
                            <label style={styles.label}>Loại Giảm giá:</label>
                            <select name="offerType" value={product.offer?.offerType || 'Percentage'} onChange={handleOfferChange} style={styles.input}>
                                {offerTypeOptions.map(t => <option key={t} value={t}>{t === 'Percentage' ? 'Phần trăm (%)' : 'Giá trị cố định (VND)'}</option>)}
                            </select>
                        </div>
                        <div style={styles.formGroup}>
                            <label style={styles.label}>Giá Trị Giảm Giá:</label>
                            <input type="number" name="offerValue" value={product.offer?.offerValue || 0} onChange={handleOfferChange} style={styles.input} required />
                        </div>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                        <div style={styles.formGroup}>
                            <label style={styles.label}>Ngày Bắt Đầu:</label>
                            <input type="date" name="startDate" value={formatTimestampToDateInput(product.offer?.startDate)} onChange={handleOfferChange} style={styles.input} />
                        </div>
                        <div style={styles.formGroup}>
                            <label style={styles.label}>Ngày Kết Thúc:</label>
                            <input type="date" name="endDate" value={formatTimestampToDateInput(product.offer?.endDate)} onChange={handleOfferChange} style={styles.input} />
                        </div>
                    </div>
                </div>
            )}
            
            <div style={{ marginTop: '20px', padding: '15px', border: '1px solid #444', borderRadius: '4px', backgroundColor: '#292929' }}>
                <p>ID Sản Phẩm: <strong style={{color: '#C40000'}}>{product.id || 'Chưa lưu'}</strong></p>
                <p>Ngày Tạo: {product.createdAt ? new Date(Number(product.createdAt)).toLocaleString() : 'N/A'}</p>
                <p>Đánh giá TB: {(product.averageRating || 0).toFixed(1)} (Reviews: {product.totalReviews || 0})</p>
            </div>
        </section>
    );

    const renderVariantSection = () => {
        return (
            <section>
                <h3 style={styles.sectionHeader}>3. Quản lý Biến thể (Variants: Màu sắc & Size)</h3>
                <p style={{color: '#888', marginBottom: '15px'}}>Kích hoạt (Active) biến thể để chúng hiển thị trên ứng dụng. Cập nhật Tồn kho và Giá riêng tại đây.</p>
                
                {/* Khu vực thêm màu mới */}
                <div style={{ marginBottom: '20px', display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
                    <div style={{ position: 'relative', flexGrow: 1, maxWidth: '300px' }}>
                        <input type="text" placeholder="Nhập/Gợi ý tên Màu mới" value={newColorInput} onChange={handleNewColorInputChange} style={{...styles.input, width: '100%', border: '2px solid #007bff'}}/>
                        {suggestedColors.length > 0 && (
                            <div style={{...styles.autosuggestList, top: '40px'}}>
                                {suggestedColors.map(color => (
                                    <div key={color} onClick={() => selectSuggestedColor(color)} style={styles.suggestItem}> {color} </div>
                                ))}
                            </div>
                        )}
                    </div>
                    <button onClick={() => addNewColorToProduct(newColorInput)} style={{...styles.saveButton, backgroundColor: '#007bff', margin: 0, padding: '10px 15px'}}> + Thêm Màu </button>
                </div>

                <table style={styles.table}>
                    <thead>
                        <tr>
                            <th style={{...styles.th, width: '5%', borderLeft: '1px solid #C40000'}}>Ảnh</th>
                            <th style={{...styles.th, width: '18%'}}>Màu sắc</th>
                            <th style={{...styles.th, width: '12%'}}>Biến thể Active</th>
                            <th style={{...styles.th, width: '12%'}}>Tổng Tồn kho</th>
                            <th style={{...styles.th, width: '10%', borderRight: '1px solid #C40000'}}>Chi tiết</th>
                        </tr>
                    </thead>
                    <tbody>
                        {Object.entries(groupedVariants).map(([colorKey, group]) => {
                            const isOpen = openColor === colorKey;
                            const totalStockStyle = group.totalStock < 5 ? styles.lowStock : {};

                            return (
                                <React.Fragment key={colorKey}>
                                    <tr style={styles.groupRow(isOpen)}>
                                        <td style={{...styles.td, borderLeft: isOpen ? '1px solid #C40000' : '1px solid #444', borderTop: isOpen ? 'none' : '1px solid #444'}}>
                                            <img src={group.displayImage} alt={colorKey} style={{ width: '40px', height: '40px', objectFit: 'cover', borderRadius: '2px' }}/>
                                        </td>
                                        <td style={styles.td} onClick={() => setOpenColor(isOpen ? null : colorKey)}>
                                            <span style={{ fontWeight: 'bold' }}>{colorKey}</span>
                                            <button onClick={(e) => { e.stopPropagation(); handleDeleteColorGroupPermanently(colorKey); }} style={{ background: 'none', border: 'none', cursor: 'pointer', marginLeft: '10px', color: '#FF4D4D' }} title={`Xóa vĩnh viễn màu ${colorKey}`}>🗑️</button>
                                        </td>
                                        <td style={styles.td}>{group.variants.filter(v => v.isActive).length} / {FIXED_SIZES.length}</td>
                                        <td style={{...styles.td, ...totalStockStyle}}>{group.totalStock}</td>
                                        <td style={{...styles.td, borderRight: isOpen ? '1px solid #C40000' : '1px solid #444'}} onClick={() => setOpenColor(isOpen ? null : colorKey)}>
                                            {isOpen ? '🔽 Đóng' : '▶️ Mở chi tiết'}
                                        </td>
                                    </tr>

                                    {isOpen && (
                                        <tr>
                                            <td colSpan={5} style={{ padding: '0', border: 'none' }}>
                                                <table style={styles.detailTable}>
                                                    <thead>
                                                        <tr>
                                                            <th style={{...styles.th, width: '10%', borderLeft: '1px solid #C40000', backgroundColor: '#000'}}>Size</th>
                                                            <th style={{...styles.th, width: '20%', backgroundColor: '#000'}}>SKU (VariantId)</th>
                                                            <th style={{...styles.th, width: '20%', backgroundColor: '#000'}}>Giá Bán Lẻ</th>
                                                            <th style={{...styles.th, width: '20%', backgroundColor: '#000'}}>Tồn kho</th>
                                                            <th style={{...styles.th, width: '20%', borderRight: '1px solid #C40000', backgroundColor: '#000'}}>Trạng thái</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        {group.variants.map(v => {
                                                            const uniqueKey = v.variantId || `new_temp_${v.color}_${v.size}`;
                                                            const isLowStock = v.quantity < 5 && v.quantity > 0 && v.isActive;
                                                            const isDisabled = !v.isActive;

                                                            return (
                                                                <tr key={uniqueKey} style={isDisabled ? { opacity: 0.5 } : {}}>
                                                                    <td style={{...styles.td, borderLeft: '1px solid #C40000', backgroundColor: '#1A1A1A', color: isDisabled ? '#aaa' : '#E0E0E0' }}>{v.size}</td>
                                                                    <td style={{...styles.td, backgroundColor: '#1A1A1A'}}>
                                                                        <input type="text" value={v.variantId || ''} onChange={(e) => handleVariantChange(uniqueKey, 'variantId', e.target.value)} style={styles.input} disabled={isDisabled} />
                                                                    </td>
                                                                    <td style={{...styles.td, backgroundColor: '#1A1A1A'}}>
                                                                        <input type="number" value={v.price || 0} onChange={(e) => handleVariantChange(uniqueKey, 'price', e.target.value)} style={styles.input} min="0" disabled={isDisabled}/>
                                                                        <small style={{display: 'block', color: '#888', marginTop: '4px'}}>{formatCurrency(v.price)}</small>
                                                                    </td>
                                                                    <td style={{...styles.td, ...totalStockStyle, backgroundColor: '#1A1A1A'}}>
                                                                        <input type="number" value={v.quantity || 0} onChange={(e) => handleVariantChange(uniqueKey, 'quantity', e.target.value)} style={styles.input} min="0" disabled={isDisabled}/>
                                                                    </td>
                                                                    <td style={{...styles.td, borderRight: '1px solid #C40000', backgroundColor: '#1A1A1A'}}>
                                                                        <select value={v.status || 'Inactive'} onChange={(e) => handleStatusChange(uniqueKey, e.target.value)} style={styles.statusDropdown(v.status)}>
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
                                    )}
                                </React.Fragment>
                            );
                        })}
                    </tbody>
                </table>
            </section>
        );
    };

    return (
        <div style={styles.overlay}>
            <div style={styles.modalContent}>
                <h2 style={{ borderBottom: '1px solid #555', paddingBottom: '10px', marginBottom: '20px', color: '#C40000', fontSize: '28px' }}>
                    {isEditing ? `Sửa Sản Phẩm: ${product.name}` : 'Thêm Sản Phẩm Mới'}
                </h2>
                
                <form onSubmit={handleSave}>
                    {renderImageSection()}
                    {renderOfferSection()}
                    {renderVariantSection()}

                    <div style={{ marginTop: '30px', borderTop: '1px solid #555', paddingTop: '20px', textAlign: 'right' }}>
                        <button type="button" style={{ ...styles.saveButton, backgroundColor: '#666' }} onClick={onClose} disabled={saving || isAnyUploading}>
                            Đóng (Chưa lưu)
                        </button>
                        <button type="submit" style={styles.saveButton} disabled={saving || isAnyUploading}>
                            {saving ? 'Đang lưu...' : (isAnyUploading ? 'Vui lòng đợi tải ảnh...' : 'LƯU TẤT CẢ THAY ĐỔI')}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default ProductManagementModal;