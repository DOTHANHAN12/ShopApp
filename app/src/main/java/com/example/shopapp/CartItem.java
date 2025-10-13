package com.example.shopapp;

// Model cho từng item trong giỏ hàng
public class CartItem {
    public String productId;
    public String variantId;
    public int quantity;
    public double priceAtTimeOfAdd;
    public Long addedAt; // NEW

    public CartItem() {}

    public CartItem(String productId, String variantId, int quantity, double priceAtTimeOfAdd, Long addedAt) {
        this.productId = productId;
        this.variantId = variantId;
        this.quantity = quantity;
        this.priceAtTimeOfAdd = priceAtTimeOfAdd;
        this.addedAt = addedAt;
    }

    // GETTERS VÀ SETTERS
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) { this.variantId = variantId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPriceAtTimeOfAdd() { return priceAtTimeOfAdd; }
    public void setPriceAtTimeOfAdd(double priceAtTimeOfAdd) { this.priceAtTimeOfAdd = priceAtTimeOfAdd; }

    public Long getAddedAt() { return addedAt; }
    public void setAddedAt(Long addedAt) { this.addedAt = addedAt; }
}