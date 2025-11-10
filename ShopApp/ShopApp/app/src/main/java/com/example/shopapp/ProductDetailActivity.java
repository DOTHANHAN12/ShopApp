package com.example.shopapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RatingBar;
import android.graphics.Paint;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductDetailActivity extends AppCompatActivity implements
        ColorSelectorAdapter.OnColorSelectedListener,
        SizeSelectorAdapter.OnSizeSelectedListener
{

    private static final String TAG = "ProductDetailAct";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Elements
    private TextView textImageIndicator, textProductName, textPrice, textReviewCount, textColorName, textSizeLabel, textStockStatus, textSelectedQuantity, textInventoryCount;
    private TextView textOriginalPrice, textSeeAllReviews;
    private ViewPager2 viewPager;
    private RatingBar ratingBarDetail;
    private Button btnAddToCart;
    private RecyclerView recyclerColors, recyclerSizes, recyclerRecommendations, recyclerReviews;
    private ImageView imgBack;
    private ImageView navHomeDetail;
    private TextView textToolbarTitle;
    private ImageView iconFavoriteToolbar;
    private ImageView iconFavoriteDetail;

    // Nút Cộng/Trừ
    private TextView btnIncrementQty;
    private TextView btnDecrementQty;

    // Data
    private Product currentProduct;
    private String currentSelectedColor;
    private List<String> currentImages;
    private ProductVariant currentSelectedVariant;
    private boolean isFavorited = false;

    private int currentQuantityToBuy = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SETUP UI CƠ BẢN VÀ FIX STATUS BAR
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_product_detail);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 1. Ánh xạ Views
        mapViews();

        // *** Gán Listener cho nút Thêm vào Giỏ hàng ***
        btnAddToCart.setOnClickListener(v -> addToCart());
        // ******************************************************

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
        textOriginalPrice = findViewById(R.id.text_original_price);
        textReviewCount = findViewById(R.id.text_review_count);
        textColorName = findViewById(R.id.text_color_name);
        textSizeLabel = findViewById(R.id.text_size_label);
        textStockStatus = findViewById(R.id.text_stock_status);
        textSelectedQuantity = findViewById(R.id.text_selected_quantity);
        textInventoryCount = findViewById(R.id.text_inventory_count);
        textSeeAllReviews = findViewById(R.id.text_see_all_reviews);

        ratingBarDetail = findViewById(R.id.rating_bar_detail);

        recyclerColors = findViewById(R.id.recycler_color_selector);
        recyclerSizes = findViewById(R.id.recycler_size_selector);
        recyclerRecommendations = findViewById(R.id.recycler_frequently_bought);
        recyclerReviews = findViewById(R.id.recycler_reviews);

        imgBack = findViewById(R.id.img_back);
        btnAddToCart = findViewById(R.id.btn_add_to_cart);

        // Ánh xạ nút Cộng/Trừ
        btnIncrementQty = findViewById(R.id.btn_increment_qty);
        btnDecrementQty = findViewById(R.id.btn_decrement_qty);

        // KÍCH HOẠT VÀ ÁNH XẠ ICONS HEADER
        iconFavoriteToolbar = findViewById(R.id.img_favorite);
        iconFavoriteDetail = findViewById(R.id.img_favorite_detail);
        ImageView iconCartToolbar = findViewById(R.id.img_cart);

        if (iconCartToolbar != null) {
            iconCartToolbar.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }
        if (iconFavoriteToolbar != null) {
            iconFavoriteToolbar.setOnClickListener(v -> toggleFavorite());
        }
        if (iconFavoriteDetail != null) {
            iconFavoriteDetail.setOnClickListener(v -> toggleFavorite());
        }

        // Setup notification button
        ImageView iconNotification = findViewById(R.id.img_notification);
        if (iconNotification != null) {
            iconNotification.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() != null) {
                    startActivity(new Intent(this, NotificationActivity.class));
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để xem thông báo.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                }
            });
        }

        // Ánh xạ nút Home (Footer)
        navHomeDetail = findViewById(R.id.nav_home_cs);
        if (navHomeDetail != null) {
            navHomeDetail.setOnClickListener(v -> {
                Intent intent = new Intent(ProductDetailActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Setup user button (footer)
        ImageView navUserDetail = findViewById(R.id.nav_user_cs);
        if (navUserDetail != null) {
            navUserDetail.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() != null) {
                    startActivity(new Intent(this, ProfileActivity.class));
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để xem Hồ sơ.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                }
            });
        }

        // Thiết lập Listener cho Cộng/Trừ
        setupQuantityButtons();
    }

    // --------------------------------------------------------------------------------
    // LOGIC ADD TO CART
    // --------------------------------------------------------------------------------

    private boolean isOfferValid(OfferDetails offer) {
        if (offer == null || offer.getStartDate() == null || offer.getEndDate() == null) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        return currentTime >= offer.getStartDate() && currentTime <= offer.getEndDate();
    }

    private void addToCart() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        if (currentProduct == null || currentSelectedVariant == null) {
            Toast.makeText(this, "Vui lòng chọn Màu sắc và Kích cỡ trước.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentSelectedVariant.quantity <= 0) {
            Toast.makeText(this, "Sản phẩm đã hết hàng.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentQuantityToBuy <= 0) {
            Toast.makeText(this, "Vui lòng chọn số lượng.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. TÍNH TOÁN GIÁ HIỂN THỊ CUỐI CÙNG
        double priceAtTimeOfAdd = currentSelectedVariant.price;
        if (currentProduct.getIsOfferStatus() && isOfferValid(currentProduct.getOffer())) {
            OfferDetails offer = currentProduct.getOffer();
            if (offer != null && offer.getOfferValue() != null) {
                 double discount = offer.getOfferValue() / 100.0;
                 priceAtTimeOfAdd = currentSelectedVariant.price * (1.0 - discount);
            }
        }

        final String userId = user.getUid();
        final String productId = currentProduct.getProductId();
        final String variantId = currentSelectedVariant.variantId;
        final String documentId = productId + "_" + variantId; // Key duy nhất trong giỏ hàng
        final int quantityToAdd = currentQuantityToBuy;
        final double finalPriceAtTimeOfAdd = priceAtTimeOfAdd; // Khai báo final

        // 2. Tải giỏ hàng hiện tại để kiểm tra số lượng cộng dồn
        db.collection("users").document(userId).collection("cart").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    int existingQuantity = 0;

                    if (documentSnapshot.exists()) {
                        // SẢN PHẨM ĐÃ CÓ TRONG GIỎ: Lấy số lượng hiện tại
                        CartItem existingItem = documentSnapshot.toObject(CartItem.class);
                        if (existingItem != null) {
                            existingQuantity = existingItem.getQuantity();
                        }
                    }

                    int finalQuantity = existingQuantity + quantityToAdd; // Tính toán tổng số lượng

                    // Kiểm tra lại tồn kho sau khi cộng dồn
                    if (finalQuantity > currentSelectedVariant.quantity) {
                        Toast.makeText(this, "Số lượng trong giỏ (" + existingQuantity + ") + số lượng thêm (" + quantityToAdd + ") vượt quá tồn kho!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 3. Tạo/Cập nhật đối tượng CartItem
                    CartItem newCartItem = new CartItem(
                            productId,
                            variantId,
                            finalQuantity, // Số lượng mới đã cộng dồn
                            finalPriceAtTimeOfAdd, // Giá được tính toán tại thời điểm thêm vào
                            System.currentTimeMillis() // Thời gian cập nhật
                    );

                    // 4. Lưu vào Firestore
                    db.collection("users").document(userId).collection("cart").document(documentId)
                            .set(newCartItem)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã thêm " + quantityToAdd + " sản phẩm vào Giỏ hàng. Tổng số lượng: " + finalQuantity, Toast.LENGTH_SHORT).show();
                                // Reset số lượng mua về 1 và cập nhật UI
                                currentQuantityToBuy = 1;
                                textSelectedQuantity.setText("1");
                                updateButtonStates();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lỗi thêm vào Giỏ hàng: ", e);
                                Toast.makeText(this, "Lỗi thêm vào Giỏ hàng. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi truy vấn Giỏ hàng: ", e);
                    Toast.makeText(this, "Lỗi kiểm tra giỏ hàng.", Toast.LENGTH_SHORT).show();
                });
    }

    // --------------------------------------------------------------------------------
    // LOGIC FAVORITE
    // --------------------------------------------------------------------------------

    private void toggleFavorite() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào Yêu thích.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (currentProduct == null) return;

        String productId = currentProduct.getProductId();

        if (isFavorited) {
            // Bỏ yêu thích
            db.collection("users").document(user.getUid()).collection("favorites").document(productId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã bỏ yêu thích.", Toast.LENGTH_SHORT).show();
                        isFavorited = false;
                        updateFavoriteIcon();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi bỏ yêu thích.", Toast.LENGTH_SHORT).show());
        } else {
            // Thêm vào yêu thích
            db.collection("users").document(user.getUid()).collection("favorites").document(productId)
                    .set(new FavoriteItem(System.currentTimeMillis()))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã thêm vào mục Yêu thích.", Toast.LENGTH_SHORT).show();
                        isFavorited = true;
                        updateFavoriteIcon();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi thêm vào Yêu thích.", Toast.LENGTH_SHORT).show());
        }
    }

    private void checkFavoriteStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || currentProduct == null) {
            isFavorited = false;
            updateFavoriteIcon();
            return;
        }

        String productId = currentProduct.getProductId();
        db.collection("users").document(user.getUid()).collection("favorites").document(productId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        isFavorited = document != null && document.exists();
                        updateFavoriteIcon();
                    } else {
                        isFavorited = false;
                        updateFavoriteIcon();
                    }
                });
    }

    private void updateFavoriteIcon() {
        if (isFavorited) {
            iconFavoriteToolbar.setImageResource(R.drawable.ic_favorite_filled);
            iconFavoriteDetail.setImageResource(R.drawable.ic_favorite_filled);
            iconFavoriteToolbar.setColorFilter(ContextCompat.getColor(this, R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);
            iconFavoriteDetail.setColorFilter(ContextCompat.getColor(this, R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            iconFavoriteToolbar.setImageResource(R.drawable.ic_favorite_outline);
            iconFavoriteDetail.setImageResource(R.drawable.ic_favorite_outline);
            iconFavoriteToolbar.clearColorFilter();
            iconFavoriteDetail.clearColorFilter();
        }
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

                                if (currentProduct.getColorImages() != null && !currentProduct.getColorImages().isEmpty()) {
                                    displayProductData(currentProduct);
                                    checkFavoriteStatus();
                                    loadReviews(productId);
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
        textProductName.setText(product.getName());

        // 2. TÍNH TOÁN VÀ GÁN GIÁ (Áp dụng KM cho lần đầu)
        updatePriceUIInitial(product.getBasePrice(), product.getIsOfferStatus(), product.getOffer());

        // Gán Review và Rating
        if (product.getTotalReviews() != null && product.getAverageRating() != null) {
            textReviewCount.setText(String.format("(%d)", product.getTotalReviews()));
            ratingBarDetail.setRating(product.getAverageRating().floatValue());
        }

        // 3. Setup Color Selector
        List<String> colorNames = new ArrayList<>(product.getColorImages().keySet());
        if (!colorNames.isEmpty()) {
            currentSelectedColor = colorNames.get(0);

            recyclerColors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerColors.setAdapter(new ColorSelectorAdapter(colorNames, this));

            // 4. Load ảnh và size cho màu mặc định
            loadImagesAndSizesForColor(currentSelectedColor);
        }

        // 5. Setup Recommendations
        if (product.getType() != null) {
            setupRecommendation(product.getType());
        } else {
            Log.w(TAG, "Sản phẩm thiếu trường 'type' để đề xuất.");
        }

        textSeeAllReviews.setOnClickListener(v -> {
            Intent intent = new Intent(ProductDetailActivity.this, ReviewListActivity.class);
            intent.putExtra("PRODUCT_ID", currentProduct.getProductId());
            startActivity(intent);
        });
    }

    private void loadReviews(String productId) {
        db.collection("products").document(productId).collection("reviews")
                .whereEqualTo("status", "APPROVED")
                .limit(3) // Giới hạn ở 3 bài đánh giá gần nhất
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Review> reviews = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            reviews.add(document.toObject(Review.class));
                        }
                        setupReviewsRecyclerView(reviews);
                    } else {
                        Log.e(TAG, "Error getting reviews: ", task.getException());
                    }
                });
    }

    private void setupReviewsRecyclerView(List<Review> reviews) {
        recyclerReviews.setLayoutManager(new LinearLayoutManager(this));
        recyclerReviews.setAdapter(new ReviewAdapter(reviews));
    }

    /**
     * TÍNH TOÁN VÀ CẬP NHẬT GIÁ DỰA TRÊN LOGIC KHUYẾN MÃI (CHỈ GỌI KHI LOAD BAN ĐẦU)
     */
    private void updatePriceUIInitial(double basePrice, boolean isOffer, OfferDetails offer) {
        double displayPrice = basePrice;
        boolean hasValidOffer = false;

        if (isOffer && isOfferValid(offer)) {
            hasValidOffer = true;
            if (offer != null && offer.getOfferValue() != null) {
                double discount = offer.getOfferValue() / 100.0;
                displayPrice = basePrice * (1.0 - discount);
            }
        }

        // GÁN GIÁ HIỆN TẠI (ĐÃ GIẢM HOẶC BASE PRICE)
        textPrice.setText(String.format(Locale.getDefault(), "%,.0f VND", displayPrice));

        // HIỂN THỊ GIÁ GỐC VÀ GẠCH NGANG (Chỉ cho lần load đầu tiên)
        if (textOriginalPrice != null) {
            if (hasValidOffer) {
                textOriginalPrice.setText(String.format(Locale.getDefault(), "%,.0f VND", basePrice));
                textOriginalPrice.setPaintFlags(textOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textOriginalPrice.setText("");
                textOriginalPrice.setPaintFlags(0);
            }
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

        // -----------------------------------------------------------
        // FIX LOGIC: TÍNH GIÁ ĐÃ GIẢM DỰA TRÊN GIÁ VARIANT BASE PRICE
        // -----------------------------------------------------------

        double variantBasePrice = selectedVariant.price;
        double finalDisplayPrice = variantBasePrice;
        boolean hasValidOffer = false;

        if (currentProduct.getIsOfferStatus() && isOfferValid(currentProduct.getOffer())) {
            hasValidOffer = true;
            OfferDetails offer = currentProduct.getOffer();
            if (offer != null && offer.getOfferValue() != null) {
                double discount = offer.getOfferValue() / 100.0;
                finalDisplayPrice = variantBasePrice * (1.0 - discount);
            }
        }

        // 3. Cập nhật GIÁ HIỆN TẠI (Giá sau khi giảm)
        textPrice.setText(String.format(Locale.getDefault(), "%,.0f VND", finalDisplayPrice));

        // 4. CẬP NHẬT GIÁ GỐC GẠCH NGANG
        if (textOriginalPrice != null) {
            if (hasValidOffer && finalDisplayPrice < variantBasePrice) {
                // Hiển thị giá gốc của variant và gạch ngang
                textOriginalPrice.setText(String.format(Locale.getDefault(), "%,.0f VND", variantBasePrice));
                textOriginalPrice.setPaintFlags(textOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                // Khuyến mãi hết hạn hoặc không hợp lệ, xóa giá gốc
                textOriginalPrice.setText("");
                textOriginalPrice.setPaintFlags(0);
            }
        }

        // 5. Cập nhật Tồn kho (Hiển thị số lượng và trạng thái)
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

        // 6. Cập nhật TextView số lượng mua và nút
        if (textSelectedQuantity != null) {
            textSelectedQuantity.setText(String.valueOf(currentQuantityToBuy));
        }

        btnAddToCart.setEnabled(quantity > 0);

        // 7. Cập nhật trạng thái nút Cộng/Trừ
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
                    .filter(v -> v.color != null && v.color.equalsIgnoreCase(colorName))
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
                .whereEqualTo("status", "Active") // ✅ LỌC SẢN PHẨM ACTIVE
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

                                // ✅ SỬA LỖI: TÍNH TOÁN GIÁ VÀ GỌI ĐÚNG CONSTRUCTOR
                                double originalPrice = recommendedProduct.getBasePrice();
                                double displayPrice = originalPrice;
                                boolean hasOffer = false;

                                if (recommendedProduct.getIsOfferStatus() && isOfferValid(recommendedProduct.getOffer())) {
                                    OfferDetails offer = recommendedProduct.getOffer();
                                    if (offer != null && offer.getOfferValue() != null) {
                                        hasOffer = true;
                                        double discount = offer.getOfferValue() / 100.0;
                                        displayPrice = originalPrice * (1.0 - discount);
                                    }
                                }

                                recommendations.add(new Recommendation(
                                        recommendedProduct.getProductId(),
                                        recommendedProduct.getName(),
                                        recommendedProduct.getMainImage(),
                                        recommendedProduct.getCategory() + ", Size: S-XL",
                                        "Various Colors",
                                        displayPrice,
                                        originalPrice,
                                        hasOffer
                                ));
                            }
                        }

                        if (!recommendations.isEmpty()) {
                            Log.d(TAG, "Đã tải " + recommendations.size() + " sản phẩm đề xuất cùng loại.");
                            recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                            recyclerRecommendations.setAdapter(new RecommendationAdapter(recommendations, this));
                        } else {
                            Log.d(TAG, "Không tìm thấy sản phẩm cùng loại để đề xuất.");
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi tải sản phẩm đề xuất.", task.getException());
                    }
                });
    }
}
