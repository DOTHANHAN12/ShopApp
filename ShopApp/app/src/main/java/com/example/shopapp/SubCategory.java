package com.example.shopapp;

public class SubCategory {
    public String name;
    public String imageUrl;

    public static final String SHOW_ALL_TYPE = "SHOW ALL";
    public static final String SHOW_ALL_IMAGE_URL = "URL_SHOW_ALL_ICON"; // Thay bằng URL icon

    public SubCategory(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }
    // Getters/Setters (cần thiết cho Deserialization của Firestore, mặc dù không dùng ở đây)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    // ...
}