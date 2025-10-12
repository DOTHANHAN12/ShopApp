package com.example.shopapp;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CategorySearchActivity extends AppCompatActivity {

    private static final String TAG = "CategorySearchAct";
    private FirebaseFirestore db;

    private String currentCategory;
    private TextView tabWomen, tabMen, tabKids, tabBaby;
    private TextView currentSelectedTab;
    private EditText edtSearchKeyword;
    private ImageView navHome;

    private RecyclerView recyclerView;
    private SubCategoryAdapter subCategoryAdapter;
    private final List<SubCategory> subCategoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_search);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = FirebaseFirestore.getInstance();

        currentCategory = getIntent().getStringExtra("CATEGORY_KEY");
        if (currentCategory == null) {
            currentCategory = "WOMEN";
        }

        mapViews();
        mapViewsFooter();

        // Khởi tạo RecyclerView và Adapter
        recyclerView = findViewById(R.id.recycler_category_grid);
        // Lưu ý: Cần truyền currentCategory vào Adapter để xử lý click item sau này
        // SubCategoryAdapter hiện đã không dùng iconResId nữa, nhưng ta giữ nguyên cấu trúc này
        subCategoryAdapter = new SubCategoryAdapter(subCategoryList, currentCategory);

        // Thiết lập GridLayout 2 cột
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(subCategoryAdapter);

        // Khởi tạo UI và Listener
        updateCategoryUI(currentCategory);
        setupCategoryTabs();
        setupSearchBarListener();

        // Tải dữ liệu Sub Category từ Firestore
        loadSubCategoryData(currentCategory);
    }

    private void mapViews() {
        // Ánh xạ các tab category trên màn hình nền trắng
        tabWomen = findViewById(R.id.tab_women_cs);
        tabMen = findViewById(R.id.tab_men_cs);
        tabKids = findViewById(R.id.tab_kids_cs);
        tabBaby = findViewById(R.id.tab_baby_cs);
        edtSearchKeyword = findViewById(R.id.edt_search_keyword);
    }

    /**
     * Ánh xạ và thiết lập Listener cho Footer (theo yêu cầu phải giống Home)
     */
    private void mapViewsFooter() {
        navHome = findViewById(R.id.nav_home_cs);

        // Thiết lập Listener chuyển về HomeActivity
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(CategorySearchActivity.this, HomeActivity.class);
            // Cờ để xóa stack và quay về màn hình chính
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Kết thúc CategorySearchActivity
        });
    }

    private void setupCategoryTabs() {
        View.OnClickListener tabClickListener = view -> {
            String category = ((TextView) view).getText().toString();

            updateCategoryUI(category);
            currentCategory = category;

            // Tải lại dữ liệu và cập nhật Adapter với Category mới
            loadSubCategoryData(category);
        };

        tabWomen.setOnClickListener(tabClickListener);
        tabMen.setOnClickListener(tabClickListener);
        tabKids.setOnClickListener(tabClickListener);
        tabBaby.setOnClickListener(tabClickListener);
    }

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
        this.currentCategory = newCategory;
    }

    private void setupSearchBarListener() {
        // Khi bấm vào Search Bar, chuyển sang ProductListActivity với Category hiện tại
        edtSearchKeyword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductListActivity.class);
            intent.putExtra("CATEGORY_KEY", currentCategory);
            // Có thể thêm tham số để ProductListActivity hiểu đây là "danh sách tổng"
            startActivity(intent);
        });

        // Icon Filter
        findViewById(R.id.img_search_filter).setOnClickListener(v -> {
            // Logic mở màn hình Filter
        });
    }

    /**
     * Tải danh sách các TYPE (Danh mục con) duy nhất từ Collection 'products'
     * dựa trên Category đang được chọn và lấy URL ảnh đại diện cho mỗi Type.
     */
    private void loadSubCategoryData(String category) {
        db.collection("products")
                .whereEqualTo("category", category)
                // Lọc thêm điều kiện để tránh tải quá nhiều
                .limit(50)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        subCategoryList.clear();

                        // Map lưu trữ: Type Name -> SubCategory (đã có URL ảnh)
                        Map<String, SubCategory> subCategoryMap = new HashMap<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            String type = product.type;

                            if (type != null && !subCategoryMap.containsKey(type)) {
                                // Chỉ lấy SubCategory đầu tiên tìm thấy cho mỗi Type
                                String imageUrl = null;
                                if (product.images != null && product.images.subCategoryImage != null) {
                                    imageUrl = product.images.subCategoryImage;
                                }

                                // Tạo SubCategory mới với URL ảnh thay vì Resource ID
                                SubCategory subCat = new SubCategory(type, -1); // Dùng -1 cho iconResId
                                subCat.imageUrl = imageUrl; // Đặt URL ảnh mới
                                subCategoryMap.put(type, subCat);
                            }
                        }

                        // Thêm tất cả SubCategory đã được ánh xạ vào danh sách
                        subCategoryList.addAll(subCategoryMap.values());

                        // Cập nhật Adapter
                        subCategoryAdapter = new SubCategoryAdapter(subCategoryList, category);
                        recyclerView.setAdapter(subCategoryAdapter);
                        subCategoryAdapter.notifyDataSetChanged();

                    } else {
                        Log.e(TAG, "Lỗi khi tải danh sách Sub-Category:", task.getException());
                    }
                });
    }
}