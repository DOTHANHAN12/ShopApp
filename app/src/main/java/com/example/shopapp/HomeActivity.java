package com.example.shopapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private FirebaseFirestore db;

    private ViewPager2 viewPagerFeaturedProducts;
    private FeaturedProductAdapter adapter;
    private final List<Product> featuredProductsList = new ArrayList<>();

    private TextView tabWomen, tabMen, tabKids, tabBaby;
    private TextView currentSelectedTab;

    // Khai báo cho nút Search ở Footer Home (ID đã được thêm vào XML)
    private View searchButton;

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

        db = FirebaseFirestore.getInstance();

        // 1. Ánh xạ Views
        mapViews();

        // 2. Khởi tạo Adapter và ViewPager2
        adapter = new FeaturedProductAdapter(featuredProductsList);
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

        // Ánh xạ FrameLayout chứa icon Search ở Footer (Đảm bảo ID này tồn tại trong activity_home.xml)
        searchButton = findViewById(R.id.btn_search_footer);

        // Đặt tab mặc định
        currentSelectedTab = tabMen;
    }

    // ------------------- LOGIC XỬ LÝ TAB -------------------
    private void setupCategoryTabs() {
        View.OnClickListener tabClickListener = view -> {
            String category = ((TextView) view).getText().toString();

            // 1. Cập nhật UI (Gạch chân/In đậm)
            updateCategoryUI(category);

            // 2. Tải danh sách Featured Products mới cho Category đó (Vẫn ở Home)
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
                // Lấy Category đang được chọn để truyền sang màn hình Search
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

    // ------------------- TẢI DANH SÁCH SẢN PHẨM NỔI BẬT THEO CATEGORY -------------------

    /**
     * Tải danh sách các sản phẩm có thuộc tính isFeatured=true từ Firestore VÀ lọc theo Category.
     */
    private void loadFeaturedProductsList(String category) {
        db.collection("products")
                .whereEqualTo("isFeatured", true)
                .whereEqualTo("category", category) // Lọc theo Category được chọn
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        featuredProductsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            featuredProductsList.add(product);
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