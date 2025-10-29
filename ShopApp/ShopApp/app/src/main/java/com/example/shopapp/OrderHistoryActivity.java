package com.example.shopapp;

import android.content.Intent;
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

    private static final String TAG = "OrderHistoryActivity";

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
        setupNavigation();

        loadOrders();
    }

    private void setupNavigation() {
        ImageView cartButton = findViewById(R.id.ic_cart);
        if (cartButton != null) {
            cartButton.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }

        ImageView homeButton = findViewById(R.id.nav_home_cs);
        if (homeButton != null) {
            homeButton.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        }

        ImageView userButton = findViewById(R.id.nav_user_cs);
        if (userButton != null) {
            userButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        ImageView backButton = findViewById(R.id.img_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        adapter = new OrderHistoryAdapter(orderList, this); // Pass context to adapter
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrders.setAdapter(adapter);
    }

    private void loadOrders() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.e(TAG, "Error: User not logged in, cannot load orders.");
            return;
        }

        Log.d(TAG, "Starting to load orders for userId: " + userId);

        db.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Successfully loaded orders! Count: " + queryDocumentSnapshots.size());
                    orderList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = document.toObject(Order.class);
                        order.setOrderId(document.getId());
                        orderList.add(order);
                        Log.d(TAG, "Added order: " + order.getOrderId() + " with status: " + order.getOrderStatus());
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading orders from Firestore", e);
                });
    }
}
