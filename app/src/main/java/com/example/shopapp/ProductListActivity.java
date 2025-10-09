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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Đảm bảo layout này đã tồn tại và không bị lỗi
        setContentView(R.layout.activity_product_list);

        // 1. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();

        // 2. Lấy Category từ Intent
        currentCategory = getIntent().getStringExtra("CATEGORY_KEY");
        if (currentCategory == null) {
            currentCategory = "MEN"; // Giá trị mặc định nếu không nhận được
        }

        // 3. Khởi tạo UI
        categoryHeader = findViewById(R.id.text_category_header);
        recyclerView = findViewById(R.id.recycler_product_list);

        categoryHeader.setText(currentCategory.toUpperCase(Locale.ROOT) + " Collection");

        // 4. Thiết lập RecyclerView
        productList = new ArrayList<>();
        adapter = new ProductAdapter(this, productList);

        // Dùng GridLayoutManager cho hiển thị danh sách dạng lưới 2 cột
        // Hoặc dùng LinearLayoutManager nếu muốn hiển thị dạng danh sách dọc
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // 5. Tải dữ liệu
        loadProductsFromFirestore(currentCategory);
    }

    /** * Tải danh sách sản phẩm từ Firestore dựa trên Category
     * Vấn đề thường gặp: Category không khớp (Case Sensitivity)
     */
    private void loadProductsFromFirestore(String category) {
        // KIỂM TRA LẠI: Tên trường trong Firestore có đúng là "category" (chữ thường) không?
        db.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("category", category) // Lọc theo Category
                .orderBy("currentPrice", Query.Direction.ASCENDING) // Sắp xếp theo giá
                .limit(20) // Giới hạn 20 sản phẩm
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            productList.clear(); // Xóa dữ liệu cũ (nếu có)
                            int count = 0;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    // Deserialization: Chuyển Document thành đối tượng Product
                                    Product product = document.toObject(Product.class);
                                    productList.add(product);
                                    count++;
                                } catch (Exception e) {
                                    // BẮT LỖI DESERIALIZATION - RẤT QUAN TRỌNG
                                    Log.e(TAG, "Lỗi khi chuyển đổi Document ID: " + document.getId(), e);
                                    Toast.makeText(ProductListActivity.this, "Lỗi định dạng dữ liệu (Debug log)", Toast.LENGTH_SHORT).show();
                                }
                            }

                            // Cập nhật RecyclerView
                            adapter.notifyDataSetChanged();

                            if (count == 0) {
                                Toast.makeText(ProductListActivity.this,
                                        "Không tìm thấy sản phẩm nào cho danh mục " + category,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ProductListActivity.this,
                                        "Đã tải thành công " + count + " sản phẩm.",
                                        Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            // BẮT LỖI KẾT NỐI FIRESTORE
                            Log.e(TAG, "Lỗi truy vấn Firestore: ", task.getException());
                            Toast.makeText(ProductListActivity.this,
                                    "❌ Lỗi tải dữ liệu: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}