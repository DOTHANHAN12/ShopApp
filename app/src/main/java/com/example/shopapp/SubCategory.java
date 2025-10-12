package com.example.shopapp;

public class SubCategory {
    public String name;
    public String imageUrl; // Dùng để lưu URL ảnh fix cứng

    /** Tên Sub-Category đặc biệt cho tùy chọn Show All */
    public static final String SHOW_ALL_TYPE = "SHOW ALL";
    public static final String SHOW_ALL_IMAGE_URL = "https://i.ibb.co/6y45s1x/showall.png"; // URL icon Show All

    public SubCategory(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }
}