package com.example.shopapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private FirebaseFirestore db;

    // Khai báo Views chi tiết sản phẩm
    private ImageView imgProduct;
    private TextView textTitle;
    private TextView textCurrentPrice;
    private TextView textOriginalPrice;
    private TextView textDescription;
    private TextView textOfferDetails;
    private TextView textDisclaimer;
    private TextView textLimitedOfferBadge;

    private TextView tabWomen, tabMen, tabKids, tabBaby;
    private TextView currentSelectedTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SETUP UI CƠ BẢN
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_home);

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        // Ánh xạ Views
        mapViews();

        // Thiết lập Listener cho các Tab
        setupCategoryTabs();

        // Tải sản phẩm nổi bật mặc định (ví dụ: MEN)
        loadFeaturedProductByCategory("MEN");
    }

    // --- MAPPING VIEWS ---
    private void mapViews() {
        imgProduct = findViewById(R.id.img_product);
        textTitle = findViewById(R.id.text_product_title);
        textCurrentPrice = findViewById(R.id.text_current_price);
        textOriginalPrice = findViewById(R.id.text_original_price);
        textDescription = findViewById(R.id.text_description);
        textOfferDetails = findViewById(R.id.text_offer_details);
        textDisclaimer = findViewById(R.id.text_disclaimer);
        textLimitedOfferBadge = findViewById(R.id.text_limited_offer_badge);

        tabWomen = findViewById(R.id.tab_women);
        tabMen = findViewById(R.id.tab_men);
        tabKids = findViewById(R.id.tab_kids);
        tabBaby = findViewById(R.id.tab_baby);

        currentSelectedTab = tabMen;
    }

    // --- LOGIC XỬ LÝ CLICK TAB VÀ SEARCH ---
    private void setupCategoryTabs() {
        // Listener cho các Tab Category (Chỉ cập nhật trên HomeActivity)
        View.OnClickListener tabClickListener = view -> {
            String category = ((TextView) view).getText().toString();

            updateCategoryUI(category);
            loadFeaturedProductByCategory(category);
        };

        tabWomen.setOnClickListener(tabClickListener);
        tabMen.setOnClickListener(tabClickListener);
        tabKids.setOnClickListener(tabClickListener);
        tabBaby.setOnClickListener(tabClickListener);

        // Listener cho nút Search (Chuyển sang màn hình Menu Danh mục)
        // Ánh xạ FrameLayout chứa icon Search (trong layout_bottom_nav)
        findViewById(R.id.layout_bottom_nav).findViewById(R.id.ic_search).getParent().setOnClickListener(v -> {
            // Chuyển sang màn hình Menu Danh mục (ví dụ: cho Category MEN)
            startMenuActivity("MEN");
        });
    }

    private void startMenuActivity(String defaultCategory) {
        // ProductListActivity sẽ đóng vai trò là màn hình Menu/Danh mục
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra("CATEGORY_KEY", defaultCategory);
        startActivity(intent);
    }

    private void updateCategoryUI(String newCategory) {
        if (currentSelectedTab != null) {
            currentSelectedTab.setAlpha(0.7f);
        }

        switch (newCategory) {
            case "WOMEN": currentSelectedTab = tabWomen; break;
            case "MEN": currentSelectedTab = tabMen; break;
            case "KIDS": currentSelectedTab = tabKids; break;
            case "BABY": currentSelectedTab = tabBaby; break;
            default: return;
        }
        currentSelectedTab.setAlpha(1.0f);
    }

    // ---------------------------------------------------------------------
    // TẢI SẢN PHẨM NỔI BẬT THEO CATEGORY
    // ---------------------------------------------------------------------

    /**
     * Tải MỘT sản phẩm isFeatured từ Firestore thuộc Category đã chọn.
     */
    private void loadFeaturedProductByCategory(String category) {
        db.collection("products")
                .whereEqualTo("category", category)
                .whereEqualTo("isFeatured", true)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        Product product = document.toObject(Product.class);
                        if (product != null) {
                            bindDataToViews(product);
                        }
                    } else {
                        Log.w(TAG, "Không tìm thấy sản phẩm nổi bật cho Category: " + category);
                        textTitle.setText(category + " - Không có sản phẩm nổi bật.");
                        // Xử lý ẩn ảnh/chi tiết khi không có dữ liệu
                    }
                });
    }

    /**
     * Gán dữ liệu từ đối tượng Product vào các Views.
     */
    private void bindDataToViews(Product product) {
        // Gán Tên
        textTitle.setText(product.name);

        // SỬA ĐỔI: Sử dụng trường 'desc' cho mô tả
        textDescription.setText(product.desc);

        // Gán Giá
        String currentPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", product.currentPrice);
        String originalPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", product.originalPrice);
        textCurrentPrice.setText(currentPriceFormatted);
        textOriginalPrice.setText(originalPriceFormatted);

        // Gán Thông tin Khuyến mãi
        textOfferDetails.setText(product.offerDetails);
        textDisclaimer.setText(product.extraInfo);

        // Cập nhật Badge "LIMITED OFFER"
        if (product.isOffer) {
            textLimitedOfferBadge.setVisibility(View.VISIBLE);
        } else {
            textLimitedOfferBadge.setVisibility(View.GONE);
        }

        // Tải Ảnh bằng Picasso
        loadImageFromUrl(product.imageUrl, imgProduct);
    }

    // --- PICASSO TẢI ẢNH ---
    private void loadImageFromUrl(String url, ImageView imageView) {
        if (url != null && !url.isEmpty()) {
            Picasso.get()
                    .load(url)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(imageView);
        }
    }
}