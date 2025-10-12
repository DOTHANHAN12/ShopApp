package com.example.shopapp;

public class SubCategory {
    public String name;
    public int iconResId; // Giữ lại cho tương thích, nhưng giá trị sẽ là -1
    public String imageUrl; // <-- TRƯỜNG MỚI ĐỂ LƯU URL ẢNH ICON

    // Constructor cũ
    public SubCategory(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    // Bạn có thể thêm một constructor nếu muốn khởi tạo imageUrl ngay
    // public SubCategory(String name, String imageUrl) { ... }
}