package com.example.shopapp;

// Model chi tiết cho thông tin khuyến mãi (Offer Details)
public class OfferDetails {
    // Thuộc tính
    public Long offerValue;
    public String offerType;
    public Long startDate;
    public Long endDate;

    // Constructor rỗng bắt buộc cho Firestore
    public OfferDetails() {}

    // Constructor đầy đủ
    public OfferDetails(Long offerValue, String offerType, Long startDate, Long endDate) {
        this.offerValue = offerValue;
        this.offerType = offerType;
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

    public String getOfferType() {
        return offerType;
    }
    public void setOfferType(String offerType) {
        this.offerType = offerType;
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
