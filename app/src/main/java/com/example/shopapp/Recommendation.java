package com.example.shopapp;

// Model đơn giản cho sản phẩm đề xuất
public class Recommendation {
    public String name;
    public double price;
    public String imageUrl;
    public String sizeRange;
    public String colorOptions;

    public Recommendation() {}

    public Recommendation(String name, double price, String imageUrl, String sizeRange, String colorOptions) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.sizeRange = sizeRange;
        this.colorOptions = colorOptions;
    }
}