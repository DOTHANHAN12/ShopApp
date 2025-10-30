package com.example.shopapp;

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

    // *** SỬA ĐỔI: Ánh xạ cho icon kính lúp (search icon) và icon filter ***
    private ImageView imgSearchIcon;
    private ImageView imgSearchFilter;

    // Khai báo Footer Buttons
    private ImageView navHome;
    private ImageView navProfile;

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

        mapViews();
        setupSearchListener();
        setupFooterNavigation();

        // Khởi tạo RecyclerView và Adapter (SỬA LỖI: Truyền Context)
        recyclerView = findViewById(R.id.recycler_category_grid);
        subCategoryAdapter = new SubCategoryAdapter(subCategoryList, currentCategory, this);

        // Thiết lập GridLayout 2 cột
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(subCategoryAdapter);

        // Khởi tạo UI và Listener
        updateCategoryUI(currentCategory);
        setupCategoryTabs();

        // Tải dữ liệu Sub Category từ Firestore
        loadSubCategoryData(currentCategory);
    }

    private void initializeFixedImages() {
        Map<String, String> commonTypes = new HashMap<>();
        commonTypes.put("OUTERWEAR", "URL_OUTERWEAR_DEFAULT");
        commonTypes.put("SWEATERS & KNITWEAR", "URL_SWEATERS_DEFAULT");
        commonTypes.put("BOTTOMS", "URL_BOTTOMS_DEFAULT");
        commonTypes.put("T-SHIRTS, SWEAT & FLEECE", "URL_TSHIRTS_DEFAULT");
        commonTypes.put("INNERWEAR & UNDERWEAR", "URL_INNERWEAR_DEFAULT");
        commonTypes.put("ACCESSORIES", "URL_ACCESSORIES_DEFAULT");

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

    private void mapViews() {
        tabWomen = findViewById(R.id.tab_women_cs);
        tabMen = findViewById(R.id.tab_men_cs);
        tabKids = findViewById(R.id.tab_kids_cs);
        tabBaby = findViewById(R.id.tab_baby_cs);
        edtSearchKeyword = findViewById(R.id.edt_search_keyword);

        // *** ĐÃ SỬA: Ánh xạ icon search và filter ***
        imgSearchIcon = findViewById(R.id.img_search_icon);
        imgSearchFilter = findViewById(R.id.img_search_filter);

        // ÁNH XẠ FOOTER
        navHome = findViewById(R.id.nav_home_cs);
        navProfile = findViewById(R.id.nav_user_cs);
    }

    // --------------------------------------------------------------------------------
    // LOGIC FOOTER NAVIGATION
    // --------------------------------------------------------------------------------
    private void setupFooterNavigation() {
        // NÚT HOME
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(CategorySearchActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
        // NÚT PROFILE
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(CategorySearchActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupSearchListener() {

        // 1. Lắng nghe sự kiện ENTER/SEARCH trên bàn phím
        if (edtSearchKeyword != null) {

            edtSearchKeyword.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(edtSearchKeyword.getText().toString().trim());
                    return true;
                }
                return false;
            });
        }

        // 2. *** ĐÃ SỬA: Lắng nghe sự kiện click vào ICON KÍNH LÚP (BÊN TRÁI) ***
        if (imgSearchIcon != null) {
            imgSearchIcon.setOnClickListener(v -> {
                performSearch(edtSearchKeyword.getText().toString().trim());
            });
        }

        // 3. *** ĐÃ SỬA: Lắng nghe sự kiện click vào ICON FILTER (BÊN PHẢI) ***
        if (imgSearchFilter != null) {
            imgSearchFilter.setOnClickListener(v -> {
                // Có thể mở một Activity Filter hoặc coi đây là trigger Search
                performSearch(edtSearchKeyword.getText().toString().trim());
            });
        }
    }

    private void performSearch(String keyword) {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edtSearchKeyword.getWindowToken(), 0);

        // CHUYỂN SANG PRODUCTLISTACTIVITY VÀ TRUYỀN KEYWORD
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra("CATEGORY_KEY", currentCategory);
        intent.putExtra("SEARCH_KEYWORD", keyword);
        startActivity(intent);
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

    /**
     * Tải danh sách các TYPE (Danh mục con) duy nhất từ Collection 'products'
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
                            // LƯU Ý: Dữ liệu type được lưu trong DB phải khớp
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

                            // CHỈ thêm Type nếu nó có trong DB (Kiểm tra bằng chữ HOA)
                            if (uniqueTypesFromDB.contains(typeName.toUpperCase(Locale.ROOT))) {
                                subCategoryList.add(new SubCategory(typeName, imageUrl));
                            }
                        }

                        // 4. Cập nhật Adapter (SỬA LỖI: Truyền Context)
                        subCategoryAdapter = new SubCategoryAdapter(subCategoryList, category, this);
                        recyclerView.setAdapter(subCategoryAdapter);
                        subCategoryAdapter.notifyDataSetChanged();

                    } else {
                        Log.e(TAG, "Lỗi khi tải danh sách Sub-Category:", task.getException());
                        Toast.makeText(CategorySearchActivity.this, "Lỗi tải danh mục con.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
