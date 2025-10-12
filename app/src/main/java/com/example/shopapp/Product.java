package com.example.shopapp;

import java.util.List;
import java.util.Map; // Cần thiết cho Map

public class Product {
    // Thuộc tính cốt lõi
    public String productId;
    public String name;
    public String desc;
    public double currentPrice;
    public double originalPrice;

    // ĐÃ XÓA: public String imageUrl; (cũ)
    // ĐÃ THÊM: mainImage (Ảnh chính)
    public String mainImage;

    public String category;
    public String type;
    public String status;

    // Thuộc tính Khuyến mãi
    public boolean isOffer;
    public String offerDetails;
    public String extraInfo;

    // --- Thuộc tính Bổ sung/Nâng cao ---
    public List<ProductVariant> variants;
    public Double averageRating;
    public Long totalReviews;
    public Boolean isFeatured;

    // TRƯỜNG MỚI: Cấu trúc lưu trữ 5 ảnh chi tiết theo từng màu
    // Key: Tên/Mã màu (ví dụ: "Black")
    // Value: List<String> chứa 5 tên file/URL ảnh cho màu đó
    public Map<String, List<String>> colorImages;

    // Constructor rỗng bắt buộc cho Firestore
    public Product() {}

    // Constructor đầy đủ (Đã điều chỉnh để khớp với logic tạo dữ liệu)
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
}