package com.example.shopapp;

import java.util.List;
import java.util.Date;
import java.util.Map;

// Model cho đơn hàng chính
public class Order {
    public String orderId;
    public String userId;
    public double totalAmount;
    public double subtotal;             // Tổng phụ trước giảm giá
    public double discountAmount;       // Giá trị giảm giá từ voucher
    public String voucherCode;          // Mã voucher đã áp dụng
    public String paymentMethod;        // VD: "MOMO", "COD"
    public String orderStatus;          // VD: "PENDING", "PROCESSING", "COMPLETED"

    // Lưu trữ địa chỉ dưới dạng Map để dễ dàng truy vấn/hiển thị
    public Map<String, String> shippingAddress;

    // Danh sách các mặt hàng (lưu trữ dưới dạng list các đối tượng CartItem hoặc Map)
    public List<Map<String, Object>> items;

    public Long createdAt;

    public Order() {}

    // Constructor đơn giản dùng khi Checkout
    public Order(String userId, double totalAmount, double subtotal, double discountAmount, String voucherCode, Map<String, String> shippingAddress, List<Map<String, Object>> items) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.voucherCode = voucherCode;
        this.shippingAddress = shippingAddress;
        this.items = items;
        this.createdAt = new Date().getTime();
        this.orderStatus = "PENDING"; // Trạng thái ban đầu
    }

    // GETTERS VÀ SETTERS (Đầy đủ)
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Map<String, String> getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Map<String, String> shippingAddress) { this.shippingAddress = shippingAddress; }

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
}