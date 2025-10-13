package com.example.shopapp;

// Model chi tiết cho thông tin khuyến mãi (Offer Details)
public class OfferDetails {
    // Thuộc tính
    public Long discountPercent; // % giảm giá (vd: 20)
    public Long startDate;       // Thời gian bắt đầu (Timestamp/Long)
    public Long endDate;         // Thời gian kết thúc (Timestamp/Long)

    // Constructor rỗng bắt buộc cho Firestore
    public OfferDetails() {}

    // Constructor đầy đủ
    public OfferDetails(Long discountPercent, Long startDate, Long endDate) {
        this.discountPercent = discountPercent;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // =================================================================
    // GETTERS VÀ SETTERS (BẮT BUỘC ĐỂ ĐẢM BẢO FIREBASE DESERIALIZATION)
    // =================================================================

    public Long getDiscountPercent() {
        return discountPercent;
    }
    public void setDiscountPercent(Long discountPercent) {
        this.discountPercent = discountPercent;
    }

    public Long getStartDate() {
        return startDate;
    }
    public void setStartDate(Long startDate) {
        this.startDate = startDate;
    }

    public Long getEndDate() {
        return endDate;
    }
    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }
}