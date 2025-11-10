package com.example.shopapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductListActivity extends AppCompatActivity implements FilterSortBottomSheet.FilterApplyListener {

    private static final String TAG = "ProductListActivity";
    private FirebaseFirestore db;

    // UI Components
    private RecyclerView recyclerView;
    private EditText edtSearchKeyword;
    private ImageView imgSearchIcon, imgSearchFilter, imgBack;
    private TextView textCategoryHeader;
    private LinearLayout layoutActiveFilters;
    private TextView txtActiveFiltersCount;

    // Current filters and search
    private String currentCategory;
    private String currentType;
    private String searchKeyword;
    private int minPrice = 0;
    private int maxPrice = 10000000;
    private int minRating = 0;
    private String sortBy = "NEWEST";

    private List<Product> productList = new ArrayList<>();
    private ProductAdapter productAdapter;

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

        setContentView(R.layout.activity_product_list);

        db = FirebaseFirestore.getInstance();

        // Get intent extras
        currentCategory = getIntent().getStringExtra("CATEGORY_KEY");
        currentType = getIntent().getStringExtra("TYPE_KEY");
        searchKeyword = getIntent().getStringExtra("SEARCH_KEYWORD");

        if (currentCategory == null) {
            currentCategory = "WOMEN";
        }

        mapViews();
        setupListeners();
        loadProducts();
    }

    private void mapViews() {
        recyclerView = findViewById(R.id.recycler_product_list);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        edtSearchKeyword = findViewById(R.id.edt_search_keyword);
        imgSearchIcon = findViewById(R.id.img_search_icon);
        imgSearchFilter = findViewById(R.id.img_search_filter);
        imgBack = findViewById(R.id.img_back_list);
        textCategoryHeader = findViewById(R.id.text_category_header);
        layoutActiveFilters = findViewById(R.id.layout_active_filters);
        txtActiveFiltersCount = findViewById(R.id.txt_active_filters_count);

        // Set header text
        if (currentType != null && !currentType.isEmpty()) {
            textCategoryHeader.setText(currentType);
        } else if (searchKeyword != null && !searchKeyword.isEmpty()) {
            textCategoryHeader.setText("Kết quả tìm kiếm: " + searchKeyword);
        } else {
            textCategoryHeader.setText(currentCategory);
        }

        if (edtSearchKeyword != null && searchKeyword != null) {
            edtSearchKeyword.setText(searchKeyword);
        }
    }

    private void setupListeners() {
        // Back button
        if (imgBack != null) {
            imgBack.setOnClickListener(v -> finish());
        }

        // Search
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
            imgSearchIcon.setOnClickListener(v -> {
                String keyword = edtSearchKeyword.getText().toString().trim();
                performSearch(keyword);
            });
        }

        // Filter
        if (imgSearchFilter != null) {
            imgSearchFilter.setOnClickListener(v -> openFilterBottomSheet());
        }

        // Clear filters
        if (layoutActiveFilters != null) {
            layoutActiveFilters.setOnClickListener(v -> clearAllFilters());
        }
    }

    private void performSearch(String keyword) {
        if (!SearchValidator.isValidKeyword(keyword)) {
            Toast.makeText(this, SearchValidator.getErrorMessage(keyword), Toast.LENGTH_SHORT).show();
            return;
        }

        if (edtSearchKeyword != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edtSearchKeyword.getWindowToken(), 0);
        }

        searchKeyword = keyword;
        textCategoryHeader.setText("Kết quả tìm kiếm: " + keyword);
        currentType = null;
        loadProducts();
    }

    private void openFilterBottomSheet() {
        FilterSortBottomSheet bottomSheet = new FilterSortBottomSheet();
        bottomSheet.setFilterApplyListener(this);
        bottomSheet.show(getSupportFragmentManager(), "FilterSort");
    }

    @Override
    public void onFiltersApplied(int minPrice, int maxPrice, int minRating, String sortBy) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minRating = minRating;
        this.sortBy = sortBy;

        updateActiveFiltersDisplay();
        loadProducts();
    }

    @Override
    public void onFiltersReset() {
        minPrice = 0;
        maxPrice = 10000000;
        minRating = 0;
        sortBy = "NEWEST";

        clearActiveFiltersDisplay();
        loadProducts();
    }

    private void clearAllFilters() {
        minPrice = 0;
        maxPrice = 10000000;
        minRating = 0;
        sortBy = "NEWEST";
        searchKeyword = null;
        currentType = null;

        if (edtSearchKeyword != null) {
            edtSearchKeyword.setText("");
        }

        textCategoryHeader.setText(currentCategory);
        clearActiveFiltersDisplay();
        loadProducts();
    }

    private void updateActiveFiltersDisplay() {
        int activeFiltersCount = 0;
        StringBuilder filterText = new StringBuilder();

        if (minPrice > 0 || maxPrice < 10000000) {
            activeFiltersCount++;
            filterText.append("Giá: ").append(formatPrice(minPrice)).append(" - ").append(formatPrice(maxPrice)).append("\n");
        }

        if (minRating > 0) {
            activeFiltersCount++;
            filterText.append("Đánh giá ≥ ").append(minRating).append(" sao\n");
        }

        if (!sortBy.equals("NEWEST")) {
            activeFiltersCount++;
            filterText.append("Sắp xếp: ").append(getSortLabel(sortBy));
        }

        if (activeFiltersCount > 0) {
            if (layoutActiveFilters != null) {
                layoutActiveFilters.setVisibility(View.VISIBLE);
            }
            if (txtActiveFiltersCount != null) {
                txtActiveFiltersCount.setText(activeFiltersCount + " Bộ lọc đang hoạt động");
            }
        } else {
            clearActiveFiltersDisplay();
        }
    }

    private void clearActiveFiltersDisplay() {
        if (layoutActiveFilters != null) {
            layoutActiveFilters.setVisibility(View.GONE);
        }
    }

    private String formatPrice(int price) {
        if (price >= 1000000) {
            return String.format("%.1fM", price / 1000000.0);
        } else if (price >= 1000) {
            return String.format("%.0fK", price / 1000.0);
        }
        return String.valueOf(price);
    }

    private String getSortLabel(String sortBy) {
        switch (sortBy) {
            case "PRICE_LOW_TO_HIGH":
                return "Giá thấp đến cao";
            case "PRICE_HIGH_TO_LOW":
                return "Giá cao đến thấp";
            case "RATING":
                return "Đánh giá cao nhất";
            case "POPULAR":
                return "Phổ biến nhất";
            default:
                return "Mới nhất";
        }
    }

    /**
     * Load products from Firestore with applied filters and search
     */
    private void loadProducts() {
        Query query = db.collection("products")
                .whereEqualTo("status", "Active") // ✅ LỌC SẢN PHẨM ACTIVE
                .whereEqualTo("category", currentCategory);

        // Filter by type if specified
        if (currentType != null && !currentType.isEmpty() && !currentType.equals("SHOW_ALL")) {
            query = query.whereEqualTo("type", currentType);
        }

        // Execute query
        query.limit(500).get().addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (task.isSuccessful()) {
                List<Product> allProducts = new ArrayList<>();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        Product product = document.toObject(Product.class);
                        if (product != null) {
                            // Apply filters
                            if (applyFilters(product)) {
                                allProducts.add(product);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing product", e);
                    }
                }

                // Apply sorting
                sortProducts(allProducts);

                // Update UI
                productList = allProducts;
                if (productAdapter == null) {
                    productAdapter = new ProductAdapter(allProducts, this);
                    recyclerView.setAdapter(productAdapter);
                } else {
                    productAdapter.updateData(allProducts);
                }

                if (allProducts.isEmpty()) {
                    Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
                }

            } else {
                Log.e(TAG, "Error loading products", task.getException());
                Toast.makeText(this, "Lỗi tải sản phẩm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Apply all filters to a product
     */
    private boolean applyFilters(Product product) {
        if (product == null) {
            return false;
        }

        // Price filter
        double price = product.basePrice > 0 ? product.basePrice : 0;
        if (price < minPrice || price > maxPrice) {
            return false;
        }

        // Rating filter
        double rating = product.averageRating != null ? product.averageRating : 0;
        if (rating < minRating) {
            return false;
        }

        // Search keyword filter
        if (searchKeyword != null && !searchKeyword.isEmpty()) {
            String sanitizedKeyword = SearchValidator.sanitizeKeyword(searchKeyword).toLowerCase();
            String productName = product.name != null ? product.name.toLowerCase() : "";
            String productDesc = product.desc != null ? product.desc.toLowerCase() : "";
            String productBarcode = product.barcode != null ? product.barcode.toLowerCase() : "";

            if (!productName.contains(sanitizedKeyword) && 
                !productDesc.contains(sanitizedKeyword) &&
                !productBarcode.contains(sanitizedKeyword)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sort products based on selected sort option
     */
    private void sortProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        switch (sortBy) {
            case "PRICE_LOW_TO_HIGH":
                Collections.sort(products, (p1, p2) -> {
                    double price1 = p1.basePrice > 0 ? p1.basePrice : 0;
                    double price2 = p2.basePrice > 0 ? p2.basePrice : 0;
                    return Double.compare(price1, price2);
                });
                break;

            case "PRICE_HIGH_TO_LOW":
                Collections.sort(products, (p1, p2) -> {
                    double price1 = p1.basePrice > 0 ? p1.basePrice : 0;
                    double price2 = p2.basePrice > 0 ? p2.basePrice : 0;
                    return Double.compare(price2, price1);
                });
                break;

            case "RATING":
                Collections.sort(products, (p1, p2) -> {
                    double rating1 = p1.averageRating != null ? p1.averageRating : 0;
                    double rating2 = p2.averageRating != null ? p2.averageRating : 0;
                    return Double.compare(rating2, rating1);
                });
                break;

            case "POPULAR":
                Collections.sort(products, (p1, p2) -> {
                    long views1 = p1.totalReviews != null ? p1.totalReviews : 0;
                    long views2 = p2.totalReviews != null ? p2.totalReviews : 0;
                    return Long.compare(views2, views1);
                });
                break;

            case "NEWEST":
            default:
                Collections.sort(products, (p1, p2) -> {
                    long timestamp1 = p1.updatedAt != null ? p1.updatedAt : 0;
                    long timestamp2 = p2.updatedAt != null ? p2.updatedAt : 0;
                    return Long.compare(timestamp2, timestamp1);
                });
                break;
        }
    }
}