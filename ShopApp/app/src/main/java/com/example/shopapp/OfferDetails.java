package com.example.shopapp;

// Model chi tiết cho thông tin khuyến mãi (Offer Details)
public class OfferDetails {
    // Thuộc tính
    public Long offerValue; // % giảm giá (vd: 20)
    public String startDate;       // Thời gian bắt đầu (String YYYY-MM-DD)
    public String endDate;         // Thời gian kết thúc (String YYYY-MM-DD)

    // Constructor rỗng bắt buộc cho Firestore
    public OfferDetails() {}

    // Constructor đầy đủ
    public OfferDetails(Long offerValue, String startDate, String endDate) {
        this.offerValue = offerValue;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // =================================================================
    // GETTERS VÀ SETTERS (BẮT BUỘC ĐỂ ĐẢM BẢO FIREBASE DESERIALIZATION)
    // =================================================================

    public Long getOfferValue() {
        return offerValue;
    }
    public void setOfferValue(Long offerValue) {
        this.offerValue = offerValue;
    }

    public String getStartDate() {
        return startDate;
    }
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}