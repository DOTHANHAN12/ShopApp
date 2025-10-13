package com.example.shopapp;

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
import java.util.Locale;

public class CategorySearchActivity extends AppCompatActivity {

    private static final String TAG = "CategorySearchAct";
    private FirebaseFirestore db;

    private String currentCategory;
    private TextView tabWomen, tabMen, tabKids, tabBaby;
    private TextView currentSelectedTab;
    private EditText edtSearchKeyword;
    private ImageView navHome; // <--- Đã có khai báo

    private RecyclerView recyclerView;
    private SubCategoryAdapter subCategoryAdapter;
    private final List<SubCategory> subCategoryList = new ArrayList<>();

    private final Map<String, Map<String, String>> categoryTypeImages = new HashMap<>();

    private static final String SHOW_ALL_TYPE = SubCategory.SHOW_ALL_TYPE;
    private static final String SHOW_ALL_IMAGE_URL = SubCategory.SHOW_ALL_IMAGE_URL;


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

        setContentView(R.layout.activity_category_search);

        db = FirebaseFirestore.getInstance();

        initializeFixedImages();

        currentCategory = getIntent().getStringExtra("CATEGORY_KEY");
        if (currentCategory == null) {
            currentCategory = "WOMEN";
        }

        // 1. Ánh xạ Views và Footer (Đã gộp)
        mapViews();

        // 2. Khởi tạo RecyclerView và Adapter
        recyclerView = findViewById(R.id.recycler_category_grid);
        subCategoryAdapter = new SubCategoryAdapter(subCategoryList, currentCategory);

        // Thiết lập GridLayout 2 cột
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(subCategoryAdapter);

        // 3. Khởi tạo UI và Listener
        updateCategoryUI(currentCategory);
        setupCategoryTabs();
        setupSearchBarListener();

        // 4. Tải dữ liệu Sub Category từ Firestore
        loadSubCategoryData(currentCategory);
    }

    /**
     * Khởi tạo Map chứa URL ảnh Fix Cứng cho từng TYPE theo từng CATEGORY
     */
    private void initializeFixedImages() {
        Map<String, String> commonTypes = new HashMap<>();
        commonTypes.put("OUTERWEAR", "URL_OUTERWEAR_DEFAULT");
        commonTypes.put("SWEATERS & KNITWEAR", "URL_SWEATERS_DEFAULT");
        commonTypes.put("BOTTOMS", "URL_BOTTOMS_DEFAULT");
        commonTypes.put("T-SHIRTS, SWEAT & FLEECE", "URL_TSHIRTS_DEFAULT");
        commonTypes.put("INNERWEAR & UNDERWEAR", "URL_INNERWEAR_DEFAULT");
        commonTypes.put("ACCESSORIES", "https://i.pinimg.com/736x/e6/85/5b/e6855b431c74ca89257d0605de878943.jpg");

        Map<String, String> womenTypes = new HashMap<>(commonTypes);
        womenTypes.put("BOTTOMS", "URL_BOTTOMS_WOMEN_RIENG_BIET");
        womenTypes.put("DRESSES", "URL_DRESSES_WOMEN");
        womenTypes.put("MATERNITY CLOTHES", "URL_MATERNITY_WOMEN");
        womenTypes.put("OUTERWEAR", "URL_OUTERWEAR_WOMEN_SPECIAL");
        categoryTypeImages.put("WOMEN", womenTypes);

        Map<String, String> menTypes = new HashMap<>(commonTypes);
        menTypes.put("BOTTOMS", "URL_BOTTOMS_MAN_RIENG_BIET");
        menTypes.put("SPORT UTILITY WEAR", "URL_SPORT_MAN");
        categoryTypeImages.put("MEN", menTypes);

        Map<String, String> kidsTypes = new HashMap<>(commonTypes);
        categoryTypeImages.put("KIDS", kidsTypes);

        Map<String, String> babyTypes = new HashMap<>(commonTypes);
        categoryTypeImages.put("BABY", babyTypes);
    }

    // GỘP LOGIC ÁNH XẠ CHÍNH VÀ FOOTER
    private void mapViews() {
        tabWomen = findViewById(R.id.tab_women_cs);
        tabMen = findViewById(R.id.tab_men_cs);
        tabKids = findViewById(R.id.tab_kids_cs);
        tabBaby = findViewById(R.id.tab_baby_cs);
        edtSearchKeyword = findViewById(R.id.edt_search_keyword);

        // Ánh xạ nút Home từ Footer
        navHome = findViewById(R.id.nav_home_cs);

        // THIẾT LẬP LISTENER CHO NÚT HOME (ĐÃ FIX)
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(CategorySearchActivity.this, HomeActivity.class);
                // Cờ để đảm bảo quay về màn hình Home gốc
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    // HÀM mapViewsFooter() ĐÃ BỊ XÓA BỎ VÀ GỘP VÀO mapViews()


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

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String type = document.getString("type");
                            if (type != null) {
                                uniqueTypesFromDB.add(type.toUpperCase(Locale.ROOT));
                            }
                        }

                        Map<String, String> fixedImageMap = categoryTypeImages.getOrDefault(category, new HashMap<>());

                        // 2. Thêm mục "SHOW ALL" vào đầu danh sách
                        subCategoryList.add(new SubCategory(SHOW_ALL_TYPE, SHOW_ALL_IMAGE_URL));

                        // 3. Lọc và gán ảnh
                        for (Map.Entry<String, String> entry : fixedImageMap.entrySet()) {
                            String typeName = entry.getKey();
                            String imageUrl = entry.getValue();

                            // CHỈ thêm Type nếu nó có trong DB
                            if (uniqueTypesFromDB.contains(typeName.toUpperCase(Locale.ROOT))) {
                                subCategoryList.add(new SubCategory(typeName, imageUrl));
                            }
                        }

                        // 4. Cập nhật Adapter
                        subCategoryAdapter = new SubCategoryAdapter(subCategoryList, category);
                        recyclerView.setAdapter(subCategoryAdapter);
                        subCategoryAdapter.notifyDataSetChanged();

                    } else {
                        Log.e(TAG, "Lỗi khi tải danh sách Sub-Category:", task.getException());
                        Toast.makeText(CategorySearchActivity.this, "Lỗi tải danh mục con.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}