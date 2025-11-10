import React, { useState, useRef } from 'react';
import { doc, updateDoc, collection, setDoc } from 'firebase/firestore';
import { db } from '../firebaseConfig';
import { uploadFile, deleteFile } from '../firebaseConfig';
import { generateBarcodeFromId } from '../utils/barcodeUtils';

// --- HÀM TIỆN ÍCH MỚI: TẠO SLUG ID ---
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

// --- HÀM TIỆN ÍCH THỜI GIAN ---
const formatTimestampToDateInput = (timestamp) => {
  if (!timestamp) return '';
  if (typeof timestamp === 'string') {
    return timestamp;
  }
  if (typeof timestamp === 'number') {
    const date = new Date(timestamp);
    return date.toISOString().split('T')[0];
  }
  return '';
};

const modalStyles = {
  overlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
    zIndex: 1000,
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center'
  },
  modalContent: {
    backgroundColor: '#1A1A1A',
    padding: '30px',
    borderRadius: '8px',
    width: '90%',
    maxWidth: '900px',
    maxHeight: '90vh',
    overflowY: 'auto',
    color: '#E0E0E0'
  },
  formGroup: {
    marginBottom: '15px'
  },
  label: {
    display: 'block',
    fontWeight: 'bold',
    marginBottom: '5px'
  },
  input: {
    width: '100%',
    padding: '10px',
    border: '1px solid #444',
    borderRadius: '4px',
    boxSizing: 'border-box',
    backgroundColor: '#333',
    color: '#E0E0E0'
  },
  saveButton: {
    backgroundColor: '#C40000',
    color: 'white',
    padding: '10px 15px',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    marginRight: '10px',
    transition: 'background-color 0.2s'
  },
  tabBar: {
    display: 'flex',
    borderBottom: '1px solid #555',
    marginBottom: '20px'
  },
  tab: (isActive) => ({
    padding: '10px 20px',
    cursor: 'pointer',
    fontWeight: isActive ? 'bold' : 'normal',
    borderBottom: isActive ? '3px solid #C40000' : '3px solid transparent',
    color: isActive ? '#C40000' : '#A0A0A0',
    transition: 'color 0.2s'
  }),
  uploadBox: {
    border: '2px dashed #C40000',
    padding: '20px',
    textAlign: 'center',
    cursor: 'pointer',
    backgroundColor: '#292929'
  },
  imagePreview: {
    width: '80px',
    height: '80px',
    objectFit: 'cover',
    borderRadius: '4px',
    marginRight: '10px'
  },
  imageContainer: {
    position: 'relative',
    display: 'inline-block',
    margin: '5px'
  },
  removeImageButton: {
    position: 'absolute',
    top: '-8px',
    right: '-8px',
    background: '#FF4D4D',
    color: 'white',
    border: 'none',
    borderRadius: '50%',
    width: '20px',
    height: '20px',
    fontSize: '12px',
    cursor: 'pointer'
  }
};

const ProductDetailModal = ({ product: initialProduct, onClose, onSave }) => {
  const [product, setProduct] = useState(initialProduct);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState('core');
  const isEditing = !!initialProduct.id;
  const [uploading, setUploading] = useState({});
  const fileInputRefs = useRef({});
  const statusOptions = ['Active', 'Draft', 'Archived'];
  const offerTypeOptions = ['Percentage', 'FlatAmount'];
  const isAnyUploading = Object.values(uploading).some(Boolean);

  const isFirebaseStorageUrl = (url) => {
    return typeof url === 'string' && (url.includes('firebasestorage.googleapis.com') || url.includes('.appspot.com/o/'));
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setProduct(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : (name === 'basePrice' ? parseFloat(value) : value)
    }));
  };

  // ✅ FIX: Xử lý offer với đúng kiểu dữ liệu
  // offerValue: GIỮ NGUYÊN STRING trong state (sẽ convert lúc save)
  // startDate: String (YYYY-MM-DD)
  // endDate: String (YYYY-MM-DD)
  const handleOfferChange = (e) => {
    const { name, value } = e.target;
    let processedValue = value;

    if (name === 'offerType') {
      processedValue = value; // String: 'Percentage' hoặc 'FlatAmount'
    } else if (name === 'offerValue') {
      // Giữ nguyên string - sẽ convert lúc save
      processedValue = value;
    } else if (name === 'startDate' || name === 'endDate') {
      processedValue = value || null; // String YYYY-MM-DD hoặc null
    }

    setProduct(prev => ({
      ...prev,
      offer: {
        ...prev.offer,
        [name]: processedValue
      }
    }));
  };

  // --- LOGIC UPLOAD VÀ IMAGE MANAGEMENT ---
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

  // --- VALIDATION ---
  const validateBarcode = (barcode) => {
    if (!barcode) return true;
    return /^\d{12,13}$/.test(barcode);
  };

  const validateBeforeSave = () => {
    if (!product.name) {
      alert("Vui lòng nhập Tên Sản Phẩm.");
      return false;
    }
    if (product.barcode && !validateBarcode(product.barcode)) {
      alert("❌ Barcode phải là 12 hoặc 13 chữ số! (VD: 8901002108560)");
      return false;
    }
    return true;
  };

  // --- HÀM LƯU DỮ LIỆU ---
  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);

    if (isAnyUploading) {
      alert("Vui lòng đợi quá trình tải ảnh hoàn tất trước khi lưu.");
      setSaving(false);
      return;
    }

    if (!validateBeforeSave()) {
      setSaving(false);
      return;
    }

    // ✅ QUAN TRỌNG: Convert offerValue thành Integer (Long) khi lưu
    const offerDataToSave = product.isOffer
      ? {
          offerType: product.offer?.offerType || 'Percentage',
          // ✅ Convert thành Integer ngay tại đây - không phải Float
          // Math.round() để chắc chắn lưu là số nguyên
          offerValue: product.offer?.offerValue !== null && product.offer?.offerValue !== undefined && product.offer?.offerValue !== ''
            ? Math.round(parseFloat(product.offer.offerValue))
            : 0,
          startDate: product.offer?.startDate || null, // String YYYY-MM-DD
          endDate: product.offer?.endDate || null // String YYYY-MM-DD
        }
      : null;

    let newProductId = product.id;

    try {
      if (isEditing) {
        // CHỈNH SỬA
        const docRef = doc(db, 'products', product.id);
        const dataToSave = {
          ...product,
          basePrice: parseFloat(product.basePrice || 0),
          barcode: product.barcode || null,
          isOffer: product.isOffer || false,
          isFeatured: product.isFeatured || false,
          offer: offerDataToSave,
          updatedAt: Date.now()
        };
        await updateDoc(docRef, dataToSave);
        alert("✅ Cập nhật thành công!");
      } else {
        // THÊM MỚI
        const productsCollectionRef = collection(db, 'products');
        const newDocRef = doc(productsCollectionRef);
        const temporaryFirestoreId = newDocRef.id;
        const finalCustomId = createSlugId(product.name, temporaryFirestoreId);

        let finalBarcode = product.barcode;
        if (!finalBarcode) {
          finalBarcode = generateBarcodeFromId(finalCustomId);
        }

        const finalDocRef = doc(db, 'products', finalCustomId);
        newProductId = finalCustomId;

        const dataToSave = {
          ...product,
          productId: finalCustomId,
          basePrice: parseFloat(product.basePrice || 0),
          barcode: finalBarcode,
          status: product.status || 'Draft',
          isOffer: product.isOffer || false,
          isFeatured: product.isFeatured || false,
          offer: offerDataToSave,
          createdAt: Date.now(),
          updatedAt: Date.now()
        };

        await setDoc(finalDocRef, dataToSave);
        setProduct(prev => ({
          ...prev,
          id: newProductId,
          productId: newProductId,
          barcode: finalBarcode,
          createdAt: Date.now(),
          updatedAt: Date.now()
        }));
        alert(`✅ Thêm sản phẩm mới thành công!\n\nID: ${newProductId}\nBarcode: ${finalBarcode}\n\nBạn có thể tải ảnh ngay bây giờ.`);
      }

      onSave();
      if (isEditing) onClose();
    } catch (error) {
      console.error("Lỗi khi lưu sản phẩm:", error);
      alert("❌ LỖI LƯU DỮ LIỆU: Vui lòng kiểm tra console.");
    } finally {
      setSaving(false);
    }
  };

  // --- RENDER CÁC TAB ---
  const renderCoreTab = () => (
    <div>
      <div style={modalStyles.formGroup}>
        <label style={modalStyles.label}>Tên Sản Phẩm:</label>
        <input
          type="text"
          name="name"
          value={product.name || ''}
          onChange={handleChange}
          style={modalStyles.input}
          required
        />
      </div>

      <div style={modalStyles.formGroup}>
        <label style={modalStyles.label}>Mô Tả:</label>
        <textarea
          name="description"
          value={product.description || ''}
          onChange={handleChange}
          style={{ ...modalStyles.input, minHeight: '100px' }}
        />
      </div>

      <div style={modalStyles.formGroup}>
        <label style={modalStyles.label}>Giá Gốc (Base Price):</label>
        <input
          type="number"
          name="basePrice"
          value={product.basePrice || 0}
          onChange={handleChange}
          style={modalStyles.input}
          required
          min="0"
        />
      </div>

      <div style={modalStyles.formGroup}>
        <label style={modalStyles.label}>Danh Mục:</label>
        <input
          type="text"
          name="category"
          value={product.category || ''}
          onChange={handleChange}
          style={modalStyles.input}
        />
      </div>

      <div style={modalStyles.formGroup}>
        <label style={modalStyles.label}>Loại Sản Phẩm:</label>
        <input
          type="text"
          name="type"
          value={product.type || ''}
          onChange={handleChange}
          style={modalStyles.input}
        />
      </div>

      <div style={modalStyles.formGroup}>
        <label style={modalStyles.label}>Barcode (Mã vạch):</label>
        {!isEditing && (
          <div style={{ backgroundColor: '#292929', padding: '8px', borderRadius: '4px', marginBottom: '8px', border: '1px solid #444' }}>
            <small style={{ color: '#00FF00' }}>
              💡 <strong>Tự động generate</strong> khi lưu sản phẩm mới.
            </small>
          </div>
        )}
        <input
          type="text"
          name="barcode"
          value={product.barcode || ''}
          onChange={handleChange}
          placeholder={isEditing ? "VD: 8901002108560" : "Để trống → Tự động tạo"}
          style={modalStyles.input}
        />
        <small style={{ color: '#888', marginTop: '5px', display: 'block' }}>
          💡 Mã EAN-13 hoặc UPC-12.
        </small>
      </div>

      <div style={modalStyles.formGroup}>
        <label style={modalStyles.label}>Trạng thái:</label>
        <select name="status" value={product.status || 'Draft'} onChange={handleChange} style={modalStyles.input}>
          {statusOptions.map(s => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>
    </div>
  );

  const renderImageTab = () => {
    let colorKeys = new Set(Object.keys(product.colorImages || {}));
    (product.variants || []).forEach(v => {
      if (v.color) colorKeys.add(v.color);
    });
    colorKeys = Array.from(colorKeys);

    return (
      <div>
        <h3>Ảnh Chính (Main Image)</h3>
        <div style={modalStyles.formGroup}>
          <label style={modalStyles.label}>{product.mainImage ? 'Ảnh Chính Hiện Tại:' : 'Chưa có ảnh chính'}</label>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
            {product.mainImage && (
              <div style={modalStyles.imageContainer}>
                <img src={product.mainImage} alt="Main Preview" style={modalStyles.imagePreview} />
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
              backgroundColor: isAnyUploading ? '#444' : '#292929'
            }}
            onClick={() => triggerUpload('mainImageUpload', product.mainImage || null)}
          >
            <input
              type="file"
              accept="image/*"
              onChange={(e) => handleFileUpload(e, 'mainImage', null, e.target.dataset.oldurl)}
              style={{ display: 'none' }}
              ref={el => (fileInputRefs.current['mainImageUpload'] = el)}
              disabled={!product.id || isAnyUploading}
            />
            {isAnyUploading
              ? 'Đang tải lên...'
              : product.id
              ? product.mainImage
                ? 'Click để THAY THẾ ảnh chính mới'
                : 'Click hoặc Kéo thả ảnh chính tại đây'
              : 'LƯU thông tin cơ bản để tải ảnh'}
          </div>
        </div>

        <h3>Ảnh theo Màu sắc (Color Images)</h3>
        {colorKeys.length === 0 && <p style={{ color: '#888' }}>Chưa có màu nào được tạo. Vui lòng tạo màu ở tab Quản lý Biến thể.</p>}

        {colorKeys.map(color => {
          const isUploadingColor = Object.keys(uploading).some(key => key.includes(`colorImages_${color}_`));
          const currentImages = product.colorImages?.[color] || [];

          return (
            <div key={color} style={{ border: '1px dashed #555', padding: '10px', marginTop: '10px', backgroundColor: '#292929' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                <label style={modalStyles.label}>{color}:</label>
                <button
                  type="button"
                  onClick={() => {
                    if (!window.confirm(`Xóa tất cả ảnh cho màu "${color}"?`)) return;
                    const newColorImages = { ...product.colorImages };
                    delete newColorImages[color];
                    setProduct(prev => ({ ...prev, colorImages: newColorImages }));
                  }}
                  style={{
                    backgroundColor: '#666',
                    color: 'white',
                    padding: '5px',
                    borderRadius: '4px',
                    border: 'none'
                  }}
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
                      ref={el => (fileInputRefs.current[`colorUpload_${color}_${index}`] = el)}
                      disabled={!product.id || isAnyUploading}
                    />
                  </div>
                ))}
              </div>

              <div
                style={{
                  ...modalStyles.uploadBox,
                  cursor: product.id && !isUploadingColor ? 'pointer' : 'not-allowed',
                  padding: '10px',
                  backgroundColor: isUploadingColor ? '#444' : '#292929'
                }}
                onClick={() => triggerUpload(`colorUpload_new_${color}`)}
              >
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => handleFileUpload(e, 'colorImages', color, e.target.dataset.oldurl)}
                  style={{ display: 'none' }}
                  ref={el => (fileInputRefs.current[`colorUpload_new_${color}`] = el)}
                  disabled={!product.id || isAnyUploading}
                />
                {isUploadingColor
                  ? 'Đang tải lên...'
                  : product.id
                  ? `+ Click để TẢI THÊM ảnh mới cho màu ${color}`
                  : 'LƯU thông tin cơ bản để tải ảnh'}
              </div>
            </div>
          );
        })}
      </div>
    );
  };

  // ✅ TAB KHUYẾN MÃI - Fix offerValue convert thành Integer
  const renderOfferTab = () => (
    <div>
      <div style={modalStyles.formGroup}>
        <label style={modalStyles.label}>
          <input
            type="checkbox"
            name="isOffer"
            checked={product.isOffer || false}
            onChange={handleChange}
            style={{ marginRight: '10px' }}
          />
          <strong>BẬT/TẮT KHUYẾN MÃI</strong>
        </label>
      </div>

      {product.isOffer && (
        <div style={{ padding: '15px', border: '1px solid #C40000', borderRadius: '4px', backgroundColor: '#292929' }}>
          <h3>Chi Tiết Khuyến Mãi (OfferDetails)</h3>

          <div style={modalStyles.formGroup}>
            <label style={modalStyles.label}>Loại Giảm giá:</label>
            <select
              name="offerType"
              value={product.offer?.offerType || 'Percentage'}
              onChange={handleOfferChange}
              style={modalStyles.input}
            >
              {offerTypeOptions.map(t => (
                <option key={t} value={t}>
                  {t === 'Percentage' ? 'Phần trăm (%)' : 'Giá trị cố định (VND)'}
                </option>
              ))}
            </select>
          </div>

          <div style={modalStyles.formGroup}>
            <label style={modalStyles.label}>
              Giá Trị Giảm Giá <span style={{ color: '#00FF00' }}>(Long/Integer)</span>:
            </label>
            <input
              type="number"
              name="offerValue"
              value={product.offer?.offerValue || 0}
              onChange={handleOfferChange}
              style={modalStyles.input}
              required
              min="0"
              step="1"
            />
            <small style={{ color: '#888', marginTop: '5px', display: 'block' }}>
              ✅ Tự động convert thành Long (Integer) khi lưu - không lưu số thập phân
            </small>
          </div>

          <div style={modalStyles.formGroup}>
            <label style={modalStyles.label}>
              Ngày Bắt Đầu <span style={{ color: '#00FF00' }}>(String YYYY-MM-DD)</span>:
            </label>
            <input
              type="date"
              name="startDate"
              value={formatTimestampToDateInput(product.offer?.startDate)}
              onChange={handleOfferChange}
              style={modalStyles.input}
            />
          </div>

          <div style={modalStyles.formGroup}>
            <label style={modalStyles.label}>
              Ngày Kết Thúc <span style={{ color: '#00FF00' }}>(String YYYY-MM-DD)</span>:
            </label>
            <input
              type="date"
              name="endDate"
              value={formatTimestampToDateInput(product.offer?.endDate)}
              onChange={handleOfferChange}
              style={modalStyles.input}
            />
          </div>

          <div style={{ backgroundColor: '#333', padding: '10px', borderRadius: '4px', marginTop: '10px' }}>
            <strong style={{ color: '#00FF00' }}>📋 Dữ liệu sẽ lưu (FIX):</strong>
            <pre style={{ color: '#E0E0E0', fontSize: '12px', marginTop: '5px' }}>
{`{
  offerType: "${product.offer?.offerType || 'Percentage'}" (String),
  offerValue: ${product.offer?.offerValue ? Math.round(parseFloat(product.offer.offerValue)) : 0} (✅ Integer/Long),
  startDate: "${product.offer?.startDate || 'null'}" (String),
  endDate: "${product.offer?.endDate || 'null'}" (String)
}`}
            </pre>
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
            style={{ marginRight: '10px' }}
          />
          <strong>Đánh Dấu Nổi Bật (Is Featured)</strong>
        </label>
      </div>

      <p style={{ color: '#C40000' }}>
        ID Sản Phẩm: <strong>{product.id || 'Chưa lưu'}</strong>
      </p>

      <p>
        Barcode:{' '}
        <code
          style={{
            backgroundColor: '#333',
            padding: '3px 6px',
            borderRadius: '3px',
            color: '#00FF00',
            fontFamily: 'monospace'
          }}
        >
          {product.barcode || 'Sẽ tự động generate khi lưu'}
        </code>
      </p>

      <p>Ngày Tạo: <strong>{product.createdAt ? new Date(Number(product.createdAt)).toLocaleString() : 'N/A'}</strong></p>
      <p>Ngày Cập nhật: <strong>{product.updatedAt ? new Date(Number(product.updatedAt)).toLocaleString() : 'N/A (Sẽ tự động cập nhật)'}</strong></p>
      <p>
        Đánh giá TB: <strong>{(product.averageRating || 0).toFixed(1)}</strong> (Tổng Reviews:{' '}
        <strong>{product.totalReviews || 0}</strong>)
      </p>
    </div>
  );

  const renderContent = () => {
    switch (activeTab) {
      case 'core':
        return renderCoreTab();
      case 'image':
        return renderImageTab();
      case 'offer':
        return renderOfferTab();
      case 'management':
        return renderManagementTab();
      default:
        return null;
    }
  };

  return (
    <div style={modalStyles.overlay}>
      <div style={modalStyles.modalContent}>
        <h2 style={{ borderBottom: '1px solid #555', paddingBottom: '10px', marginBottom: '20px', color: '#C40000' }}>
          {isEditing ? `Sửa Sản Phẩm: ${product.name}` : 'Thêm Sản Phẩm Mới'}
        </h2>

        {product.id ? null : <p style={{ color: '#FF4D4D', fontWeight: 'bold' }}>*** LƯU Ý: Phải LƯU THÔNG TIN CƠ BẢN trước khi tải ảnh! ***</p>}

        <div style={modalStyles.tabBar}>
          <span style={modalStyles.tab(activeTab === 'core')} onClick={() => setActiveTab('core')}>
            Thông tin Cơ bản
          </span>
          <span style={modalStyles.tab(activeTab === 'image')} onClick={() => setActiveTab('image')}>
            Hình ảnh & Media
          </span>
          <span style={modalStyles.tab(activeTab === 'offer')} onClick={() => setActiveTab('offer')}>
            Khuyến mãi
          </span>
          <span style={modalStyles.tab(activeTab === 'management')} onClick={() => setActiveTab('management')}>
            Quản lý & Meta
          </span>
        </div>

        <form onSubmit={handleSave}>
          {renderContent()}

          <div style={{ marginTop: '30px', borderTop: '1px solid #555', paddingTop: '20px' }}>
            <button
              type="submit"
              style={modalStyles.saveButton}
              disabled={saving || isAnyUploading}
            >
              {saving ? 'Đang lưu...' : isAnyUploading ? 'Vui lòng đợi tải ảnh...' : 'Lưu Tất Cả Thay Đổi'}
            </button>
            <button
              type="button"
              style={{ ...modalStyles.saveButton, backgroundColor: '#666' }}
              onClick={onClose}
              disabled={saving || isAnyUploading}
            >
              Hủy / Đóng
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ProductDetailModal;