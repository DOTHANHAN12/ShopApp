package com.example.shopapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ViewPager2 viewPagerFeaturedProducts;
    private FeaturedProductAdapter adapter;
    private final List<Product> featuredProductsList = new ArrayList<>();

    private TextView tabWomen, tabMen, tabKids, tabBaby;
    private TextView currentSelectedTab;
    private View searchButton;
    private ImageView iconProfile;

    // Khai báo Icons Header
    private ImageView iconNotification;
    private ImageView iconFavorite;
    private ImageView iconCart;


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
        // Status Bar
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 1. Ánh xạ Views và Icons Header
        mapViews();
        setupHeaderIcons(); // <--- THIẾT LẬP LISTENER ICONS

        // 2. Khởi tạo Adapter và ViewPager2
        adapter = new FeaturedProductAdapter(featuredProductsList, this::isOfferValid);
        viewPagerFeaturedProducts.setAdapter(adapter);

        // 3. Thiết lập Listener cho các Tab
        setupCategoryTabs();

        // 4. Thiết lập Listener cho nút Search ở Footer
        setupSearchButton();

        // 5. Khởi tạo trạng thái UI cho tab mặc định và tải dữ liệu ban đầu
        String initialCategory = "MEN";
        updateCategoryUI(initialCategory);
        loadFeaturedProductsList(initialCategory);
    }

    // ------------------- MAPPING VIEWS -------------------
    private void mapViews() {
        viewPagerFeaturedProducts = findViewById(R.id.view_pager_featured_products);

        tabWomen = findViewById(R.id.tab_women);
        tabMen = findViewById(R.id.tab_men);
        tabKids = findViewById(R.id.tab_kids);
        tabBaby = findViewById(R.id.tab_baby);

        searchButton = findViewById(R.id.btn_search_footer); // Nút Search lớn ở Footer

        // ÁNH XẠ ICONS HEADER
        iconNotification = findViewById(R.id.ic_notification);
        iconFavorite = findViewById(R.id.ic_favorite);
        iconCart = findViewById(R.id.ic_cart);
        iconProfile = findViewById(R.id.nav_profile); // Make sure this ID exists in your layout

        currentSelectedTab = tabMen;
    }

    // ------------------- LOGIC KÍCH HOẠT ICONS HEADER -------------------
    private void setupHeaderIcons() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // NÚT YÊU THÍCH (Favorite)
        if (iconFavorite != null) {
            iconFavorite.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() != null) {
                    startActivity(new Intent(this, FavoriteActivity.class));
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để xem mục Yêu thích.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                }
            });
        }

        // NÚT GIỎ HÀNG (Cart)
        if (iconCart != null) {
            iconCart.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() != null) {
                    startActivity(new Intent(this, CartActivity.class));
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để xem Giỏ hàng.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                }
            });
        }

        // NÚT THÔNG BÁO (Notification)
        if (iconNotification != null) {
            iconNotification.setOnClickListener(v -> {
                Toast.makeText(this, "Chức năng Thông báo đang phát triển.", Toast.LENGTH_SHORT).show();
            });
        }

        // NÚT PROFILE
        if (iconProfile != null) {
            iconProfile.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() != null) {
                    startActivity(new Intent(this, ProfileActivity.class));
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để xem thông tin cá nhân.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                }
            });
        }
    }


    // ------------------- LOGIC XỬ LÝ TAB -------------------
    private void setupCategoryTabs() {
        View.OnClickListener tabClickListener = view -> {
            String category = ((TextView) view).getText().toString();

            // 1. Cập nhật UI (Gạch chân/In đậm)
            updateCategoryUI(category);

            // 2. Tải danh sách Featured Products mới cho Category đó
            loadFeaturedProductsList(category);
        };

        tabWomen.setOnClickListener(tabClickListener);
        tabMen.setOnClickListener(tabClickListener);
        tabKids.setOnClickListener(tabClickListener);
        tabBaby.setOnClickListener(tabClickListener);
    }

    // ------------------- LOGIC XỬ LÝ NÚT SEARCH -------------------
    private void setupSearchButton() {
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> {
                String categoryToSearch = (currentSelectedTab != null) ? currentSelectedTab.getText().toString() : "MEN";
                Intent intent = new Intent(HomeActivity.this, CategorySearchActivity.class);
                intent.putExtra("CATEGORY_KEY", categoryToSearch);
                startActivity(intent);
            });
        }
    }

    /**
     * Cập nhật UI cho Tab Category: Gạch chân, In đậm (Tăng Alpha/Size).
     */
    private void updateCategoryUI(String newCategory) {
        if (currentSelectedTab != null) {
            currentSelectedTab.setAlpha(0.7f);
            currentSelectedTab.setPaintFlags(currentSelectedTab.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            currentSelectedTab.setTextSize(16);
        }

        switch (newCategory) {
            case "WOMEN": currentSelectedTab = tabWomen; break;
            case "MEN": currentSelectedTab = tabMen; break;
            case "KIDS": currentSelectedTab = tabKids; break;
            case "BABY": currentSelectedTab = tabBaby; break;
            default: return;
        }

        currentSelectedTab.setAlpha(1.0f);
        currentSelectedTab.setPaintFlags(currentSelectedTab.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        currentSelectedTab.setTextSize(18);
    }

    private boolean isOfferValid(OfferDetails offer) {
        if (offer == null || offer.getStartDate() == null || offer.getEndDate() == null) {
            return false;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = sdf.parse(offer.getStartDate());
            Date endDate = sdf.parse(offer.getEndDate());
            Date currentDate = new Date();
            return !currentDate.before(startDate) && !currentDate.after(endDate);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing offer dates", e);
            return false;
        }
    }

    // ------------------- TẢI DANH SÁCH SẢN PHẨM NỔI BẬT THEO CATEGORY -------------------

    /**
     * Tải danh sách các sản phẩm có thuộc tính isFeatured=true từ Firestore VÀ lọc theo Category.
     */
    private void loadFeaturedProductsList(String category) {
        db.collection("products")
                .whereEqualTo("isFeatured", true)
                .whereEqualTo("category", category)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        featuredProductsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Product product = new Product();
                                product.setProductId(document.getString("productId"));
                                product.setName(document.getString("name"));
                                product.setDesc(document.getString("desc"));

                                if (document.get("basePrice") instanceof Number) {
                                    product.setBasePrice(((Number) document.get("basePrice")).doubleValue());
                                }

                                product.setMainImage(document.getString("mainImage"));
                                product.setCategory(document.getString("category"));
                                product.setType(document.getString("type"));
                                product.setStatus(document.getString("status"));

                                if (document.getBoolean("isOffer") != null) {
                                    product.setIsOfferStatus(document.getBoolean("isOffer"));
                                }

                                if (document.get("averageRating") instanceof Number) {
                                    product.setAverageRating(((Number) document.get("averageRating")).doubleValue());
                                }

                                product.setTotalReviews(document.getLong("totalReviews"));

                                if (document.getBoolean("isFeatured") != null) {
                                    product.setFeatured(document.getBoolean("isFeatured"));
                                }

                                product.setColorImages((Map<String, List<String>>) document.get("colorImages"));
                                product.setCreatedAt(document.getLong("createdAt"));
                                product.setUpdatedAt(document.getLong("updatedAt"));

                                if (document.contains("offer")) {
                                    product.setOffer(document.get("offer", OfferDetails.class));
                                }

                                if (document.contains("variants")) {
                                    List<Map<String, Object>> variantMaps = (List<Map<String, Object>>) document.get("variants");
                                    List<ProductVariant> variants = new ArrayList<>();
                                    for (Map<String, Object> map : variantMaps) {
                                        ProductVariant variant = new ProductVariant();
                                        variant.setVariantId((String) map.get("variantId"));
                                        variant.setSize((String) map.get("size"));
                                        variant.setColor((String) map.get("color"));
                                        variant.setQuantity((Long) map.get("quantity"));

                                        if (map.get("price") instanceof Number) {
                                            variant.setPrice(((Number) map.get("price")).doubleValue());
                                        }

                                        if (map.containsKey("status")) {
                                            variant.setStatus((String) map.get("status"));
                                        }
                                        variants.add(variant);
                                    }
                                    product.setVariants(variants);
                                }
                                featuredProductsList.add(product);
                            } catch (Exception e) {
                                Log.e(TAG, "Could not parse product", e);
                            }
                        }

                        if (!featuredProductsList.isEmpty()) {
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.w(TAG, "Không tìm thấy sản phẩm nổi bật nào cho danh mục " + category);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi tải danh sách sản phẩm nổi bật.", task.getException());
                    }
                });
    }
}
