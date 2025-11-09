package com.example.shopapp;

import java.util.List;
import java.util.Map;

public class Product {
    // Thuộc tính cốt lõi và quản lý
    public String productId;
    public String name;
    public String desc;

    public double basePrice;
    public String mainImage;

    public String category;
    public String type;
    public String status;
    public String barcode; // Barcode/QR code của sản phẩm

    // --- CẤU TRÚC KHUYẾN MÃI MỚI ---
    public boolean isOffer; // Cờ bật/tắt chính
    public OfferDetails offer; // Object chi tiết khuyến mãi

    // --- THUỘC TÍNH QUẢN LÝ THỜI GIAN MỚI ---
    public Long createdAt;
    public Long updatedAt;

    // --- Thuộc tính Phức tạp ---
    public List<ProductVariant> variants;
    public Double averageRating;
    public Long totalReviews;
    public Boolean isFeatured;
    public Map<String, List<String>> colorImages;

    // Constructor rỗng bắt buộc cho Firestore
    public Product() {}

    // Constructor đầy đủ
    public Product(String productId, String name, String desc, double basePrice,
                   String mainImage, String category, String type, String status, boolean isOffer,
                   OfferDetails offer, List<ProductVariant> variants,
                   Double averageRating, Long totalReviews, Boolean isFeatured, Map<String, List<String>> colorImages,
                   Long createdAt, Long updatedAt) {
        this.productId = productId;
        this.name = name;
        this.desc = desc;
        this.basePrice = basePrice;
        this.mainImage = mainImage;
        this.category = category;
        this.type = type;
        this.status = status;
        this.isOffer = isOffer;
        this.offer = offer;
        this.variants = variants;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.isFeatured = isFeatured;
        this.colorImages = colorImages;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // =================================================================
    // GETTERS VÀ SETTERS (ĐÃ SỬA LỖI XUNG ĐỘT CHO isOffer)
    // =================================================================

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    // Logic Khuyến mãi (ĐÃ ĐỔI TÊN GETTER/SETTER ĐỂ KHÔNG XUNG ĐỘT)
    public boolean getIsOfferStatus() { return isOffer; } // <-- ĐÃ SỬA
    public void setIsOfferStatus(boolean offer) { this.isOffer = offer; } // <-- ĐÃ SỬA
    public OfferDetails getOffer() { return offer; }
    public void setOffer(OfferDetails offer) { this.offer = offer; } // <-- Giữ nguyên vì tên trường là 'offer'

    // Quản lý thời gian
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }

    // Thuộc tính phức tạp (Variants, Rating, Images)
    public List<ProductVariant> getVariants() { return variants; }
    public void setVariants(List<ProductVariant> variants) { this.variants = variants; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Long getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Long totalReviews) { this.totalReviews = totalReviews; }

    public Boolean getFeatured() { return isFeatured; }
    public void setFeatured(Boolean featured) { isFeatured = featured; }

    public Map<String, List<String>> getColorImages() { return colorImages; }
    public void setColorImages(Map<String, List<String>> colorImages) { this.colorImages = colorImages; }
}