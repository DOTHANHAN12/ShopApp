package com.example.shopapp;

import java.util.Map;

public class User {
    // Thuộc tính cốt lõi
    public String fullName;
    public String email;

    public Long createdAt;
    public String gender; // NEW: Thêm trường gender

    // Địa chỉ mặc định (Sử dụng Map để lưu Map trong Firestore)

    public User() {}

    // Constructor đầy đủ đã cập nhật
    public User(String fullName, String email, String phoneNumber, Long createdAt, String gender, Map<String, String> defaultAddress) {
        this.fullName = fullName;
        this.email = email;
        this.createdAt = createdAt;
        this.gender = gender;
    }

    // GETTERS VÀ SETTERS
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }



    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; } // NEW SETTER


}