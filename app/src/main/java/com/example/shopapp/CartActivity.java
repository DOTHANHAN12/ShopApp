package com.example.shopapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger; // Thêm import AtomicInteger

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartActionListener {

    private static final String TAG = "CartActivity";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private final List<CartItem> cartItemList = new ArrayList<>();

    private TextView textCheckoutTotal;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }

        mapViews();
        setupRecyclerView();
        loadCartItems();
    }

    private void mapViews() {
        recyclerView = findViewById(R.id.recycler_cart_items);
        textCheckoutTotal = findViewById(R.id.text_checkout_total);
        ImageView imgBack = findViewById(R.id.img_back_cart);

        imgBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new CartAdapter(this, cartItemList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    // *** SỬA ĐỔI: Tải chi tiết sản phẩm và biến thể ***
    private void loadCartItems() {
        if (userId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để xem giỏ hàng.", Toast.LENGTH_LONG).show();
            return;
        }

        db.collection("users").document(userId).collection("cart")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cartItemList.clear();
                        List<Task<Void>> detailTasks = new ArrayList<>();

                        for (QueryDocumentSnapshot cartDocument : task.getResult()) {
                            CartItem item = cartDocument.toObject(CartItem.class);

                            // Giả định các trường productId và variantId được lưu trong document
                            item.setProductId(cartDocument.getString("productId"));
                            item.setVariantId(cartDocument.getString("variantId"));

                            if (item.getProductId() != null && item.getVariantId() != null) {
                                cartItemList.add(item);

                                // Tạo Task tải chi tiết cho từng item
                                Task<Void> detailTask = loadProductDetailForCartItem(item);
                                detailTasks.add(detailTask);
                            }
                        }

                        // Chờ tất cả các Task tải chi tiết hoàn thành
                        Tasks.whenAllComplete(detailTasks)
                                .addOnCompleteListener(allTasks -> {
                                    // Sau khi tất cả chi tiết được tải, cập nhật giao diện
                                    adapter.notifyDataSetChanged();
                                    calculateCartTotal();
                                    Log.d(TAG, "Tải giỏ hàng hoàn thành với chi tiết sản phẩm.");
                                });

                    } else {
                        Log.e(TAG, "Lỗi tải giỏ hàng: ", task.getException());
                        Toast.makeText(this, "Lỗi tải giỏ hàng.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // *** HÀM MỚI: Tải chi tiết Product và Variant ***
    private Task<Void> loadProductDetailForCartItem(CartItem item) {
        // 1. Tải chi tiết Product
        Task<DocumentSnapshot> productTask = db.collection("products").document(item.getProductId()).get();

        return productTask.continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Product product = task.getResult().toObject(Product.class);
                item.setProductDetails(product);

                if (product != null && product.getVariants() != null && item.getVariantId() != null) {
                    // 2. Tìm chi tiết Variant từ danh sách Variants của Product
                    for (ProductVariant variant : product.getVariants()) {
                        if (variant.getVariantId() != null && variant.getVariantId().equals(item.getVariantId())) {
                            item.setVariantDetails(variant);
                            break;
                        }
                    }
                }
            } else {
                Log.w(TAG, "Không tìm thấy chi tiết Product cho ID: " + item.getProductId());
                // Nếu sản phẩm không tồn tại, bạn có thể xóa nó khỏi giỏ hàng hoặc đánh dấu để ẩn
            }
            return Tasks.forResult(null); // Trả về Task hoàn thành
        });
    }


    // *** Hàm tính tổng tiền (Sử dụng giá đã lưu) ***
    private void calculateCartTotal() {
        double total = 0;
        for (CartItem item : cartItemList) {
            // Sử dụng giá đã lưu trong CartItem (priceAtTimeOfAdd)
            total += item.getPriceAtTimeOfAdd() * item.getQuantity();
        }
        updateCartTotal(total);
    }

    private void updateCartTotal(double total) {
        textCheckoutTotal.setText(String.format(Locale.getDefault(), "%,.0f VND", total));
    }

    // --- TRIỂN KHAI INTERFACE CART ADAPTER ---

    @Override
    public void onCartUpdated() {
        calculateCartTotal();
    }

    @Override
    public void onItemDeleted(CartItem item) {
        if (userId == null) return;

        // Tạo Document ID: productId_variantId
        String documentId = item.getProductId() + "_" + item.getVariantId();

        db.collection("users").document(userId).collection("cart").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa sản phẩm khỏi giỏ hàng.", Toast.LENGTH_SHORT).show();
                    cartItemList.remove(item);
                    adapter.notifyDataSetChanged();
                    calculateCartTotal();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xóa item.", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        if (userId == null) return;

        // Tạo Document ID: productId_variantId
        String documentId = item.getProductId() + "_" + item.getVariantId();

        db.collection("users").document(userId).collection("cart").document(documentId)
                .update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cập nhật số lượng thành công.");
                    Toast.makeText(this, "Cập nhật số lượng thành công.", Toast.LENGTH_SHORT).show();

                    // Cập nhật local list và tính tổng tiền
                    item.setQuantity(newQuantity);
                    adapter.notifyDataSetChanged();
                    calculateCartTotal();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật số lượng.", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                });
    }
}