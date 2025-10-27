package com.example.shopapp;

import java.util.Map;

public class User {
    // Thuộc tính cốt lõi
    public String fullName;
    public String email;
    public String profileImageUrl; // NEW

    public Long createdAt;
    public String gender; // NEW: Thêm trường gender

    // Địa chỉ mặc định (Sử dụng Map để lưu Map trong Firestore)

    public User() {}

    // Constructor đầy đủ đã cập nhật
    public User(String fullName, String email, String phoneNumber, Long createdAt, String gender, Map<String, String> defaultAddress, String profileImageUrl) {
        this.fullName = fullName;
        this.email = email;
        this.createdAt = createdAt;
        this.gender = gender;
        this.profileImageUrl = profileImageUrl; // NEW
    }

    // GETTERS VÀ SETTERS
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProfileImageUrl() { return profileImageUrl; } // NEW
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; } // NEW

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; } // NEW SETTER


}