package com.example.shopapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import android.view.View;       // Cần thiết cho setOnClickListener, setAlpha
import android.view.WindowManager;

import android.widget.ImageView;
import android.widget.TextView;    // Cần thiết cho TextView

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private FirebaseFirestore db;

    // Khai báo ViewPager2 mới
    private ViewPager2 viewPagerFeaturedProducts;
    private FeaturedProductAdapter adapter;
    private final List<Product> featuredProductsList = new ArrayList<>();

    // Các Views cũ (Chỉ còn các tab, các icon)
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

        db = FirebaseFirestore.getInstance();

        // 1. Ánh xạ Views
        mapViews();

        // 2. Khởi tạo Adapter và ViewPager2
        adapter = new FeaturedProductAdapter(featuredProductsList);
        viewPagerFeaturedProducts.setAdapter(adapter);

        // 3. Thiết lập Listener cho các Tab
        setupCategoryTabs();

        // 4. Tải danh sách sản phẩm nổi bật
        loadFeaturedProductsList();
    }

    // --- MAPPING VIEWS ---
    private void mapViews() {
        // Ánh xạ ViewPager2 mới
        viewPagerFeaturedProducts = findViewById(R.id.view_pager_featured_products);

        // Tabs giữ nguyên
        tabWomen = findViewById(R.id.tab_women);
        tabMen = findViewById(R.id.tab_men);
        tabKids = findViewById(R.id.tab_kids);
        tabBaby = findViewById(R.id.tab_baby);

        currentSelectedTab = tabMen;
    }

    // --- LOGIC XỬ LÝ CLICK TAB ---
    private void setupCategoryTabs() {
        View.OnClickListener tabClickListener = view -> {
            String category = ((TextView) view).getText().toString();

            updateCategoryUI(category);

            // Chuyển Activity
            startProductListActivity(category);
        };

        tabWomen.setOnClickListener(tabClickListener);
        tabMen.setOnClickListener(tabClickListener);
        tabKids.setOnClickListener(tabClickListener);
        tabBaby.setOnClickListener(tabClickListener);
    }

    private void startProductListActivity(String category) {
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra("CATEGORY_KEY", category);
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
    // TẢI DANH SÁCH SẢN PHẨM NỔI BẬT
    // ---------------------------------------------------------------------

    /**
     * Tải danh sách các sản phẩm có thuộc tính isFeatured=true từ Firestore.
     */
    private void loadFeaturedProductsList() {
        db.collection("products")
                .whereEqualTo("isFeatured", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        featuredProductsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            featuredProductsList.add(product);
                        }

                        if (!featuredProductsList.isEmpty()) {
                            // Cập nhật ViewPager2
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.w(TAG, "Không tìm thấy sản phẩm nổi bật nào được tìm thấy.");
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi tải danh sách sản phẩm nổi bật.", task.getException());
                    }
                });
    }
}