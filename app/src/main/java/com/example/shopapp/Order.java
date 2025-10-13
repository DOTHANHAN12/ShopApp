package com.example.shopapp;

import java.util.List;
import java.util.Map;

// Model cho đơn hàng chính
public class Order {
    public String orderId;
    public String userId;
    public double totalAmount;
    public String status; // Pending, Shipped, Delivered, etc.
    public Long createdAt;

    // Địa chỉ giao hàng (lưu trữ giá trị tĩnh tại thời điểm đặt hàng)
    public Map<String, String> shippingAddress;

    // Danh sách các mặt hàng trong đơn hàng
    public List<Map<String, Object>> items; // Sử dụng Map<String, Object> để linh hoạt

    public Order() {}

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, String> getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Map<String, String> shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public void setItems(List<Map<String, Object>> items) {
        this.items = items;
    }

    // Constructor đơn giản
    public Order(String orderId, String userId, double totalAmount, String status, Long createdAt, Map<String, String> shippingAddress, List<Map<String, Object>> items) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.shippingAddress = shippingAddress;
        this.items = items;
    }

    // GETTERS VÀ SETTERS (Một phần)
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // ... Thêm các Getters/Setters còn lại ...
}