package com.example.shopapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

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

    private static final String TAG = "ProductListActivity";
    private static final String PRODUCTS_COLLECTION = "products";
    private FirebaseFirestore db;

    private TextView categoryHeader;
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> productList;

    private String currentCategory;
    private String currentSubCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        // 1. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();

        // 2. Lấy Category VÀ Sub-category từ Intent
        currentCategory = getIntent().getStringExtra("CATEGORY_KEY");
        // ĐỌC THAM SỐ LỌC SUB-CATEGORY (TYPE_KEY)
        currentSubCategory = getIntent().getStringExtra("TYPE_KEY");

        // Thêm kiểm tra cho giá trị đặc biệt SHOW ALL
        if (currentSubCategory != null && currentSubCategory.equals(SubCategory.SHOW_ALL_TYPE)) {
            currentSubCategory = null; // Đặt về null để không lọc theo Type
        }

        if (currentCategory == null) {
            currentCategory = "MEN";
        }

        // 3. Khởi tạo UI
        categoryHeader = findViewById(R.id.text_category_header);
        recyclerView = findViewById(R.id.recycler_product_list);

        // Cập nhật tiêu đề hiển thị cả Sub-category nếu có
        String headerText = currentCategory.toUpperCase(Locale.ROOT) + " Collection";
        if (currentSubCategory != null && !currentSubCategory.isEmpty()) {
            headerText = currentCategory.toUpperCase(Locale.ROOT) + " / " + currentSubCategory.toUpperCase(Locale.ROOT);
        }
        categoryHeader.setText(headerText);

        // 4. Thiết lập RecyclerView
        productList = new ArrayList<>();
        adapter = new ProductAdapter(this, productList);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // 5. Tải dữ liệu, truyền cả Sub-category
        loadProductsFromFirestore(currentCategory, currentSubCategory);
    }

    /** * Tải danh sách sản phẩm từ Firestore dựa trên Category VÀ Sub-Category
     */
    private void loadProductsFromFirestore(String category, String subCategory) {

        // BƯỚC 1: BẮT ĐẦU VỚI TRUY VẤN CƠ BẢN (luôn lọc theo Category)
        Query query = db.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("category", category);

        // BƯỚC 2: BỔ SUNG THÊM LỌC THEO SUB-CATEGORY (type) NẾU subCategory KHÔNG NULL/EMPTY
        if (subCategory != null && !subCategory.isEmpty()) {
            // LỌC THÊM THEO TRƯỜNG "type"
            query = query.whereEqualTo("type", subCategory);
        }

        // BƯỚC 3: TIẾP TỤC SẮP XẾP VÀ GIỚI HẠN
        query.orderBy("currentPrice", Query.Direction.ASCENDING)
                .limit(20)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            productList.clear();
                            int count = 0;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    Product product = document.toObject(Product.class);
                                    productList.add(product);
                                    count++;
                                } catch (Exception e) {
                                    Log.e(TAG, "Lỗi khi chuyển đổi Document ID: " + document.getId(), e);
                                    Toast.makeText(ProductListActivity.this, "Lỗi định dạng dữ liệu (Debug log)", Toast.LENGTH_SHORT).show();
                                }
                            }

                            adapter.notifyDataSetChanged();

                            if (count == 0) {
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