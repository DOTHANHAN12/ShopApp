package com.example.shopapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RatingBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays; // Dùng để giả lập dữ liệu đề xuất
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ProductDetailActivity extends AppCompatActivity implements ColorSelectorAdapter.OnColorSelectedListener {

    private static final String TAG = "ProductDetailAct";
    private FirebaseFirestore db;

    // UI Elements
    private ViewPager2 viewPager;
    private TextView textImageIndicator, textProductName, textPrice, textReviewCount, textColorName;
    private RatingBar ratingBarDetail;
    private Button btnAddToCart;
    private RecyclerView recyclerColors, recyclerSizes, recyclerRecommendations;
    private ImageView imgBack;

    // Data
    private Product currentProduct;
    private String currentSelectedColor;
    private List<String> currentImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = FirebaseFirestore.getInstance();

        // 1. Ánh xạ Views
        mapViews();

        // 2. Lấy Product ID từ Intent
        String productId = getIntent().getStringExtra("PRODUCT_ID");

        if (productId != null) {
            loadProductDetails(productId);
        } else {
            Toast.makeText(this, "Không tìm thấy ID sản phẩm.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 3. Xử lý nút Back
        imgBack.setOnClickListener(v -> finish());
    }

    private void mapViews() {
        viewPager = findViewById(R.id.view_pager_product_images);
        textImageIndicator = findViewById(R.id.text_image_indicator);
        textProductName = findViewById(R.id.text_product_name_detail);
        textPrice = findViewById(R.id.text_price);
        textReviewCount = findViewById(R.id.text_review_count);
        textColorName = findViewById(R.id.text_color_name);
        ratingBarDetail = findViewById(R.id.rating_bar_detail);

        recyclerColors = findViewById(R.id.recycler_color_selector);
        recyclerSizes = findViewById(R.id.recycler_size_selector);
        recyclerRecommendations = findViewById(R.id.recycler_frequently_bought);
        imgBack = findViewById(R.id.img_back);
        btnAddToCart = findViewById(R.id.btn_add_to_cart);
    }

    private void loadProductDetails(String productId) {
        // Giả định Product ID được dùng làm Doc ID trong Firestore
        db.collection("products")
                .document(productId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        currentProduct = document.toObject(Product.class);
                        if (currentProduct != null && currentProduct.colorImages != null) {
                            displayProductData(currentProduct);
                        } else {
                            Log.e(TAG, "Dữ liệu sản phẩm rỗng hoặc thiếu trường colorImages.");
                            Toast.makeText(this, "Thiếu dữ liệu ảnh chi tiết.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi tải chi tiết sản phẩm.", task.getException());
                        Toast.makeText(this, "Không thể tải chi tiết sản phẩm.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void displayProductData(Product product) {
        // 1. Gán dữ liệu cơ bản
        textProductName.setText(product.name);
        textPrice.setText(String.format(Locale.getDefault(), "%,.0f VND", product.currentPrice));

        // Gán Review và Rating
        if (product.totalReviews != null && product.averageRating != null) {
            textReviewCount.setText(String.format("(%d)", product.totalReviews));
            ratingBarDetail.setRating(product.averageRating.floatValue());
        }

        // 2. Setup Color Selector
        List<String> colorNames = new ArrayList<>(product.colorImages.keySet());
        if (!colorNames.isEmpty()) {
            currentSelectedColor = colorNames.get(0); // Chọn màu đầu tiên làm mặc định

            recyclerColors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerColors.setAdapter(new ColorSelectorAdapter(colorNames, this));

            // 3. Load ảnh và size cho màu mặc định
            loadImagesAndSizesForColor(currentSelectedColor);
        }

        // 4. Setup Recommendations
        setupRecommendation(product.category);
    }

    // Xử lý khi màu được chọn
    @Override
    public void onColorSelected(String colorName) {
        if (!colorName.equals(currentSelectedColor)) {
            currentSelectedColor = colorName;
            loadImagesAndSizesForColor(currentSelectedColor);
        }
    }

    private void loadImagesAndSizesForColor(String colorName) {
        // Cập nhật tên màu hiển thị
        textColorName.setText("Color: " + colorName.toUpperCase());

        // 1. Tải và hiển thị ảnh (Sử dụng Map colorImages)
        List<String> images = currentProduct.colorImages.getOrDefault(colorName, new ArrayList<>());
        currentImages = images;

        // Cần Adapter mới: ProductImageSliderAdapter
        viewPager.setAdapter(new ProductImageSliderAdapter(images, currentProduct));

        // Cập nhật chỉ báo ảnh
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                textImageIndicator.setText(String.format(Locale.getDefault(), "%d | %d", position + 1, images.size()));
            }
        });
        // Cập nhật chỉ báo lần đầu
        textImageIndicator.setText(String.format(Locale.getDefault(), "%d | %d", 1, images.size()));


        // 2. Lọc và hiển thị Sizes (Variants) cho màu này
        if (currentProduct.variants != null) {
            List<ProductVariant> variantsForColor = currentProduct.variants.stream()
                    .filter(v -> v.color.equalsIgnoreCase(colorName))
                    .collect(Collectors.toList());

            // Cần Adapter mới: SizeSelectorAdapter
            recyclerSizes.setAdapter(new SizeSelectorAdapter(variantsForColor));
        } else {
            recyclerSizes.setAdapter(new SizeSelectorAdapter(new ArrayList<>()));
        }
    }

    // Logic giả lập/Placeholder cho Recommendations
    private void setupRecommendation(String category) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Giả lập 2 sản phẩm đề xuất như mẫu
        recommendations.add(new Recommendation(
                "AirSense Pants | Wool Like", 784000.0, "URL_PANTS_REC", "MEN, 70CM-95CM", "Gray/Navy"));
        recommendations.add(new Recommendation(
                "Super Non Iron Slim Fit Shirt", 784000.0, "URL_SHIRT_REC", "MEN, XS-XXL", "White/Blue"));

        // Cần Adapter mới: RecommendationAdapter
        recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerRecommendations.setAdapter(new RecommendationAdapter(recommendations));
    }
}