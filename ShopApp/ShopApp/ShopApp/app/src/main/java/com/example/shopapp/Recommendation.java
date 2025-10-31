package com.example.shopapp;

// Model đơn giản cho sản phẩm đề xuất
public class Recommendation {
    public String productId; // Đã thêm
    public String name;
    public double price;
    public String imageUrl;
    public String sizeRange;
    public String colorOptions;

    public Recommendation() {}

    public Recommendation(String productId, String name, double price, String imageUrl, String sizeRange, String colorOptions) {
        this.productId = productId; // Đã thêm
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.sizeRange = sizeRange;
        this.colorOptions = colorOptions;
    }
}
