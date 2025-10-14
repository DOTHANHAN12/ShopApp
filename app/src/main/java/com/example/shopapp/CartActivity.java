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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartActionListener {

    private static final String TAG = "CartActivity";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private final List<CartItem> cartItemList = new ArrayList<>();

    private TextView textCheckoutTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        // Fix Status Bar icons cho nền trắng
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        mapViews();
        setupRecyclerView();
        loadCartItems();
    }

    private void mapViews() {
        recyclerView = findViewById(R.id.recycler_cart_items);
        textCheckoutTotal = findViewById(R.id.text_checkout_total);
        ImageView imgBack = findViewById(R.id.img_back_cart);

        imgBack.setOnClickListener(v -> finish());
        // TODO: Ánh xạ và xử lý các nút Checkout, Coupon nếu cần
    }

    private void setupRecyclerView() {
        adapter = new CartAdapter(this, cartItemList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadCartItems() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để xem giỏ hàng.", Toast.LENGTH_LONG).show();
            return;
        }

        db.collection("users").document(user.getUid()).collection("cart")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cartItemList.clear();
                        double total = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CartItem item = document.toObject(CartItem.class);
                            cartItemList.add(item);
                            total += item.getPriceAtTimeOfAdd() * item.getQuantity();
                        }
                        adapter.notifyDataSetChanged();
                        updateCartTotal(total);
                    } else {
                        Log.e(TAG, "Lỗi tải giỏ hàng: ", task.getException());
                        Toast.makeText(this, "Lỗi tải giỏ hàng.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateCartTotal(double total) {
        textCheckoutTotal.setText(String.format(Locale.getDefault(), "%,.0f VND", total));
        // TODO: Cần cập nhật các TextView khác trong Order Summary
    }

    // --- TRIỂN KHAI INTERFACE CART ADAPTER ---
    @Override
    public void onItemDeleted(CartItem item) {
        // Logic xóa khỏi Firestore
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).collection("cart").document(item.getProductId() + "_" + item.getVariantId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã xóa sản phẩm khỏi giỏ hàng.", Toast.LENGTH_SHORT).show();
                        loadCartItems(); // Tải lại danh sách
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xóa item.", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        // TODO: Logic cập nhật số lượng trong Firestore
    }
}