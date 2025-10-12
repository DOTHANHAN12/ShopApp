package com.example.shopapp;

import java.util.List;
import java.util.UUID;

public class Product {
    // Thuộc tính cốt lõi
    public String productId;
    public String name;
    public String desc;
    public double currentPrice;
    public double originalPrice;

    // THAY THẾ TRƯỜNG imageUrl bằng đối tượng images
    public ProductImageDetails images;

    public String category;
    public String type;
    public String status;

    // Thuộc tính Khuyến mãi
    public boolean isOffer;
    public String offerDetails;
    public String extraInfo;

    // Thuộc tính Bổ sung/Nâng cao
    public List<ProductVariant> variants;
    public Double averageRating;
    public Long totalReviews;
    public Boolean isFeatured;

    // Constructor rỗng bắt buộc cho Firestore
    public Product() {}

    // Constructor đầy đủ (Đã thay thế imageUrl bằng ProductImageDetails images)
    public Product(String productId, String name, String desc, double currentPrice, double originalPrice,
                   ProductImageDetails images, String category, String type, String status, boolean isOffer,
                   String offerDetails, String extraInfo, List<ProductVariant> variants,
                   Double averageRating, Long totalReviews, Boolean isFeatured) {
        this.productId = productId;
        this.name = name;
        this.desc = desc;
        this.currentPrice = currentPrice;
        this.originalPrice = originalPrice;
        this.images = images; // <--- ĐÃ THAY ĐỔI
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