package com.example.shopapp;

import java.util.Map;

public class User {
    // Thuộc tính cốt lõi và định danh
    public String uid;
    public String fullName;
    public String email;
    public String profileImageUrl;

    // Thuộc tính quản lý
    public String phoneNumber;
    public String role; // (Admin, Customer, Staff)
    public String status; // "pending", "active"
    public boolean isDisabled; // Trạng thái khóa tài khoản (true = locked)

    // Thuộc tính thời gian & cá nhân
    public Long createdAt;
    public Long lastLogin;
    public String gender;

    // Địa chỉ mặc định
    public Map<String, String> defaultAddress;

    public User() {}

    // Constructor đầy đủ đã cập nhật
    public User(String uid, String fullName, String email, String phoneNumber,
                String role, String status, boolean isDisabled, Long createdAt,
                Long lastLogin, String gender, Map<String, String> defaultAddress, String profileImageUrl) {

        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.status = status;
        this.isDisabled = isDisabled;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.gender = gender;
        this.defaultAddress = defaultAddress;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters and Setters

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDisabled() { return isDisabled; }
    public void setDisabled(boolean disabled) { isDisabled = disabled; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getLastLogin() { return lastLogin; }
    public void setLastLogin(Long lastLogin) { this.lastLogin = lastLogin; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Map<String, String> getDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(Map<String, String> defaultAddress) { this.defaultAddress = defaultAddress; }
}
