import React, { useState } from 'react';
import { collection, getDocs, writeBatch, doc } from 'firebase/firestore';
import { db } from '../firebaseConfig';

// ===== UNSPLASH PREMIUM FASHION IMAGES (No 403 errors!) =====
const uniqloImages = {
  "jeans": [
    "https://images.unsplash.com/photo-1542272604-787c62d465d1?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1543163521-9efcc06814d7?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1516991891340-532eb60a2a5c?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1506629082632-33ccb2fb2671?w=800&h=800&fit=crop",
  ],
  "shirt": [
    "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1552664730-d307ca884978?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1598033129519-3dd4a1e42b84?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1559056199-641a0ac8b3f4?w=800&h=800&fit=crop",
  ],
  "tshirt": [
    "https://images.unsplash.com/photo-1521572215127-d8422ad63d65?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1529720317453-c8da503f2051?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1542272604-787c62d465d1?w=800&h=800&fit=crop",
  ],
  "sweater": [
    "https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1529720317453-c8da503f2051?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1556821552-5ff63b1b6bcc?w=800&h=800&fit=crop",
  ],
  "polo": [
    "https://images.unsplash.com/photo-1516992654410-c116b78e6541?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1598033129519-3dd4a1e42b84?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1552664730-d307ca884978?w=800&h=800&fit=crop",
  ],
  "pants": [
    "https://images.unsplash.com/photo-1542272604-787c62d465d1?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1506629082632-33ccb2fb2671?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1543163521-9efcc06814d7?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1516991891340-532eb60a2a5c?w=800&h=800&fit=crop",
  ],
  "accessories": [
    "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&h=800&fit=crop",
    "https://images.unsplash.com/photo-1523170335258-f5ed11844a49?w=800&h=800&fit=crop",
  ]
};

const getImageByProductName = (productName, category) => {
  const nameLower = productName.toLowerCase();
  const catLower = category.toLowerCase();

  if (nameLower.includes("jeans")) return uniqloImages.jeans[Math.floor(Math.random() * uniqloImages.jeans.length)];
  if (nameLower.includes("flannel") || nameLower.includes("shirt")) return uniqloImages.shirt[Math.floor(Math.random() * uniqloImages.shirt.length)];
  if (nameLower.includes("airism") || nameLower.includes("t-shirt")) return uniqloImages.tshirt[Math.floor(Math.random() * uniqloImages.tshirt.length)];
  if (nameLower.includes("cashmere") || nameLower.includes("sweater")) return uniqloImages.sweater[Math.floor(Math.random() * uniqloImages.sweater.length)];
  if (nameLower.includes("polo")) return uniqloImages.polo[Math.floor(Math.random() * uniqloImages.polo.length)];
  if (nameLower.includes("jogger") || nameLower.includes("pants")) return uniqloImages.pants[Math.floor(Math.random() * uniqloImages.pants.length)];
  
  if (catLower.includes("accessories")) return uniqloImages.accessories[Math.floor(Math.random() * uniqloImages.accessories.length)];
  if (catLower.includes("bottoms")) return uniqloImages.pants[Math.floor(Math.random() * uniqloImages.pants.length)];
  if (catLower.includes("dresses")) return uniqloImages.shirt[Math.floor(Math.random() * uniqloImages.shirt.length)];
  
  const allImages = Object.values(uniqloImages).flat();
  return allImages[Math.floor(Math.random() * allImages.length)];
};

const BatchUploadUniqloImages = ({ onComplete }) => {
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState('');
  const [stats, setStats] = useState({ total: 0, updated: 0, skipped: 0 });
  const [showDetails, setShowDetails] = useState(false);

  const handleBatchUpload = async () => {
    if (!window.confirm("🚀 Bạn có chắc muốn NHÉT ÃNH CHO TẤT CẢ SẢN PHẨM không?\n\nCửa hàng sẽ đẹp lắm đó! 😎")) {
      return;
    }

    setLoading(true);
    setProgress("🔄 Đang tải danh sách sản phẩm...");
    setStats({ total: 0, updated: 0, skipped: 0 });
    setShowDetails(true);

    try {
      const productsRef = collection(db, "products");
      const snapshot = await getDocs(productsRef);

      if (snapshot.empty) {
        alert("❌ Không tìm thấy sản phẩm nào!");
        setLoading(false);
        return;
      }

      setStats(prev => ({ ...prev, total: snapshot.size }));
      setProgress(`✅ Tìm thấy ${snapshot.size} sản phẩm. Đang cấp ảnh...\n`);

      let batch = writeBatch(db);
      let count = 0;
      let skipped = 0;
      const BATCH_SIZE = 500;

      snapshot.docs.forEach((docSnap) => {
        const productData = docSnap.data();
        const productName = productData.name || "";
        const category = productData.category || "";

        // Skip nếu đã có mainImage
        if (productData.mainImage && productData.mainImage.includes("unsplash")) {
          skipped++;
          setProgress(prev => `${prev}⏭️  ${productName}\n`);
          return;
        }

        const imageUrl = getImageByProductName(productName, category);
        const docRef = doc(db, "products", docSnap.id);
        batch.update(docRef, {
          mainImage: imageUrl,
          updatedAt: Date.now()
        });

        setProgress(prev => `${prev}✅ ${productName} → ${imageUrl.substring(0, 40)}...\n`);
        count++;

        if (count % BATCH_SIZE === 0) {
          batch.commit();
          batch = writeBatch(db);
          setProgress(prev => `${prev}📦 Đã commit ${count} updates...\n`);
        }
      });

      // Final commit
      if (count % BATCH_SIZE !== 0) {
        await batch.commit();
      }

      setStats({ total: snapshot.size, updated: count, skipped });
      setProgress(prev => `${prev}\n✅ ✅ ✅ XONG! Đã cập nhật ${count} sản phẩm, skip ${skipped} cái`);

      setTimeout(() => {
        alert(`🎉 THÀNH CÔNG!\n\n✅ Cập nhật: ${count}\n⏭️  Skip: ${skipped}\n\nCửa hàng bây giờ đẹp trai lắm rồi! 🚀`);
        onComplete?.();
      }, 1000);

    } catch (error) {
      console.error("❌ LỖI:", error);
      alert(`❌ LỖI: ${error.message}`);
      setProgress(`❌ LỖI: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      backgroundColor: '#292929',
      padding: '20px',
      borderRadius: '8px',
      border: '2px solid #C40000',
      marginBottom: '20px'
    }}>
      <h3 style={{ color: '#C40000', marginBottom: '15px' }}>🖼️  BATCH UPLOAD IMAGES</h3>
      <p style={{ color: '#888', fontSize: '12px', marginBottom: '15px' }}>
        ✅ Dùng Unsplash images - không lỗi 403, load nhanh, chất lượng cao
      </p>

      <button
        onClick={handleBatchUpload}
        disabled={loading}
        style={{
          backgroundColor: '#C40000',
          color: 'white',
          padding: '12px 20px',
          border: 'none',
          borderRadius: '4px',
          cursor: loading ? 'not-allowed' : 'pointer',
          fontWeight: 'bold',
          fontSize: '16px',
          opacity: loading ? 0.6 : 1,
          transition: 'all 0.2s'
        }}
      >
        {loading ? '⏳ Đang xử lý...' : '🚀 NHÉT ÃNH VÀO DB NGAY'}
      </button>

      {progress && showDetails && (
        <div style={{
          marginTop: '15px',
          padding: '12px',
          backgroundColor: '#333',
          borderRadius: '4px',
          color: '#E0E0E0',
          fontFamily: 'monospace',
          fontSize: '11px',
          maxHeight: '300px',
          overflowY: 'auto',
          whiteSpace: 'pre-wrap'
        }}>
          {progress}
        </div>
      )}

      {stats.total > 0 && (
        <div style={{
          marginTop: '15px',
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '10px'
        }}>
          <div style={{ backgroundColor: '#1a3a1a', padding: '10px', borderRadius: '4px', textAlign: 'center' }}>
            <div style={{ color: '#00FF00', fontSize: '18px', fontWeight: 'bold' }}>{stats.total}</div>
            <div style={{ color: '#888', fontSize: '12px' }}>Tổng sản phẩm</div>
          </div>
          <div style={{ backgroundColor: '#1a2a3a', padding: '10px', borderRadius: '4px', textAlign: 'center' }}>
            <div style={{ color: '#00AAFF', fontSize: '18px', fontWeight: 'bold' }}>{stats.updated}</div>
            <div style={{ color: '#888', fontSize: '12px' }}>Đã cập nhật</div>
          </div>
          <div style={{ backgroundColor: '#3a2a1a', padding: '10px', borderRadius: '4px', textAlign: 'center' }}>
            <div style={{ color: '#FFAA00', fontSize: '18px', fontWeight: 'bold' }}>{stats.skipped}</div>
            <div style={{ color: '#888', fontSize: '12px' }}>Bỏ qua</div>
          </div>
        </div>
      )}
    </div>
  );
};

export default BatchUploadUniqloImages;