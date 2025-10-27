package com.example.shopapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

public class WriteReviewActivity extends AppCompatActivity {

    private static final String TAG = "WriteReviewActivity";

    private RatingBar ratingBar;
    private EditText editTextComment;
    private Button btnSubmitReview;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ratingBar = findViewById(R.id.rating_bar_write_review);
        editTextComment = findViewById(R.id.edit_text_review_comment);
        btnSubmitReview = findViewById(R.id.btn_submit_review);

        productId = getIntent().getStringExtra("PRODUCT_ID");

        if (productId == null) {
            Toast.makeText(this, "Product ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to write a review.", Toast.LENGTH_SHORT).show();
            return;
        }

        float rating = ratingBar.getRating();
        String comment = editTextComment.getText().toString().trim();
        String userId = currentUser.getUid();
        String userName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous";

        if (rating == 0) {
            Toast.makeText(this, "Please provide a rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please write a comment.", Toast.LENGTH_SHORT).show();
            return;
        }

        Review review = new Review(userId, userName, rating, comment, System.currentTimeMillis());

        DocumentReference productRef = db.collection("products").document(productId);
        DocumentReference reviewRef = productRef.collection("reviews").document();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot productSnapshot = transaction.get(productRef);

            // Lấy thông tin xếp hạng hiện tại
            long totalReviews = productSnapshot.contains("totalReviews") ? productSnapshot.getLong("totalReviews") : 0;
            double averageRating = productSnapshot.contains("averageRating") ? productSnapshot.getDouble("averageRating") : 0.0;

            // Tính toán xếp hạng trung bình mới
            double newAverageRating = ((averageRating * totalReviews) + rating) / (totalReviews + 1);
            long newTotalReviews = totalReviews + 1;

            // Cập nhật tài liệu sản phẩm
            transaction.update(productRef, "averageRating", newAverageRating);
            transaction.update(productRef, "totalReviews", newTotalReviews);

            // Thêm bài đánh giá mới
            transaction.set(reviewRef, review);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(WriteReviewActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error submitting review: ", e);
            Toast.makeText(WriteReviewActivity.this, "Failed to submit review.", Toast.LENGTH_SHORT).show();
        });
    }
}
