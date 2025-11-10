package com.example.shopapp;

// Model cho sản phẩm đề xuất, đã được mở rộng để hỗ trợ hiển thị giá khuyến mãi
public class Recommendation {
    public String productId;
    public String name;
    public String imageUrl;
    public String sizeRange;
    public String colorOptions;

    // ✅ MỞ RỘNG: Thêm các trường giá để hiển thị khuyến mãi
    public double displayPrice; // Giá cuối cùng để hiển thị (có thể đã giảm)
    public double originalPrice; // Giá gốc (chỉ hiển thị khi có khuyến mãi)
    public boolean hasOffer; // Cờ để xác định có khuyến mãi hay không

    // Constructor rỗng bắt buộc cho Firestore/Deserialization
    public Recommendation() {}

    // Constructor đầy đủ
    public Recommendation(String productId, String name, String imageUrl, String sizeRange, String colorOptions,
                          double displayPrice, double originalPrice, boolean hasOffer) {
        this.productId = productId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.sizeRange = sizeRange;
        this.colorOptions = colorOptions;
        this.displayPrice = displayPrice;
        this.originalPrice = originalPrice;
        this.hasOffer = hasOffer;
    }
}
