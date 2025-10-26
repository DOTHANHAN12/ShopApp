import React, { useState, useRef } from 'react'; 
import { doc, updateDoc, collection, addDoc } from 'firebase/firestore';
import { db } from '../firebaseConfig';
import { uploadFile, deleteFile } from '../firebaseConfig'; 

// --- HÀM TIỆN ÍCH THỜI GIAN ---
const formatTimestampToDateInput = (timestamp) => {
    if (!timestamp || typeof timestamp !== 'number') return '';
    // Tạo đối tượng Date từ timestamp số
    const date = new Date(timestamp); 
    // Chuyển sang định dạng YYYY-MM-DD
    return date.toISOString().split('T')[0];
};

const dateInputToTimestamp = (dateString) => {
    if (!dateString) return null;
    // Tạo timestamp từ đầu ngày (midnight) theo múi giờ cục bộ
    return new Date(dateString).getTime();
};

// HÀM TÍNH TOÁN GIÁ SAU KHUYẾN MÃI CHO VARIANT
const calculateFinalPriceForVariant = (variantPrice, offer) => {
    if (!offer || !offer.isOffer || !variantPrice) return variantPrice;
    
    const value = parseFloat(offer.offerValue) || 0;
    const price = parseFloat(variantPrice);
    
    let finalPrice = price;

    if (offer.offerType === 'Percentage') {
        finalPrice = price - (price * value / 100);
    }
    if (offer.offerType === 'FlatAmount') {
        finalPrice = price - value;
    }
    
    finalPrice = Math.round(finalPrice * 100) / 100;
    return Math.max(0, finalPrice);
};


const modalStyles = {
    overlay: { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0, 0, 0, 0.7)', zIndex: 1000, display: 'flex', justifyContent: 'center', alignItems: 'center' },
    modalContent: { backgroundColor: '#FFFFFF', padding: '30px', borderRadius: '8px', width: '90%', maxWidth: '900px', maxHeight: '90vh', overflowY: 'auto' },
    formGroup: { marginBottom: '15px' },
    label: { display: 'block', fontWeight: 'bold', marginBottom: '5px' },
    input: { width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' },
    saveButton: { backgroundColor: '#C40000', color: 'white', padding: '10px 15px', border: 'none', borderRadius: '4px', cursor: 'pointer', marginRight: '10px' },
    tabBar: { display: 'flex', borderBottom: '1px solid #eee', marginBottom: '20px' },
    tab: (isActive) => ({
        padding: '10px 20px',
        cursor: 'pointer',
        fontWeight: isActive ? 'bold' : 'normal',
        borderBottom: isActive ? '3px solid #C40000' : '3px solid transparent',
        color: isActive ? '#C40000' : '#333',
    }),
    uploadBox: { border: '2px dashed #000', padding: '20px', textAlign: 'center', cursor: 'pointer', backgroundColor: '#f9f9f9' },
    imagePreview: { width: '80px', height: '80px', objectFit: 'cover', borderRadius: '4px', marginRight: '10px' },
    imageContainer: { position: 'relative', display: 'inline-block', margin: '5px' },
    removeImageButton: { position: 'absolute', top: '-8px', right: '-8px', background: '#C40000', color: 'white', border: 'none', borderRadius: '50%', width: '20px', height: '20px', fontSize: '12px', cursor: 'pointer' }
};

const ProductDetailModal = ({ product: initialProduct, onClose, onSave }) => {
    const [product, setProduct] = useState(initialProduct);
    const [saving, setSaving] = useState(false);
    const [activeTab, setActiveTab] = useState('core');
    const isEditing = !!initialProduct.id;
    const [uploading, setUploading] = useState({}); 

    const fileInputRefs = useRef({}); 
    const colorInputRef = useRef(null); 

    const statusOptions = ['Active', 'Draft', 'Archived'];
    const offerTypeOptions = ['Percentage', 'FlatAmount'];

    const isAnyUploading = Object.values(uploading).some(Boolean);

    const isFirebaseStorageUrl = (url) => {
        return typeof url === 'string' && 
               (url.includes('firebasestorage.googleapis.com') || url.includes('.appspot.com/o/'));
    };
    
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
        if (type === 'date') {
            // Chuyển đổi date string thành timestamp số khi input thay đổi
            processedValue = dateInputToTimestamp(value);
        } else if (type === 'number') {
            processedValue = parseFloat(value);
        }

        setProduct(prev => ({
            ...prev,
            offer: {
                ...prev.offer,
                [name]: processedValue
            }
        }));
    };

    // ----------------------------------------------------------------------
    // LOGIC UPLOAD VÀ IMAGE MANAGEMENT
    // ----------------------------------------------------------------------
    const handleFileUpload = async (e, fieldName, colorKey = null, oldImageUrl = null) => {
        const file = e.target.files[0];
        if (!file) return;

        if (!product.id) {
            alert("Vui lòng LƯU THÔNG TIN CƠ BẢN trước khi tải ảnh.");
            return;
        }

        const uploadKey = colorKey ? `${fieldName}_${colorKey}_${file.name}` : `${fieldName}_${file.name}`;
        setUploading(prev => ({ ...prev, [uploadKey]: true }));
        
        try {
            if (oldImageUrl && isFirebaseStorageUrl(oldImageUrl)) {
                 await deleteFile(oldImageUrl); 
            }

            const path = `products/${product.id}/${fieldName}${colorKey ? '/' + colorKey : ''}`;
            const url = await uploadFile(file, path); 

            setProduct(prev => {
                if (fieldName === 'mainImage') {
                    return { ...prev, mainImage: url };
                }
                
                if (colorKey) {
                    const newColorImages = { ...prev.colorImages } || {};
                    const currentImages = newColorImages[colorKey] || [];
                    
                    if (oldImageUrl && currentImages.includes(oldImageUrl)) {
                        const index = currentImages.indexOf(oldImageUrl);
                        if (index !== -1) currentImages[index] = url; 
                    } else {
                        if (!currentImages.includes(url)) {
                           newColorImages[colorKey] = [...currentImages, url];
                        }
                    }
                    return { ...prev, colorImages: newColorImages };
                }
                return prev;
            });
            alert(`Tải lên thành công: ${file.name}`);

        } catch (error) {
            console.error("[!!! LỖI UPLOAD CẤP ĐỘ CAO !!!]", error);
            const errorMessage = error.code ? `FIREBASE ERROR CODE: ${error.code}` : "Lỗi mạng hoặc CORS không rõ.";
            alert(`LỖI TẢI ẢNH: ${errorMessage}`);
        } finally {
            setUploading(prev => ({ ...prev, [uploadKey]: false }));
            e.target.value = null; 
        }
    };
    
    const handleAddColorLogic = (colorName) => { 
        if (!colorName || colorName.trim() === '') {
            alert("Vui lòng nhập tên màu.");
            return;
        }
        
        if (product.colorImages?.[colorName]) {
            alert(`Màu "${colorName}" đã tồn tại.`);
            return;
        }

        setProduct(prev => ({
            ...prev,
            colorImages: {
                ...prev.colorImages,
                [colorName]: [] 
            }
        }));
        
        colorInputRef.current.value = ''; 
    };

    const handleAddColor = () => {
        const colorName = colorInputRef.current?.value?.trim();
        handleAddColorLogic(colorName);
    };

    const handleRemoveImage = async (imageUrl, color = null, urlIndex = -1) => {
        if (!imageUrl) return;

        if (!window.confirm("Bạn có chắc chắn muốn xóa ảnh này?")) return;

        const uploadIdKey = `delete_${imageUrl.substring(imageUrl.length - 20)}`;
        setUploading(prev => ({ ...prev, [uploadIdKey]: true }));

        if (isFirebaseStorageUrl(imageUrl)) {
            await deleteFile(imageUrl); 
        }
        
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

    // Hàm tiện ích để kích hoạt upload file input
    const triggerUpload = (refName, oldImageUrl = null) => {
        if (!product.id || isAnyUploading) return;
        
        const fileInput = fileInputRefs.current[refName];
        
        if (oldImageUrl) {
            fileInput.dataset.oldurl = oldImageUrl;
        } else {
            delete fileInput.dataset.oldurl;
        }
        
        fileInput.click();
    };

    // ----------------------------------------------------------------------
    // HÀM LƯU DỮ LIỆU
    // ----------------------------------------------------------------------
    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);
        if (isAnyUploading) {
             alert("Vui lòng đợi quá trình tải ảnh hoàn tất trước khi lưu.");
             setSaving(false);
             return;
        }
        
        // Chuẩn bị dữ liệu Offer (vì offer dates đã là timestamp trong state)
        const offerDataToSave = product.isOffer ? {
            ...product.offer,
            startDate: product.offer.startDate || null,
            endDate: product.offer.endDate || null,
        } : null;

        const dataToSave = {
            ...product,
            basePrice: parseFloat(product.basePrice || 0),
            isOffer: product.isOffer || false,
            isFeatured: product.isFeatured || false,
            offer: offerDataToSave, 
            updatedAt: Date.now() 
        };
        
        let newProductId = product.id;

        try {
            if (isEditing) {
                const docRef = doc(db, 'products', product.id);
                await updateDoc(docRef, dataToSave);
                alert("Cập nhật thành công!");
            } else {
                const newDocRef = await addDoc(collection(db, 'products'), dataToSave);
                newProductId = newDocRef.id;
                setProduct(prev => ({ ...prev, id: newProductId })); 
                alert(`Thêm sản phẩm mới thành công! ID: ${newProductId}. Bạn có thể tải ảnh ngay bây giờ.`);
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

    // ----------------------------------------------------------------------
    // HÀM RENDER CÁC TAB RIÊNG LẺ
    // ----------------------------------------------------------------------
    const renderCoreTab = () => (
        <div>
            <div style={modalStyles.formGroup}>
                <label style={modalStyles.label}>Tên Sản Phẩm:</label>
                <input type="text" name="name" value={product.name || ''} onChange={handleChange} style={modalStyles.input} required />
            </div>
            <div style={modalStyles.formGroup}>
                <label style={modalStyles.label}>Mô Tả:</label>
                <textarea name="desc" value={product.desc || ''} onChange={handleChange} style={{ ...modalStyles.input, height: '100px' }} />
            </div>
            <div style={modalStyles.formGroup}>
                <label style={modalStyles.label}>Giá Gốc (Base Price):</label>
                <input type="number" name="basePrice" value={product.basePrice || 0} onChange={handleChange} style={modalStyles.input} required min="0"/>
            </div>
            <div style={modalStyles.formGroup}>
                <label style={modalStyles.label}>Danh Mục:</label>
                <input type="text" name="category" value={product.category || ''} onChange={handleChange} style={modalStyles.input} />
            </div>
            <div style={modalStyles.formGroup}>
                <label style={modalStyles.label}>Loại Sản Phẩm:</label>
                <input type="text" name="type" value={product.type || ''} onChange={handleChange} style={modalStyles.input} />
            </div>
            <div style={modalStyles.formGroup}>
                <label style={modalStyles.label}>Trạng thái:</label>
                <select name="status" value={product.status || 'Draft'} onChange={handleChange} style={modalStyles.input}>
                    {statusOptions.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
            </div>
        </div>
    );

    const renderImageTab = () => {
        // FIXED: Lấy tất cả các key màu (kể cả màu được thêm từ Modal Biến thể)
        let colorKeys = new Set(Object.keys(product.colorImages || {}));
        (product.variants || []).forEach(v => {
            if (v.color) colorKeys.add(v.color);
        });
        colorKeys = Array.from(colorKeys);

        return (
            <div>
                {/* Phần Ảnh Chính */}
                <h3>Ảnh Chính (Main Image)</h3>
                <div style={modalStyles.formGroup}>
                    <label style={modalStyles.label}>{product.mainImage ? 'Ảnh Chính Hiện Tại:' : 'Chưa có ảnh chính'}</label>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                        {product.mainImage && (
                            <div style={modalStyles.imageContainer}>
                                <img src={product.mainImage} alt="Main Preview" style={modalStyles.imagePreview}/>
                                <button 
                                    type="button" 
                                    onClick={() => handleRemoveImage(product.mainImage)}
                                    style={modalStyles.removeImageButton}
                                    disabled={isAnyUploading}
                                >
                                    x
                                </button>
                            </div>
                        )}
                    </div>
                    <div 
                        style={{ 
                            ...modalStyles.uploadBox, 
                            cursor: product.id && !isAnyUploading ? 'pointer' : 'not-allowed',
                            backgroundColor: isAnyUploading ? '#ffe0e0' : '#f9f9f9'
                        }}
                        onClick={() => triggerUpload('mainImageUpload', product.mainImage || null)} 
                    >
                        <input 
                            type="file" 
                            accept="image/*" 
                            onChange={(e) => handleFileUpload(e, 'mainImage', null, e.target.dataset.oldurl)} 
                            style={{ display: 'none' }}
                            ref={el => fileInputRefs.current['mainImageUpload'] = el}
                            disabled={!product.id || isAnyUploading} 
                        />
                        {isAnyUploading ? 'Đang tải lên...' : (
                            product.id ? (product.mainImage ? 'Click để THAY THẾ ảnh chính mới' : 'Click hoặc Kéo thả ảnh chính tại đây') : 'LƯU thông tin cơ bản để tải ảnh'
                        )}
                    </div>
                </div>

                <h3>Ảnh theo Màu sắc (Color Images)</h3>
                {(colorKeys.length === 0) && (
                    <p style={{ color: '#666' }}>Chưa có màu nào được tạo. Vui lòng tạo màu ở tab Quản lý Biến thể.</p>
                )}
                {colorKeys.map(color => {
                    const isUploadingColor = Object.keys(uploading).some(key => key.includes(`colorImages_${color}_`));
                    const currentImages = product.colorImages?.[color] || [];

                    return (
                        <div key={color} style={{ border: '1px dashed #ccc', padding: '10px', marginTop: '10px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                                <label style={modalStyles.label}>{color}:</label>
                                <button 
                                    type="button" 
                                    onClick={() => {
                                        if (!window.confirm(`Xóa tất cả ảnh cho màu "${color}"?`)) return;
                                        const newColorImages = {...product.colorImages};
                                        delete newColorImages[color];
                                        setProduct(prev => ({...prev, colorImages: newColorImages}));
                                    }}
                                    style={{ backgroundColor: '#aaa', color: 'white', padding: '5px', borderRadius: '4px', border: 'none' }}
                                >
                                    Xóa Thư mục Ảnh
                                </button>
                            </div>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', marginBottom: '10px' }}>
                                {currentImages.map((url, index) => (
                                    <div key={index} style={modalStyles.imageContainer}>
                                        <img 
                                            src={url} 
                                            alt={`Color ${color} img ${index}`} 
                                            style={modalStyles.imagePreview}
                                            onClick={() => triggerUpload(`colorUpload_${color}_${index}`, url)} 
                                        />
                                        <button 
                                            type="button" 
                                            onClick={() => handleRemoveImage(url, color, index)}
                                            style={modalStyles.removeImageButton}
                                        >
                                            x
                                        </button>
                                        <input 
                                            type="file" 
                                            accept="image/*" 
                                            onChange={(e) => handleFileUpload(e, 'colorImages', color, e.target.dataset.oldurl)} 
                                            style={{ display: 'none' }}
                                            ref={el => fileInputRefs.current[`colorUpload_${color}_${index}`] = el}
                                            disabled={!product.id || isAnyUploading}
                                        />
                                    </div>
                                ))}
                            </div>
                            
                            {/* NÚT THÊM ẢNH MỚI */}
                            <div 
                                style={{ 
                                    ...modalStyles.uploadBox, 
                                    cursor: product.id && !isUploadingColor ? 'pointer' : 'not-allowed', 
                                    padding: '10px',
                                    backgroundColor: isUploadingColor ? '#ffe0e0' : '#f9f9f9'
                                }}
                                onClick={() => triggerUpload(`colorUpload_new_${color}`)} 
                            >
                                <input 
                                    type="file" 
                                    accept="image/*" 
                                    onChange={(e) => handleFileUpload(e, 'colorImages', color, e.target.dataset.oldurl)}
                                    style={{ display: 'none' }}
                                    ref={el => fileInputRefs.current[`colorUpload_new_${color}`] = el}
                                    disabled={!product.id || isAnyUploading}
                                />
                                {isUploadingColor ? 'Đang tải lên...' : (product.id ? `+ Click để TẢI THÊM ảnh mới cho màu ${color}` : 'LƯU thông tin cơ bản để tải ảnh')}
                            </div>
                        </div>
                    );
                })}
                
            </div>
        );
    };

    const renderOfferTab = () => (
        <div>
            <div style={modalStyles.formGroup}>
                <label style={modalStyles.label}>
                    <input
                        type="checkbox"
                        name="isOffer"
                        checked={product.isOffer || false}
                        onChange={handleChange}
                        style={modalStyles.toggleSwitch}
                    />
                    **BẬT/TẮT KHUYẾN MÃI**
                </label>
            </div>

            {(product.isOffer) && (
                <div style={{ padding: '15px', border: '1px solid #C40000', borderRadius: '4px' }}>
                    <h3>Chi Tiết Khuyến Mãi (OfferDetails)</h3>
                    <div style={modalStyles.formGroup}>
                        <label style={modalStyles.label}>Loại Giảm giá:</label>
                        <select name="offerType" value={product.offer?.offerType || 'Percentage'} onChange={handleOfferChange} style={modalStyles.input}>
                            {offerTypeOptions.map(t => <option key={t} value={t}>{t === 'Percentage' ? 'Phần trăm (%)' : 'Giá trị cố định (VND)'}</option>)}
                        </select>
                    </div>
                    <div style={modalStyles.formGroup}>
                        <label style={modalStyles.label}>Giá Trị Giảm Giá:</label>
                        <input type="number" name="offerValue" value={product.offer?.offerValue || 0} onChange={handleOfferChange} style={modalStyles.input} required />
                    </div>
                    {/* FIXED: Dùng formatTimestampToDateInput để hiển thị date từ timestamp */}
                    <div style={modalStyles.formGroup}>
                        <label style={modalStyles.label}>Ngày Bắt Đầu:</label>
                        <input 
                            type="date" 
                            name="startDate" 
                            value={formatTimestampToDateInput(product.offer?.startDate)} 
                            onChange={handleOfferChange} 
                            style={modalStyles.input} 
                        />
                    </div>
                    <div style={modalStyles.formGroup}>
                        <label style={modalStyles.label}>Ngày Kết Thúc:</label>
                        <input 
                            type="date" 
                            name="endDate" 
                            value={formatTimestampToDateInput(product.offer?.endDate)} 
                            onChange={handleOfferChange} 
                            style={modalStyles.input} 
                        />
                    </div>
                </div>
            )}
        </div>
    );

    const renderManagementTab = () => (
        <div>
            <div style={modalStyles.formGroup}>
                <label style={modalStyles.label}>
                    <input
                        type="checkbox"
                        name="isFeatured"
                        checked={product.isFeatured || false}
                        onChange={handleChange}
                        style={modalStyles.toggleSwitch}
                    />
                    **Đánh Dấu Nổi Bật (Is Featured)**
                </label>
            </div>
            <p>ID Sản Phẩm: **{product.id || 'Chưa lưu'}**</p>
            {/* FIXED: Định dạng lại timestamp số thành định dạng ngày tháng */}
            <p>Ngày Tạo: **{product.createdAt ? new Date(Number(product.createdAt)).toLocaleString() : 'N/A'}**</p>
            <p>Ngày Cập nhật: **{product.updatedAt ? new Date(Number(product.updatedAt)).toLocaleString() : 'N/A (Sẽ tự động cập nhật)'}**</p>
            <p>Đánh giá TB: **{(product.averageRating || 0).toFixed(1)}** (Tổng Reviews: **{product.totalReviews || 0}**)</p>
        </div>
    );

    // ----------------------------------------------------------------------
    // HÀM TỔNG HỢP RENDER
    // ----------------------------------------------------------------------
    const renderContent = () => { 
        switch (activeTab) {
            case 'core': return renderCoreTab();
            case 'image': return renderImageTab();
            case 'offer': return renderOfferTab();
            case 'management': return renderManagementTab();
            default: return null;
        }
    };

    return (
        <div style={modalStyles.overlay}>
            <div style={modalStyles.modalContent}>
                <h2 style={{ borderBottom: '1px solid #eee', paddingBottom: '10px', marginBottom: '20px' }}>
                    {isEditing ? `Sửa Sản Phẩm: ${product.name}` : 'Thêm Sản Phẩm Mới'}
                </h2>
                {product.id ? null : <p style={{color: '#C40000', fontWeight: 'bold'}}>*** LƯU Ý: Phải LƯU THÔNG TIN CƠ BẢN trước khi tải ảnh! ***</p>}

                <div style={modalStyles.tabBar}>
                    <span style={modalStyles.tab(activeTab === 'core')} onClick={() => setActiveTab('core')}>Thông tin Cơ bản</span>
                    <span style={modalStyles.tab(activeTab === 'image')} onClick={() => setActiveTab('image')}>Hình ảnh & Media</span>
                    <span style={modalStyles.tab(activeTab === 'offer')} onClick={() => setActiveTab('offer')}>Khuyến mãi</span>
                    <span style={modalStyles.tab(activeTab === 'management')} onClick={() => setActiveTab('management')}>Quản lý & Meta</span>
                </div>

                <form onSubmit={handleSave}>
                    {renderContent()}

                    <div style={{ marginTop: '30px', borderTop: '1px solid #eee', paddingTop: '20px' }}>
                        <button type="submit" style={modalStyles.saveButton} disabled={saving || isAnyUploading}>
                            {saving ? 'Đang lưu...' : (isAnyUploading ? 'Vui lòng đợi tải ảnh...' : 'Lưu Tất Cả Thay Đổi')}
                        </button>
                        <button type="button" style={{ ...modalStyles.saveButton, backgroundColor: '#666' }} onClick={onClose} disabled={saving || isAnyUploading}>
                            Hủy / Đóng
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default ProductDetailModal;