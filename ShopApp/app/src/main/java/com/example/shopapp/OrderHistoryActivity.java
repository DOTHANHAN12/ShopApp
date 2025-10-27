package com.example.shopapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private static final String TAG = "OrderHistoryActivity"; // Thêm TAG để lọc log

    private RecyclerView recyclerOrders;
    private OrderHistoryAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerOrders = findViewById(R.id.recycler_orders);
        setupRecyclerView();

        ImageView imgBack = findViewById(R.id.img_back);
        imgBack.setOnClickListener(v -> finish());

        loadOrders();
    }

    private void setupRecyclerView() {
        adapter = new OrderHistoryAdapter(orderList);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrders.setAdapter(adapter);
    }

    private void loadOrders() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.e(TAG, "Lỗi: Người dùng chưa đăng nhập, không thể tải đơn hàng.");
            return;
        }

        Log.d(TAG, "Bắt đầu tải đơn hàng cho userId: " + userId);

        db.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Tải đơn hàng thành công! Số lượng: " + queryDocumentSnapshots.size());
                    orderList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = document.toObject(Order.class);
                        order.setOrderId(document.getId());
                        orderList.add(order);
                        Log.d(TAG, "Đã thêm đơn hàng: " + order.getOrderId());
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải đơn hàng từ Firestore", e);
                });
    }
}
