package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
    private final List<Order> filteredOrderList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private LinearLayout layoutEmptyOrders;
    private ProgressBar progressLoading;
    private Button btnStartShopping;
    private TextView textEmptySubtitle;

    // ChipGroup for filters
    private ChipGroup chipGroupFilter;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        setupNavigation();
        setupFilters();

        loadOrders();
    }

    private void initViews() {
        recyclerOrders = findViewById(R.id.recycler_orders);
        layoutEmptyOrders = findViewById(R.id.layout_empty_orders);
        progressLoading = findViewById(R.id.progress_loading);
        btnStartShopping = findViewById(R.id.btn_start_shopping);
        textEmptySubtitle = findViewById(R.id.text_empty_subtitle);
        chipGroupFilter = findViewById(R.id.chip_group_filter);

        btnStartShopping.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    private void setupNavigation() {
        ImageView cartButton = findViewById(R.id.ic_cart);
        if (cartButton != null) {
            cartButton.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }

        ImageView homeButton = findViewById(R.id.nav_home_cs);
        if (homeButton != null) {
            homeButton.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
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
        adapter = new OrderHistoryAdapter(filteredOrderList, this);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrders.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentFilter = "ALL";
            } else {
                int checkedId = checkedIds.get(0);
                Chip chip = findViewById(checkedId);
                if (chip != null) {
                    currentFilter = chip.getTag().toString();
                }
            }
            filterOrders(currentFilter);
        });
    }

    private void loadOrders() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.e(TAG, "Error: User not logged in, cannot load orders.");
            showEmptyState();
            return;
        }

        showLoading();
        Log.d(TAG, "Starting to load orders for userId: " + userId);

        db.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    hideLoading();
                    Log.d(TAG, "Successfully loaded orders! Count: " + queryDocumentSnapshots.size());
                    orderList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = document.toObject(Order.class);
                        order.setOrderId(document.getId());
                        orderList.add(order);
                        Log.d(TAG, "Added order: " + order.getOrderId() + " with status: " + order.getOrderStatus());
                    }
                    filterOrders(currentFilter);
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Log.e(TAG, "Error loading orders from Firestore", e);
                    showEmptyState();
                });
    }

    private void filterOrders(String filter) {
        filteredOrderList.clear();

        if ("ALL".equals(filter)) {
            filteredOrderList.addAll(orderList);
        } else {
            for (Order order : orderList) {
                String status = order.getOrderStatus();
                if (status != null) {
                    switch (filter) {
                        case "PENDING":
                            if (status.equalsIgnoreCase("PENDING") ||
                                    status.equalsIgnoreCase("CONFIRMED")) {
                                filteredOrderList.add(order);
                            }
                            break;
                        case "PROCESSING":
                            if (status.equalsIgnoreCase("PROCESSING")) {
                                filteredOrderList.add(order);
                            }
                            break;
                        case "SHIPPING":
                            if (status.equalsIgnoreCase("SHIPPING")) {
                                filteredOrderList.add(order);
                            }
                            break;
                        case "DELIVERED":
                            if (status.equalsIgnoreCase("DELIVERED") ||
                                    status.equalsIgnoreCase("COMPLETED") ||
                                    status.equalsIgnoreCase("PAID")) {
                                filteredOrderList.add(order);
                            }
                            break;
                        case "CANCELLED":
                            if (status.equalsIgnoreCase("CANCELLED") ||
                                    status.equalsIgnoreCase("REFUNDED")) {
                                filteredOrderList.add(order);
                            }
                            break;
                        case "FAILED_PAYMENT":
                            if (status.equalsIgnoreCase("FAILED_PAYMENT")) {
                                filteredOrderList.add(order);
                            }
                            break;
                    }
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredOrderList.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void showLoading() {
        progressLoading.setVisibility(View.VISIBLE);
        recyclerOrders.setVisibility(View.GONE);
        layoutEmptyOrders.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressLoading.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        layoutEmptyOrders.setVisibility(View.VISIBLE);
        recyclerOrders.setVisibility(View.GONE);

        // Update empty message based on filter
        if ("ALL".equals(currentFilter)) {
            textEmptySubtitle.setText("Bắt đầu mua sắm ngay!");
        } else {
            textEmptySubtitle.setText("Không có đơn hàng nào trong danh mục này");
        }
    }

    private void hideEmptyState() {
        layoutEmptyOrders.setVisibility(View.GONE);
        recyclerOrders.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload orders when returning to this activity
        loadOrders();
    }
}