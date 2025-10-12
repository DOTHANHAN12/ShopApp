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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

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

    // Khai báo Map ảnh: Category -> (Type -> URL)
    private final Map<String, Map<String, String>> categoryTypeImages = new HashMap<>();

    // Lấy hằng số từ Model
    private static final String SHOW_ALL_TYPE = SubCategory.SHOW_ALL_TYPE;
    private static final String SHOW_ALL_IMAGE_URL = SubCategory.SHOW_ALL_IMAGE_URL;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_search);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = FirebaseFirestore.getInstance();

        // Khởi tạo Map URL ảnh Fix Cứng
        initializeFixedImages();

        currentCategory = getIntent().getStringExtra("CATEGORY_KEY");
        if (currentCategory == null) {
            currentCategory = "WOMEN";
        }

        mapViews();
        mapViewsFooter();

        // Khởi tạo RecyclerView và Adapter
        recyclerView = findViewById(R.id.recycler_category_grid);
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

    /**
     * Khởi tạo Map chứa URL ảnh Fix Cứng cho từng TYPE theo từng CATEGORY
     */
    // Trong CategorySearchActivity.java

    // Trong CategorySearchActivity.java

    /**
     * Khởi tạo Map chứa URL ảnh Fix Cứng cho từng TYPE theo từng CATEGORY
     */
    private void initializeFixedImages() {
        // ------------------- DỮ LIỆU CHUNG (DEFAULT) -------------------
        Map<String, String> commonTypes = new HashMap<>();
        commonTypes.put("OUTERWEAR", "URL_OUTERWEAR_DEFAULT");
        commonTypes.put("SWEATERS & KNITWEAR", "URL_SWEATERS_DEFAULT");
        commonTypes.put("BOTTOMS", "URL_BOTTOMS_DEFAULT"); // Ảnh mặc định cho các Category khác
        commonTypes.put("T-SHIRTS, SWEAT & FLEECE", "URL_TSHIRTS_DEFAULT");
        commonTypes.put("INNERWEAR & UNDERWEAR", "URL_INNERWEAR_DEFAULT");
        commonTypes.put("ACCESSORIES", "URL_ACCESSORIES_DEFAULT");

        // ------------------- WOMEN -------------------
        Map<String, String> womenTypes = new HashMap<>(commonTypes); // BẮT ĐẦU VỚI CÁC TYPE CHUNG

        // Ghi đè ảnh BOTTOMS của WOMEN
        womenTypes.put("BOTTOMS", "https://i.pinimg.com/736x/b2/65/10/b2651094216e49852c57c125eddcab83.jpg");

        // Thêm các Type riêng của WOMEN
        womenTypes.put("DRESSES", "URL_DRESSES_WOMEN");
        womenTypes.put("MATERNITY CLOTHES", "URL_MATERNITY_WOMEN");

        categoryTypeImages.put("WOMEN", womenTypes);

        // ------------------- MAN -------------------
        Map<String, String> menTypes = new HashMap<>(commonTypes); // BẮT ĐẦU VỚI CÁC TYPE CHUNG

        // Ghi đè ảnh BOTTOMS của MAN
        menTypes.put("BOTTOMS", "https://i.pinimg.com/736x/22/d1/05/22d10514c9e010d94e4fc6bc20bdf0aa.jpg");
        menTypes.put("ACCESSORIES", "https://i.pinimg.com/736x/22/d1/05/22d10514c9e010d94e4fc6bc20bdf0aa.jpg");
        // Thêm các Type riêng của MAN
        menTypes.put("SPORT UTILITY WEAR", "URL_SPORT_MAN");

        categoryTypeImages.put("MEN", menTypes);

        // ------------------- KIDS -------------------
        // SỬA LỖI: Cần sao chép các Type chung
        Map<String, String> kidsTypes = new HashMap<>(commonTypes);
        categoryTypeImages.put("KIDS", kidsTypes);

        // ------------------- BABY -------------------
        // SỬA LỖI: Cần sao chép các Type chung
        Map<String, String> babyTypes = new HashMap<>(commonTypes);
        categoryTypeImages.put("BABY", babyTypes);

        // -------------------------------------------------------------------
        // Vui lòng thay thế tất cả các placeholder URL (URL_...) bằng URL ảnh THẬT của bạn.
        // -------------------------------------------------------------------
    }

    private void mapViews() {
        tabWomen = findViewById(R.id.tab_women_cs);
        tabMen = findViewById(R.id.tab_men_cs);
        tabKids = findViewById(R.id.tab_kids_cs);
        tabBaby = findViewById(R.id.tab_baby_cs);
        edtSearchKeyword = findViewById(R.id.edt_search_keyword);
    }

    private void mapViewsFooter() {
        navHome = findViewById(R.id.nav_home_cs);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(CategorySearchActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupCategoryTabs() {
        View.OnClickListener tabClickListener = view -> {
            String category = ((TextView) view).getText().toString();

            updateCategoryUI(category);
            currentCategory = category;

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
        edtSearchKeyword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductListActivity.class);
            intent.putExtra("CATEGORY_KEY", currentCategory);
            startActivity(intent);
        });

        findViewById(R.id.img_search_filter).setOnClickListener(v -> {
            // Logic mở màn hình Filter
        });
    }

    /**
     * Tải danh sách các TYPE (Danh mục con) duy nhất từ Collection 'products'
     * và gán URL ảnh fix cứng cho mỗi Type, bao gồm mục SHOW ALL.
     */
    private void loadSubCategoryData(String category) {
        db.collection("products")
                .whereEqualTo("category", category)
                .limit(50)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        subCategoryList.clear();
                        Set<String> uniqueTypesFromDB = new HashSet<>();

                        // 1. Lấy danh sách Type THẬT sự có sản phẩm trong DB
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String type = document.getString("type");
                            if (type != null) {
                                uniqueTypesFromDB.add(type.toUpperCase());
                            }
                        }

                        // Lấy Map ảnh fix cứng cho Category hiện tại
                        Map<String, String> fixedImageMap = categoryTypeImages.getOrDefault(category, new HashMap<>());

                        // 2. Thêm mục "SHOW ALL" vào đầu danh sách
                        subCategoryList.add(new SubCategory(SHOW_ALL_TYPE, SHOW_ALL_IMAGE_URL));

                        // 3. Lọc và gán ảnh
                        for (Map.Entry<String, String> entry : fixedImageMap.entrySet()) {
                            String typeName = entry.getKey();
                            String imageUrl = entry.getValue();

                            // CHỈ thêm Type nếu nó có trong DB
                            if (uniqueTypesFromDB.contains(typeName.toUpperCase())) {
                                subCategoryList.add(new SubCategory(typeName, imageUrl));
                            }
                        }

                        // 4. Cập nhật Adapter (Quan trọng: Cập nhật Adapter sau khi list thay đổi)
                        subCategoryAdapter = new SubCategoryAdapter(subCategoryList, category);
                        recyclerView.setAdapter(subCategoryAdapter);
                        subCategoryAdapter.notifyDataSetChanged();

                    } else {
                        Log.e(TAG, "Lỗi khi tải danh sách Sub-Category:", task.getException());
                    }
                });
    }
}