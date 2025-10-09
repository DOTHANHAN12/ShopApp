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
}