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
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyReviewsActivity extends AppCompatActivity implements MyReviewsAdapter.OnReviewActionListener {

    private static final String TAG = "MyReviewsActivity";
    private static final long EDIT_TIME_LIMIT_HOURS = 12;

    private RecyclerView recyclerViewMyReviews;
    private MyReviewsAdapter reviewAdapter;
    private List<Review> reviewList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Elements
    private ProgressBar progressLoading;
    private LinearLayout layoutEmptyState;
    private Button btnStartShopping;
    private TextView textTotalReviews;
    private TextView textAvgRating;
    private TextView textEditableCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reviews);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        mapViews();
        setupRecyclerView();
        setupNavigation();

        loadMyReviews();
    }

    private void mapViews() {
        recyclerViewMyReviews = findViewById(R.id.recycler_view_my_reviews);
        progressLoading = findViewById(R.id.progress_loading);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        btnStartShopping = findViewById(R.id.btn_start_shopping);

        textTotalReviews = findViewById(R.id.text_total_reviews);
        textAvgRating = findViewById(R.id.text_avg_rating);
        textEditableCount = findViewById(R.id.text_editable_count);

        if (btnStartShopping != null) {
            btnStartShopping.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
        }
    }

    private void setupNavigation() {
        ImageView imgBack = findViewById(R.id.img_back);
        if (imgBack != null) {
            imgBack.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        reviewList = new ArrayList<>();
        reviewAdapter = new MyReviewsAdapter(reviewList, this);
        recyclerViewMyReviews.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMyReviews.setAdapter(reviewAdapter);
    }

    private void loadMyReviews() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để xem đánh giá.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();
        showLoading(true);

        db.collectionGroup("reviews")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        reviewList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Review review = document.toObject(Review.class);
                            reviewList.add(review);
                        }
                        reviewAdapter.notifyDataSetChanged();
                        updateStats();
                        updateEmptyState();
                    } else {
                        Log.e(TAG, "Error getting reviews: ", task.getException());
                        Toast.makeText(MyReviewsActivity.this, "Lỗi tải đánh giá.", Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        if (progressLoading != null) {
            progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewMyReviews != null) {
            recyclerViewMyReviews.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        if (layoutEmptyState != null) {
            if (reviewList.isEmpty()) {
                layoutEmptyState.setVisibility(View.VISIBLE);
                recyclerViewMyReviews.setVisibility(View.GONE);
            } else {
                layoutEmptyState.setVisibility(View.GONE);
                recyclerViewMyReviews.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateStats() {
        int totalReviews = reviewList.size();

        // Calculate average rating
        float totalRating = 0;
        int editableCount = 0;

        for (Review review : reviewList) {
            totalRating += review.getRating();

            // Check if review is still editable
            if (canEditReview(review)) {
                editableCount++;
            }
        }

        float avgRating = totalReviews > 0 ? totalRating / totalReviews : 0;

        // Update UI
        if (textTotalReviews != null) {
            textTotalReviews.setText(String.valueOf(totalReviews));
        }

        if (textAvgRating != null) {
            textAvgRating.setText(String.format("%.1f", avgRating));
        }

        if (textEditableCount != null) {
            textEditableCount.setText(String.valueOf(editableCount));
        }
    }

    private boolean canEditReview(Review review) {
        if (review.isEdited()) {
            return false; // Already edited once
        }

        long currentTime = System.currentTimeMillis();
        long reviewTime = review.getTimestamp();
        long hoursDifference = TimeUnit.MILLISECONDS.toHours(currentTime - reviewTime);

        return hoursDifference < EDIT_TIME_LIMIT_HOURS;
    }

    // =================================================================
    // INTERFACE CALLBACKS
    // =================================================================

    @Override
    public void onEditReview(Review review) {
        if (!canEditReview(review)) {
            String message = review.isEdited()
                    ? "Bạn đã chỉnh sửa đánh giá này rồi."
                    : "Đã quá thời hạn chỉnh sửa (12 giờ).";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, WriteReviewActivity.class);
        intent.putExtra("PRODUCT_ID", review.getProductId());
        intent.putExtra("ORDER_ID", review.getOrderId());
        intent.putExtra("EDIT_MODE", true);
        startActivity(intent);
    }

    @Override
    public void onViewProduct(Review review) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", review.getProductId());
        startActivity(intent);
    }

    @Override
    public void onViewDetails(Review review) {
        // Show dialog with full review details
        showReviewDetailsDialog(review);
    }

    @Override
    public void onDeleteReview(Review review) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa đánh giá")
                .setMessage("Bạn có chắc chắn muốn xóa đánh giá này không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteReview(review))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteReview(Review review) {
        // Note: This is a simplified version
        // In production, you'd need to also update product's average rating

        db.collection("products")
                .document(review.getProductId())
                .collection("reviews")
                .whereEqualTo("userId", review.getUserId())
                .whereEqualTo("orderId", review.getOrderId())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        queryDocumentSnapshots.getDocuments().get(0).getReference()
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đã xóa đánh giá.", Toast.LENGTH_SHORT).show();
                                    loadMyReviews(); // Reload list
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting review", e);
                                    Toast.makeText(this, "Lỗi xóa đánh giá.", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void showReviewDetailsDialog(Review review) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_review_details, null);

        // Setup dialog views
        TextView textProduct = dialogView.findViewById(R.id.text_product_name);
        TextView textRating = dialogView.findViewById(R.id.text_rating);
        TextView textComment = dialogView.findViewById(R.id.text_comment);
        TextView textDate = dialogView.findViewById(R.id.text_date);
        TextView textEdited = dialogView.findViewById(R.id.text_edited_status);

        // Set data
        textRating.setText(String.format("%.1f ⭐", review.getRating()));
        textComment.setText(review.getComment());
        textDate.setText(android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", review.getTimestamp()));

        if (review.isEdited()) {
            textEdited.setVisibility(View.VISIBLE);
            textEdited.setText("Đã chỉnh sửa: " +
                    android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", review.getUpdatedAt()));
        } else {
            textEdited.setVisibility(View.GONE);
        }

        // Load product name
        db.collection("products").document(review.getProductId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Product product = documentSnapshot.toObject(Product.class);
                        if (product != null) {
                            textProduct.setText(product.getName());
                        }
                    }
                });

        new AlertDialog.Builder(this)
                .setTitle("Chi tiết đánh giá")
                .setView(dialogView)
                .setPositiveButton("Đóng", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload reviews when coming back from edit screen
        loadMyReviews();
    }
}