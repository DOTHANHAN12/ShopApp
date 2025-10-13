package com.example.shopapp;

// Model cho từng sản phẩm yêu thích (Document ID là productId)
public class FavoriteItem {

    // Trường này lưu thời điểm thêm vào danh sách yêu thích
    public Long addedAt;

    // Constructor rỗng bắt buộc cho Firestore
    public FavoriteItem() {}

    // Constructor đầy đủ
    public FavoriteItem(Long addedAt) {
        this.addedAt = addedAt;
    }

    // GETTERS VÀ SETTERS
    public Long getAddedAt() { return addedAt; }
    public void setAddedAt(Long addedAt) { this.addedAt = addedAt; }
}