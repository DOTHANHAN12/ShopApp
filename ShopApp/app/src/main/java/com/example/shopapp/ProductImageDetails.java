package com.example.shopapp;

import java.util.List;

/**
 * Class này chứa các URL ảnh được phân loại theo mục đích sử dụng.
 */
public class ProductImageDetails {
    // Ảnh Chính: Dùng cho ViewPager sản phẩm nổi bật
    public String mainImage;

    // Ảnh Phụ: Dùng cho màn hình chi tiết sản phẩm (nhiều ảnh)
    public List<String> secondaryImages;

    // Ảnh Sub-Category Chung: Dùng làm icon cho danh mục con trong màn hình Search
    public String subCategoryImage;

    // Constructor rỗng bắt buộc cho Firestore
    public ProductImageDetails() {}

    public ProductImageDetails(String mainImage, List<String> secondaryImages, String subCategoryImage) {
        this.mainImage = mainImage;
        this.secondaryImages = secondaryImages;
        this.subCategoryImage = subCategoryImage;
    }

    // Bạn nên thêm Getters để truy cập dữ liệu an toàn (Mặc dù Firestore có thể dùng public fields)
    public String getMainImage() {
        return mainImage;
    }
    public String getSubCategoryImage() {
        return subCategoryImage;
    }
    public List<String> getSecondaryImages() {
        return secondaryImages;
    }
}