package com.example.shopapp;

import java.util.List;
import java.util.UUID;

public class Product {
    // Thuộc tính cốt lõi
    public String productId; // ID nội bộ (UUID)
    public String name;
    public String desc;
    public double currentPrice;
    public double originalPrice;
    public String imageUrl;
    public String category;
    public String type;
    public String status;    // Trạng thái (e.g., Active, Draft, Hidden)

    // Thuộc tính Khuyến mãi
    public boolean isOffer;
    public String offerDetails;
    public String extraInfo;

    // --- Thuộc tính Bổ sung/Nâng cao ---
    public List<ProductVariant> variants; // Danh sách các biến thể (Size, Color, Quantity)
    public Double averageRating;          // Đánh giá trung bình (e.g., 4.5)
    public Long totalReviews;             // Tổng số lượt đánh giá
    public Boolean isFeatured;            // Sản phẩm nổi bật (true/false)

    // Constructor rỗng bắt buộc cho Firestore
    public Product() {}

    // Constructor đầy đủ
    public Product(String productId, String name, String desc, double currentPrice, double originalPrice,
                   String imageUrl, String category, String type, String status, boolean isOffer,
                   String offerDetails, String extraInfo, List<ProductVariant> variants,
                   Double averageRating, Long totalReviews, Boolean isFeatured) {
        this.productId = productId;
        this.name = name;
        this.desc = desc;
        this.currentPrice = currentPrice;
        this.originalPrice = originalPrice;
        this.imageUrl = imageUrl;
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
    }
}