package com.example.shopapp;

import java.util.List;
import java.util.Map;

public class Product {
    // Thuộc tính cốt lõi
    public String productId;
    public String name;
    public String desc;
    public double currentPrice;
    public double originalPrice;

    public String mainImage;

    public String category;
    public String type;
    public String status;

    // Thuộc tính Khuyến mãi
    public boolean isOffer;
    public String offerDetails;
    public String extraInfo;

    // --- Thuộc tính Lồng ghép/Phức tạp ---
    public List<ProductVariant> variants;
    public Double averageRating;
    public Long totalReviews;
    public Boolean isFeatured;
    public Map<String, List<String>> colorImages;

    // Constructor rỗng bắt buộc cho Firestore (QUAN TRỌNG)
    public Product() {}

    // Constructor đầy đủ (Đã cập nhật)
    public Product(String productId, String name, String desc, double currentPrice, double originalPrice,
                   String mainImage, String category, String type, String status, boolean isOffer,
                   String offerDetails, String extraInfo, List<ProductVariant> variants,
                   Double averageRating, Long totalReviews, Boolean isFeatured, Map<String, List<String>> colorImages) {
        this.productId = productId;
        this.name = name;
        this.desc = desc;
        this.currentPrice = currentPrice;
        this.originalPrice = originalPrice;
        this.mainImage = mainImage;
        this.category = category;
        this.type = type;
        this.status = status;
        this.isOffer = isOffer;
        this.offerDetails = offerDetails;
        this.extraInfo = extraInfo;
        this.variants = variants;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.isFeatured = isFeatured;
        this.colorImages = colorImages;
    }

    // =================================================================
    // GETTERS VÀ SETTERS (BẮT BUỘC ĐỂ ĐẢM BẢO FIREBASE DESERIALIZATION)
    // =================================================================

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isOffer() { return isOffer; }
    public void setOffer(boolean offer) { isOffer = offer; }

    public String getOfferDetails() { return offerDetails; }
    public void setOfferDetails(String offerDetails) { this.offerDetails = offerDetails; }

    public String getExtraInfo() { return extraInfo; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }

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