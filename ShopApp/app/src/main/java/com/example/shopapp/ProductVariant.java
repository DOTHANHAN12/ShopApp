package com.example.shopapp;

// Class này đại diện cho một biến thể cụ thể của sản phẩm
public class ProductVariant {
    public String variantId; // ID duy nhất cho biến thể (e.g., SKU của Size L/Color Black)
    public String size;      // Ví dụ: "S", "M", "L", "XL"
    public String color;     // Ví dụ: "Black", "Navy"
    public Long quantity;    // Tồn kho RIÊNG cho biến thể này
    public double price;     // (Tùy chọn) Giá RIÊNG cho biến thể này (nếu khác giá gốc)

    // Constructor rỗng bắt buộc cho Firestore
    public ProductVariant() {}

    // Constructor đầy đủ
    public ProductVariant(String variantId, String size, String color, Long quantity, double price) {
        this.variantId = variantId;
        this.size = size;
        this.color = color;
        this.quantity = quantity;
        this.price = price;
    }

    // *** KHẮC PHỤC LỖI: Cannot resolve method 'getVariantId' ***
    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) { this.variantId = variantId; }

    // Thêm các getter/setter còn thiếu nếu cần thiết (dựa trên các file trước):
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Long getQuantity() { return quantity; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}