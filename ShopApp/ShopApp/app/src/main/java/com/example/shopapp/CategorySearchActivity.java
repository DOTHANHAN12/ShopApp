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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class CategorySearchActivity extends AppCompatActivity {

    private static final String TAG = "CategorySearchAct";
    private FirebaseFirestore db;

    private String currentCategory;
    private TextView tabWomen, tabMen, tabKids, tabBaby;
    private TextView currentSelectedTab;
    private EditText edtSearchKeyword;

    private ImageView imgSearchIcon;
    private ImageView imgSearchFilter;

    private ImageView navHome, navProfile;

    private RecyclerView recyclerView;
    private final List<SubCategory> subCategoryList = new ArrayList<>();
    private final Map<String, Map<String, String>> categoryTypeImages = new HashMap<>();

    private static final String SHOW_ALL_TYPE = SubCategory.SHOW_ALL_TYPE;
    private static final String SHOW_ALL_IMAGE_URL = SubCategory.SHOW_ALL_IMAGE_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        if (currentCategory == null || currentCategory.isEmpty()) {
            currentCategory = "WOMEN"; // Default category
        }

        mapViews();
        setupSearchListener();
        setupFooterNavigation();

        recyclerView = findViewById(R.id.recycler_category_grid);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        updateCategoryUI(currentCategory);
        setupCategoryTabs();
        loadSubCategoryData(currentCategory);
    }

    private void initializeFixedImages() {
        // This part remains the same, defining available images
        Map<String, String> commonTypes = new HashMap<>();
        commonTypes.put("OUTERWEAR", "URL_OUTERWEAR_DEFAULT");
        commonTypes.put("SWEATERS & KNITWEAR", "URL_SWEATERS_DEFAULT");
        commonTypes.put("BOTTOMS", "URL_BOTTOMS_DEFAULT");
        commonTypes.put("T-SHIRTS, SWEAT & FLEECE", "URL_TSHIRTS_DEFAULT");
        commonTypes.put("INNERWEAR & UNDERWEAR", "URL_INNERWEAR_DEFAULT");
        commonTypes.put("ACCESSORIES", "URL_ACCESSORIES_DEFAULT");

        Map<String, String> womenTypes = new HashMap<>(commonTypes);
        womenTypes.put("DRESSES", "URL_DRESSES_WOMEN");
        womenTypes.put("MATERNITY CLOTHES", "URL_MATERNITY_WOMEN");
        categoryTypeImages.put("WOMEN", womenTypes);

        Map<String, String> menTypes = new HashMap<>(commonTypes);
        menTypes.put("SPORT UTILITY WEAR", "URL_SPORT_MAN");
        categoryTypeImages.put("MEN", menTypes);

        categoryTypeImages.put("KIDS", new HashMap<>(commonTypes));
        categoryTypeImages.put("BABY", new HashMap<>(commonTypes));
    }

    private void mapViews() {
        tabWomen = findViewById(R.id.tab_women_cs);
        tabMen = findViewById(R.id.tab_men_cs);
        tabKids = findViewById(R.id.tab_kids_cs);
        tabBaby = findViewById(R.id.tab_baby_cs);
        edtSearchKeyword = findViewById(R.id.edt_search_keyword);
        imgSearchIcon = findViewById(R.id.img_search_icon);
        imgSearchFilter = findViewById(R.id.img_search_filter);
        navHome = findViewById(R.id.nav_home_cs);
        navProfile = findViewById(R.id.nav_user_cs);
    }

    private void setupFooterNavigation() {
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }
    }

    private void setupSearchListener() {
        if (edtSearchKeyword != null) {
            edtSearchKeyword.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(v.getText().toString().trim());
                    return true;
                }
                return false;
            });
        }
        if (imgSearchIcon != null) {
            imgSearchIcon.setOnClickListener(v -> performSearch(edtSearchKeyword.getText().toString().trim()));
        }
        if (imgSearchFilter != null) {
            imgSearchFilter.setOnClickListener(v -> openFilterBottomSheet());
        }
    }

    /**
     * Validate và perform search
     */
    private void performSearch(String keyword) {
        // Validate keyword
        if (!isValidSearchKeyword(keyword)) {
            Toast.makeText(this, "Vui lòng nhập từ khóa hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (edtSearchKeyword != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edtSearchKeyword.getWindowToken(), 0);
        }

        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra("CATEGORY_KEY", currentCategory);
        intent.putExtra("SEARCH_KEYWORD", keyword);
        startActivity(intent);
    }

    /**
     * Validate search keyword
     */
    private boolean isValidSearchKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return false;
        }
        // Minimum 2 characters
        if (keyword.length() < 2) {
            return false;
        }
        // Maximum 100 characters
        if (keyword.length() > 100) {
            return false;
        }
        // Remove dangerous characters
        String sanitized = keyword.replaceAll("[<>\"'%;()&+]", "");
        return !sanitized.isEmpty();
    }

    /**
     * Open filter and sort bottom sheet
     */
    private void openFilterBottomSheet() {
        FilterSortBottomSheet bottomSheet = new FilterSortBottomSheet();
        bottomSheet.show(getSupportFragmentManager(), "FilterSort");
    }

    private void setupCategoryTabs() {
        View.OnClickListener tabClickListener = view -> {
            String categoryName = "";
            int id = view.getId();
            if (id == R.id.tab_women_cs) categoryName = "WOMEN";
            else if (id == R.id.tab_men_cs) categoryName = "MEN";
            else if (id == R.id.tab_kids_cs) categoryName = "KIDS";
            else if (id == R.id.tab_baby_cs) categoryName = "BABY";

            if (!categoryName.isEmpty() && !categoryName.equals(currentCategory)) {
                updateCategoryUI(categoryName);
                loadSubCategoryData(categoryName);
            }
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

        if ("WOMEN".equals(newCategory)) currentSelectedTab = tabWomen;
        else if ("MEN".equals(newCategory)) currentSelectedTab = tabMen;
        else if ("KIDS".equals(newCategory)) currentSelectedTab = tabKids;
        else if ("BABY".equals(newCategory)) currentSelectedTab = tabBaby;
        else return;

        currentSelectedTab.setAlpha(1.0f);
        currentSelectedTab.setPaintFlags(currentSelectedTab.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        currentSelectedTab.setTextSize(18);
        this.currentCategory = newCategory;
    }

    /**
     * ⭐️ REWRITTEN AND FIXED LOGIC ⭐️
     */
    private void loadSubCategoryData(String category) {
        db.collection("products")
                .whereEqualTo("category", category)
                .limit(200) // Increase limit to ensure all types are fetched
                .get()
                .addOnCompleteListener(task -> {
                    // Safety check to prevent crash if user leaves screen
                    if (isFinishing() || isDestroyed()) {
                        Log.w(TAG, "Activity is finishing, ignoring Firestore results.");
                        return;
                    }

                    if (task.isSuccessful()) {
                        // Use a Map to get unique types while preserving original casing
                        Map<String, String> uniqueTypes = new HashMap<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String type = document.getString("type");
                            if (type != null && !type.trim().isEmpty()) {
                                // Key: uppercase for uniqueness check. Value: original for display.
                                uniqueTypes.put(type.toUpperCase(Locale.ROOT), type);
                            }
                        }

                        // Prepare the final list
                        subCategoryList.clear();
                        subCategoryList.add(new SubCategory(SHOW_ALL_TYPE, SHOW_ALL_IMAGE_URL));

                        Map<String, String> fixedImageMap = categoryTypeImages.getOrDefault(category, new HashMap<>());

                        // Sort the original type names for a consistent order
                        List<String> sortedOriginalTypes = new ArrayList<>(uniqueTypes.values());
                        Collections.sort(sortedOriginalTypes);

                        for (String originalTypeName : sortedOriginalTypes) {
                            // Find image using case-insensitive search in the predefined map
                            String imageUrl = findImageForType(fixedImageMap, originalTypeName);
                            subCategoryList.add(new SubCategory(originalTypeName, imageUrl));
                        }

                        // Create a new adapter with the correct, consistent data and set it
                        SubCategoryAdapter adapter = new SubCategoryAdapter(subCategoryList, category, this);
                        recyclerView.setAdapter(adapter);

                    } else {
                        Log.e(TAG, "Error loading sub-category data for " + category, task.getException());
                        Toast.makeText(CategorySearchActivity.this, "Lỗi tải danh mục con.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Helper to find an image URL from the map using a case-insensitive key.
     */
    private String findImageForType(Map<String, String> imageMap, String originalTypeName) {
        for (Map.Entry<String, String> entry : imageMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(originalTypeName)) {
                return entry.getValue();
            }
        }
        return "URL_DEFAULT_IMAGE"; // Return a default placeholder if no specific image is found
    }
}