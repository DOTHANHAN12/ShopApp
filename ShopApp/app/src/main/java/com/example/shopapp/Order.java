package com.example.shopapp;

import java.util.List;
import java.util.Date;
import java.util.Map;

public class Order {
    public String orderId;
    public String userId;
    public double totalAmount;
    public double subtotal;
    public double discountAmount;
    public String voucherCode;
    public String paymentMethod;
    public String orderStatus;

    public Map<String, String> shippingAddress;
    public List<Map<String, Object>> items;
    public Long createdAt;

    public Order() {}

    public Order(String userId, double totalAmount, double subtotal, double discountAmount, String voucherCode, Map<String, String> shippingAddress, List<Map<String, Object>> items) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.voucherCode = voucherCode;
        this.shippingAddress = shippingAddress;
        this.items = items;
        this.createdAt = new Date().getTime();
        this.orderStatus = "PENDING";
    }

    // GETTERS VÀ SETTERS
    public String getOrderId() { return orderId; }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getStatus() { // Đổi tên từ getOrderStatus
        return orderStatus;
    }

    public void setStatus(String orderStatus) { // Đổi tên từ setOrderStatus
        this.orderStatus = orderStatus;
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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Date getOrderDate() { // Phương thức mới
        return createdAt != null ? new Date(createdAt) : null;
    }

    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
}