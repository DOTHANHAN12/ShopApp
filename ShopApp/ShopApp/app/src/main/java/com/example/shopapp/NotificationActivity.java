package com.example.shopapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";

    private RecyclerView recyclerViewNotifications;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;

    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyStateLayout;
    private ImageView btnBack;
    private TextView tvMarkAllRead;
    private TextView tvTitle;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup UI
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_notification);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập để xem thông báo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Load notifications
        loadNotifications();
    }

    private void initViews() {
        recyclerViewNotifications = findViewById(R.id.recycler_view_notifications);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_notifications);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        btnBack = findViewById(R.id.btn_back_notification);
        tvMarkAllRead = findViewById(R.id.tv_mark_all_read);
        tvTitle = findViewById(R.id.tv_notification_title);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Mark all as read
        tvMarkAllRead.setOnClickListener(v -> markAllAsRead());

        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadNotifications();
            swipeRefreshLayout.setRefreshing(false);
        });

        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }

    private void setupRecyclerView() {
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList, this::onNotificationClick, this::onNotificationDelete);

        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(adapter);
    }

    private void loadNotifications() {
        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            NotificationModel notification = document.toObject(NotificationModel.class);
                            notificationList.add(notification);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification: " + document.getId(), e);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notifications", e);
                    Toast.makeText(this, "Lỗi khi tải thông báo", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void onNotificationClick(NotificationModel notification) {
        // Mark as read
        if (!notification.isRead()) {
            markAsRead(notification);
        }

        // Handle action based on actionType
        handleNotificationAction(notification);
    }

    private void handleNotificationAction(NotificationModel notification) {
        String actionType = notification.getActionType();
        String actionData = notification.getActionData();

        if (actionType == null || actionType.equals("NONE")) {
            return;
        }

        switch (actionType) {
            case "OPEN_ORDER":
                // Mở OrderDetailActivity
                if (actionData != null && !actionData.isEmpty()) {
                    Intent intent = new Intent(this, OrderDetailActivity.class);
                    intent.putExtra("ORDER_ID", actionData);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
                }
                break;

            case "OPEN_PRODUCT":
                // Mở ProductDetailActivity
                if (actionData != null && !actionData.isEmpty()) {
                    Intent intent = new Intent(this, ProductDetailActivity.class);
                    intent.putExtra("PRODUCT_ID", actionData);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                }
                break;

            case "OPEN_URL":
                // Mở URL trong browser
                if (actionData != null && !actionData.isEmpty()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(actionData));
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error opening URL", e);
                        Toast.makeText(this, "Không thể mở link", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Link không hợp lệ", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                Log.d(TAG, "Unknown action type: " + actionType);
                break;
        }
    }

    private void markAsRead(NotificationModel notification) {
        if (notification.getNotificationId() == null) return;

        db.collection("notifications")
                .document(notification.getNotificationId())
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> {
                    notification.setRead(true);
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Notification marked as read");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking notification as read", e);
                });
    }

    private void markAllAsRead() {
        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().update("isRead", true);
                    }

                    Toast.makeText(this, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
                    loadNotifications(); // Reload to update UI
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking all as read", e);
                    Toast.makeText(this, "Lỗi khi đánh dấu đã đọc", Toast.LENGTH_SHORT).show();
                });
    }

    private void onNotificationDelete(NotificationModel notification) {
        if (notification.getNotificationId() == null) return;

        db.collection("notifications")
                .document(notification.getNotificationId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    notificationList.remove(notification);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    Toast.makeText(this, "Đã xóa thông báo", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting notification", e);
                    Toast.makeText(this, "Lỗi khi xóa thông báo", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            recyclerViewNotifications.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerViewNotifications.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload notifications when returning to this activity
        loadNotifications();
    }
}