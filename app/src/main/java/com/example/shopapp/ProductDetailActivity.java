package com.example.shopapp;

import android.content.Intent;
import android.graphics.Color; // Import Color
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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ProductDetailActivity extends AppCompatActivity implements
        ColorSelectorAdapter.OnColorSelectedListener,
        SizeSelectorAdapter.OnSizeSelectedListener
{

    private static final String TAG = "ProductDetailAct";
    private FirebaseFirestore db;

    // UI Elements
    private ViewPager2 viewPager;
    private TextView textImageIndicator, textProductName, textPrice, textReviewCount, textColorName, textSizeLabel, textStockStatus, textSelectedQuantity, textInventoryCount;
    private RatingBar ratingBarDetail;
    private Button btnAddToCart;
    private RecyclerView recyclerColors, recyclerSizes, recyclerRecommendations;
    private ImageView imgBack;
    private ImageView navHomeDetail;

    // Nút Cộng/Trừ
    private TextView btnIncrementQty; // Nút +
    private TextView btnDecrementQty; // Nút -

    // Data
    private Product currentProduct;
    private String currentSelectedColor;
    private List<String> currentImages;
    private ProductVariant currentSelectedVariant;

    private int currentQuantityToBuy = 1; // SỐ LƯỢNG MUA HIỆN TẠI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SETUP UI CƠ BẢN VÀ FIX STATUS BAR
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        // THIẾT LẬP Status Bar icons sang màu đen (LIGHT_STATUS_BAR)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_product_detail);

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
        textSizeLabel = findViewById(R.id.text_size_label);
        textStockStatus = findViewById(R.id.text_stock_status);
        textSelectedQuantity = findViewById(R.id.text_selected_quantity);
        textInventoryCount = findViewById(R.id.text_inventory_count);

        ratingBarDetail = findViewById(R.id.rating_bar_detail);

        recyclerColors = findViewById(R.id.recycler_color_selector);
        recyclerSizes = findViewById(R.id.recycler_size_selector);
        recyclerRecommendations = findViewById(R.id.recycler_frequently_bought);
        imgBack = findViewById(R.id.img_back);
        btnAddToCart = findViewById(R.id.btn_add_to_cart);

        // Ánh xạ nút Cộng/Trừ
        btnIncrementQty = findViewById(R.id.btn_increment_qty);
        btnDecrementQty = findViewById(R.id.btn_decrement_qty);

        // Ánh xạ nút Home
        navHomeDetail = findViewById(R.id.nav_home_cs); // <--- Đã sửa ID Home
        if (navHomeDetail != null) {
            navHomeDetail.setOnClickListener(v -> {
                Intent intent = new Intent(ProductDetailActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Thiết lập Listener cho Cộng/Trừ
        setupQuantityButtons();
    }

    private void setupQuantityButtons() {
        btnIncrementQty.setOnClickListener(v -> updateQuantity(1));
        btnDecrementQty.setOnClickListener(v -> updateQuantity(-1));
    }

    private void updateQuantity(int delta) {
        if (currentSelectedVariant == null || currentSelectedVariant.quantity <= 0) {
            Toast.makeText(this, "Sản phẩm đã hết hàng.", Toast.LENGTH_SHORT).show();
            return;
        }

        int newQuantity = currentQuantityToBuy + delta;
        long maxQuantity = currentSelectedVariant.quantity;

        // 1. KIỂM TRA GIỚI HẠN DƯỚI (Không dưới 1)
        if (newQuantity < 1) {
            newQuantity = 1;
            Toast.makeText(this, "Số lượng không được nhỏ hơn 1.", Toast.LENGTH_SHORT).show();
        }

        // 2. KIỂM TRA GIỚI HẠN TRÊN (Không vượt quá tồn kho)
        if (newQuantity > maxQuantity) {
            Toast.makeText(this, "Số lượng đặt không được vượt quá tồn kho.", Toast.LENGTH_SHORT).show();
            newQuantity = (int) maxQuantity;
        }

        if (newQuantity != currentQuantityToBuy) {
            currentQuantityToBuy = newQuantity;
            textSelectedQuantity.setText(String.valueOf(currentQuantityToBuy));
        }

        // 3. Cập nhật trạng thái kích hoạt của nút sau khi thay đổi số lượng
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (currentSelectedVariant == null || currentSelectedVariant.quantity <= 0) {
            btnIncrementQty.setEnabled(false);
            btnDecrementQty.setEnabled(false);
            return;
        }

        long maxQuantity = currentSelectedVariant.quantity;

        // BẬT/TẮT NÚT CỘNG (+)
        btnIncrementQty.setEnabled(currentQuantityToBuy < maxQuantity);

        // BẬT/TẮT NÚT TRỪ (-)
        btnDecrementQty.setEnabled(currentQuantityToBuy > 1);
    }


    private void loadProductDetails(String productId) {
        Log.d(TAG, "Đang cố tải Document ID: " + productId);

        db.collection("products")
                .document(productId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        if (document.exists()) {
                            try {
                                currentProduct = document.toObject(Product.class);

                                if (currentProduct != null && currentProduct.colorImages != null && !currentProduct.colorImages.isEmpty()) {
                                    displayProductData(currentProduct);
                                } else {
                                    Log.e(TAG, "LỖI DỮ LIỆU: currentProduct là NULL hoặc thiếu trường colorImages.");
                                    Toast.makeText(this, "Thiếu dữ liệu ảnh chi tiết. Vui lòng ghi lại dữ liệu mẫu.", Toast.LENGTH_LONG).show();
                                }

                            } catch (Exception e) {
                                Log.e(TAG, "LỖI DESERIALIZATION FATAL:", e);
                                Toast.makeText(this, "Lỗi Deserialization. Cấu trúc Model không khớp DB.", Toast.LENGTH_LONG).show();
                            }

                        } else {
                            Log.e(TAG, "LỖI ID: Document ID không tồn tại trong Firestore.");
                            Toast.makeText(this, "Không tìm thấy sản phẩm này.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "LỖI TRUY VẤN FIRESTORE:", task.getException());
                        Toast.makeText(this, "Lỗi kết nối Firestore.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void displayProductData(Product product) {
        // 1. Gán dữ liệu cơ bản
        textProductName.setText(product.name);

        textPrice.setText(String.format(Locale.getDefault(), "%,.0f VND", product.currentPrice));

        // Gán Review và Rating
        if (product.getTotalReviews() != null && product.getAverageRating() != null) {
            textReviewCount.setText(String.format("(%d)", product.getTotalReviews()));
            ratingBarDetail.setRating(product.getAverageRating().floatValue());
        }

        // 2. Setup Color Selector
        List<String> colorNames = new ArrayList<>(product.colorImages.keySet());
        if (!colorNames.isEmpty()) {
            currentSelectedColor = colorNames.get(0);

            recyclerColors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerColors.setAdapter(new ColorSelectorAdapter(colorNames, this));

            // 3. Load ảnh và size cho màu mặc định
            loadImagesAndSizesForColor(currentSelectedColor);
        }

        // 4. Setup Recommendations (GỌI HÀM VỚI TYPE THỰC TẾ)
        if (product.type != null) {
            setupRecommendation(product.type);
        } else {
            Log.w(TAG, "Sản phẩm thiếu trường 'type' để đề xuất.");
        }
    }

    // Xử lý khi màu được chọn
    @Override
    public void onColorSelected(String colorName) {
        if (!colorName.equals(currentSelectedColor)) {
            currentSelectedColor = colorName;
            loadImagesAndSizesForColor(currentSelectedColor);
        }
    }

    // Xử lý khi size được chọn (TRIỂN KHAI INTERFACE SIZE)
    @Override
    public void onSizeSelected(ProductVariant selectedVariant) {
        currentSelectedVariant = selectedVariant;

        // 1. Reset số lượng mua về 1 (hoặc 0 nếu hết hàng)
        currentQuantityToBuy = selectedVariant.quantity > 0 ? 1 : 0;

        // 2. Cập nhật tên size hiển thị
        if (currentProduct != null) {
            if (textSizeLabel != null) {
                textSizeLabel.setText(String.format("Size: %s %s", currentProduct.getCategory(), selectedVariant.size));
            }
        }

        // 3. Cập nhật Giá
        textPrice.setText(String.format(Locale.getDefault(), "%,.0f VND", selectedVariant.price));

        // 4. Cập nhật Tồn kho (Hiển thị số lượng và trạng thái)
        long quantity = selectedVariant.quantity;

        if (quantity > 0 && quantity <= 20) {
            textStockStatus.setText("Low stock");
            textStockStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            textInventoryCount.setText(String.format(Locale.getDefault(), "Còn %d sản phẩm trong kho", quantity));
        } else if (quantity > 20) {
            textStockStatus.setText("In stock");
            textStockStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            textInventoryCount.setText(String.format(Locale.getDefault(), "Còn %d sản phẩm trong kho", quantity));
        } else {
            textStockStatus.setText("Sold out");
            textStockStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            textInventoryCount.setText("Sản phẩm tạm thời hết hàng.");
        }

        // 5. Cập nhật TextView số lượng mua và nút
        if (textSelectedQuantity != null) {
            textSelectedQuantity.setText(String.valueOf(currentQuantityToBuy));
        }

        btnAddToCart.setEnabled(quantity > 0);

        // 6. Cập nhật trạng thái nút Cộng/Trừ
        updateButtonStates();
    }


    private void loadImagesAndSizesForColor(String colorName) {
        // Cập nhật tên màu hiển thị
        textColorName.setText("Color: " + colorName.toUpperCase());

        // 1. Tải và hiển thị ảnh (Sử dụng Map colorImages)
        List<String> images = currentProduct.colorImages.getOrDefault(colorName, new ArrayList<>());
        currentImages = images;

        viewPager.setAdapter(new ProductImageSliderAdapter(images, currentProduct));

        // Cập nhật chỉ báo ảnh
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                textImageIndicator.setText(String.format(Locale.getDefault(), "%d | %d", position + 1, images.size()));
            }
        });
        textImageIndicator.setText(String.format(Locale.getDefault(), "%d | %d", 1, images.size()));


        // 2. Lọc và hiển thị Sizes (Variants) cho màu này
        if (currentProduct.variants != null) {
            List<ProductVariant> variantsForColor = currentProduct.variants.stream()
                    .filter(v -> v.color.equalsIgnoreCase(colorName))
                    .collect(Collectors.toList());

            // TẠO SIZE ADAPTER VỚI LISTENER MỚI
            SizeSelectorAdapter sizeAdapter = new SizeSelectorAdapter(variantsForColor, this);
            recyclerSizes.setAdapter(sizeAdapter);

            // KÍCH HOẠT SIZE ĐẦU TIÊN MẶC ĐỊNH để hiển thị tồn kho/giá
            if (!variantsForColor.isEmpty()) {
                sizeAdapter.selectInitialVariant();
            }

        } else {
            recyclerSizes.setAdapter(new SizeSelectorAdapter(new ArrayList<>(), this));
        }
    }

    /**
     * Tải danh sách sản phẩm đề xuất từ Firestore bằng cách lọc theo TYPE (Sub-Category).
     * @param type Tên Type của sản phẩm hiện tại.
     */
    private void setupRecommendation(String type) {
        db.collection("products")
                .whereEqualTo("type", type)
                .limit(6)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Recommendation> recommendations = new ArrayList<>();

                        for (DocumentSnapshot document : task.getResult()) {
                            Product recommendedProduct = document.toObject(Product.class);

                            if (recommendedProduct != null &&
                                    !recommendedProduct.getProductId().equals(currentProduct.productId) &&
                                    recommendedProduct.getMainImage() != null)
                            {
                                recommendations.add(new Recommendation(
                                        recommendedProduct.getName(),
                                        recommendedProduct.getCurrentPrice(),
                                        recommendedProduct.getMainImage(),
                                        recommendedProduct.getCategory() + ", Size: S-XL",
                                        "Various Colors"
                                ));
                            }
                        }

                        if (!recommendations.isEmpty()) {
                            Log.d(TAG, "Đã tải " + recommendations.size() + " sản phẩm đề xuất cùng loại.");
                            recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                            recyclerRecommendations.setAdapter(new RecommendationAdapter(recommendations));
                        } else {
                            Log.d(TAG, "Không tìm thấy sản phẩm cùng loại để đề xuất.");
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi tải sản phẩm đề xuất.", task.getException());
                    }
                });
    }
}