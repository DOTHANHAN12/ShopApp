package com.example.shopapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductListActivity extends AppCompatActivity {

    private static final String TAG = "ProductListAct";
    private static final String PRODUCTS_COLLECTION = "products";
    private FirebaseFirestore db;

    private TextView categoryHeader;
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> productList;

    private String currentCategory;
    private String currentSubCategory;

    private TextView tabWomen, tabMen, tabKids, tabBaby;
    private ImageView imgBackList;
    private TextView currentSelectedTab;

    private EditText edtSearchKeyword;
    private ImageView imgSearchFilter;
    private ImageView imgSearchIcon; // *** ĐÃ THÊM: Icon kính lúp (search icon) ***

    private ImageView navHomeFloat;
    private View searchButtonFloat;
    private ImageView navProfileFloat;

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

        setContentView(R.layout.activity_product_list);

        // 1. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();

        // 2. Lấy Category VÀ Sub-category từ Intent
        currentCategory = getIntent().getStringExtra("CATEGORY_KEY");
        currentSubCategory = getIntent().getStringExtra("TYPE_KEY");

        // --- XỬ LÝ SHOW ALL ---
        if (currentSubCategory != null && currentSubCategory.equals(SubCategory.SHOW_ALL_TYPE)) {
            currentSubCategory = null;
        }

        if (currentCategory == null) {
            currentCategory = "MEN";
        }

        // 3. Khởi tạo UI và Ánh xạ Views
        mapViews();
        setupCategoryTabs();
        setupSearchListener();
        setupFooterNavigation();

        // 4. Thiết lập nút Back
        if (imgBackList != null) {
            imgBackList.setOnClickListener(v -> finish());
        }

        // Cập nhật tiêu đề hiển thị cả Sub-category
        updateHeaderAndData(currentCategory, currentSubCategory);

        // Cập nhật UI Tab để làm nổi bật tab đã chọn
        updateCategoryUI(currentCategory);

        // 5. Thiết lập RecyclerView
        productList = new ArrayList<>();
        adapter = new ProductAdapter(this, productList);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // 6. Tải dữ liệu đã được gọi trong updateHeaderAndData
    }

    private void mapViews() {
        categoryHeader = findViewById(R.id.text_category_header);
        recyclerView = findViewById(R.id.recycler_product_list);

        // Ánh xạ Tabs và nút Back
        tabWomen = findViewById(R.id.tab_women_list);
        tabMen = findViewById(R.id.tab_men_list);
        tabKids = findViewById(R.id.tab_kids_list);
        tabBaby = findViewById(R.id.tab_baby_list);
        imgBackList = findViewById(R.id.img_back_list);

        // ÁNH XẠ SEARCH BAR
        edtSearchKeyword = findViewById(R.id.edt_search_keyword);
        imgSearchFilter = findViewById(R.id.img_search_filter);
        imgSearchIcon = findViewById(R.id.img_search_icon); // *** ĐÃ THÊM ***

        // ÁNH XẠ CÁC NÚT TỪ FOOTER
        navHomeFloat = findViewById(R.id.nav_home_cs);
        searchButtonFloat = findViewById(R.id.btn_search_footer);
//        navProfileFloat = findViewById(R.id.nav_profile_detail);
    }

    // --------------------------------------------------------------------------------
    // LOGIC TÌM KIẾM
    // --------------------------------------------------------------------------------
    private void setupSearchListener() {

        if (edtSearchKeyword != null) {
            // Lắng nghe sự kiện ENTER/SEARCH trên bàn phím
            edtSearchKeyword.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    performSearch();
                    return true;
                }
                return false;
            });
        }

        // *** ĐÃ SỬA: Lắng nghe sự kiện click vào ICON KÍNH LÚP (BÊN TRÁI) ***
        if (imgSearchIcon != null) {
            imgSearchIcon.setOnClickListener(v -> {
                performSearch();
            });
        }

        if (imgSearchFilter != null) {
            imgSearchFilter.setOnClickListener(v -> {
                // Ta coi việc bấm Filter là trigger Search với từ khóa hiện tại
                performSearch();
            });
        }
    }

    private void performSearch() {
        String keyword = edtSearchKeyword.getText().toString().trim();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edtSearchKeyword.getWindowToken(), 0);

        // Tải dữ liệu với từ khóa (tìm kiếm chung trong category đó)
        loadProductsFromFirestore(currentCategory, null, keyword);
    }

    // --------------------------------------------------------------------------------
    // LOGIC NAVIGATION
    // --------------------------------------------------------------------------------

    private void setupFooterNavigation() {
        // NÚT HOME: Chuyển về HomeActivity (clear stack)
        if (navHomeFloat != null) {
            navHomeFloat.setOnClickListener(v -> {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            });
        }

        // NÚT SEARCH: Chuyển về CategorySearchActivity
        if (searchButtonFloat != null) {
            searchButtonFloat.setOnClickListener(v -> {
                Intent intent = new Intent(this, CategorySearchActivity.class);
                intent.putExtra("CATEGORY_KEY", currentCategory);
                startActivity(intent);
            });
        }

        // NÚT PROFILE: Chuyển về MainActivity (Login/Register)
        if (navProfileFloat != null) {
            navProfileFloat.setOnClickListener(v -> {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            });
        }
    }


    private void setupCategoryTabs() {
        View.OnClickListener tabClickListener = view -> {
            String newCategory = ((TextView) view).getText().toString();

            updateCategoryUI(newCategory);

            // Gửi Intent để chuyển về CategorySearchActivity với Category mới (và kết thúc Activity hiện tại)
            Intent intent = new Intent(this, CategorySearchActivity.class);
            intent.putExtra("CATEGORY_KEY", newCategory);
            startActivity(intent);
            finish();
        };

        tabWomen.setOnClickListener(tabClickListener);
        tabMen.setOnClickListener(tabClickListener);
        tabKids.setOnClickListener(tabClickListener);
        tabBaby.setOnClickListener(tabClickListener);
    }

    // --------------------------------------------------------------------------------
    // DATA LOADING & UI UPDATES
    // --------------------------------------------------------------------------------

    private void updateHeaderAndData(String category, String subCategory) {
        String headerText;

        if (subCategory != null && !subCategory.isEmpty()) {
            headerText = subCategory.toUpperCase(Locale.ROOT);
        } else {
            headerText = category.toUpperCase(Locale.ROOT) + " Collection";
        }

        categoryHeader.setText(headerText);

        // Load data ban đầu (không có keyword)
        loadProductsFromFirestore(category, subCategory, null);
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

        // Thiết lập trạng thái mới: Gạch chân và In đậm
        currentSelectedTab.setAlpha(1.0f);
        currentSelectedTab.setPaintFlags(currentSelectedTab.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        currentSelectedTab.setTextSize(18);
    }

    /** * Tải danh sách sản phẩm từ Firestore dựa trên Category, Sub-Category và Keyword
     */
    private void loadProductsFromFirestore(String category, String subCategory, String keyword) {

        String finalCategory = category.toUpperCase(Locale.ROOT);
        String finalSubCategory = (subCategory != null && !subCategory.isEmpty()) ? subCategory.toUpperCase(Locale.ROOT) : null;

        Log.d(TAG, "--- BẮT ĐẦU TRUY VẤN SẢN PHẨM ---");

        // BƯỚC 1: BẮT ĐẦU VỚI LỌC CATEGORY
        Query query = db.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("category", finalCategory);

        // BƯỚC 2: BỔ SUNG LỌC THEO TYPE
        if (finalSubCategory != null && !finalSubCategory.isEmpty()) {
            query = query.whereEqualTo("type", finalSubCategory);
        }

        // BƯỚC 3: XỬ LÝ LỌC VÀ SẮP XẾP DỰA TRÊN KEYWORD
        if (keyword != null && !keyword.isEmpty()) {
            String endKeyword = keyword + "";

            // Khi có KEYWORD, ta buộc phải sắp xếp theo 'name' để tìm kiếm tiền tố
            // CẦN INDEX TỔNG HỢP: (category, type, name) hoặc (category, name)
            query = query.orderBy("name")
                    .whereGreaterThanOrEqualTo("name", keyword)
                    .whereLessThan("name", endKeyword);

        } else {
            // TRƯỜNG HỢP MẶC ĐỊNH (SHOW ALL / LỌC TYPE):
            query = query.orderBy("basePrice", Query.Direction.ASCENDING);
        }

        query.limit(50)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Log.d(TAG, "--- KẾT THÚC TRUY VẤN SẢN PHẨM ---");
                        if (task.isSuccessful()) {
                            productList.clear();
                            int count = 0;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    Product product = document.toObject(Product.class);
                                    productList.add(product);
                                    count++;

                                    Log.d(TAG, "DOC FOUND: " + document.getId() + ", CATE: " + product.getCategory() + ", TYPE: " + product.getType());

                                } catch (Exception e) {
                                    Log.e(TAG, "Lỗi khi chuyển đổi Document ID: " + document.getId(), e);
                                    Toast.makeText(ProductListActivity.this, "Lỗi định dạng dữ liệu (Debug log)", Toast.LENGTH_SHORT).show();
                                }
                            }

                            adapter.notifyDataSetChanged();

                            if (count == 0) {
                                Log.w(TAG, "RỖNG: Không tìm thấy sản phẩm nào khớp với điều kiện lọc.");
                                Toast.makeText(ProductListActivity.this,
                                        "Không tìm thấy sản phẩm nào.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ProductListActivity.this,
                                        "Đã tải thành công " + count + " sản phẩm.",
                                        Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            Log.e(TAG, "Lỗi truy vấn Firestore: ", task.getException());
                            Toast.makeText(ProductListActivity.this,
                                    "❌ Lỗi tải dữ liệu: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}