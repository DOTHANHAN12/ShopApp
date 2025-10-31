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

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WriteReviewActivity extends AppCompatActivity {

    private static final String TAG = "WriteReviewActivity";
    private static final long EDIT_TIME_LIMIT_HOURS = 12;

    private RatingBar ratingBar;
    private EditText editTextComment;
    private Button btnSubmitReview;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String productId;
    private String orderId;
    private Review existingReview; // To hold an existing review if found
    private String existingReviewId; // To hold the ID of the existing review

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
        orderId = getIntent().getStringExtra("ORDER_ID"); // Get Order ID

        if (productId == null || orderId == null) {
            Toast.makeText(this, "Product or Order ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkExistingReview(); // Check for existing review when activity starts

        btnSubmitReview.setOnClickListener(v -> submitOrUpdateReview());
    }

    private void checkExistingReview() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return; // No user logged in
        }

        db.collection("products").document(productId).collection("reviews")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("orderId", orderId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        existingReview = doc.toObject(Review.class);
                        existingReviewId = doc.getId();
                        if (existingReview != null) {
                            ratingBar.setRating(existingReview.getRating());
                            editTextComment.setText(existingReview.getComment());
                            btnSubmitReview.setText("Update Review");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking for existing review", e));
    }


    private void submitOrUpdateReview() {
        if (existingReview != null) {
            updateReview();
        } else {
            submitNewReview();
        }
    }

    private void updateReview() {
        long currentTime = System.currentTimeMillis();
        long reviewTime = existingReview.getTimestamp();
        long hoursDifference = TimeUnit.MILLISECONDS.toHours(currentTime - reviewTime);

        if (existingReview.isEdited()) {
            Toast.makeText(this, "You have already edited this review once.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hoursDifference >= EDIT_TIME_LIMIT_HOURS) {
            Toast.makeText(this, "You can no longer edit this review. The time limit is 12 hours.", Toast.LENGTH_LONG).show();
            return;
        }

        float newRating = ratingBar.getRating();
        String newComment = editTextComment.getText().toString().trim();
        float oldRating = existingReview.getRating();


        if (newRating == 0) {
            Toast.makeText(this, "Please provide a rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newComment.isEmpty()) {
            Toast.makeText(this, "Please write a comment.", Toast.LENGTH_SHORT).show();
            return;
        }


        DocumentReference productRef = db.collection("products").document(productId);
        DocumentReference reviewRef = productRef.collection("reviews").document(existingReviewId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot productSnapshot = transaction.get(productRef);

            long totalReviews = productSnapshot.getLong("totalReviews");
            double averageRating = productSnapshot.getDouble("averageRating");

            // Adjust average rating: remove old rating, add new rating
            double newAverageRating = ((averageRating * totalReviews) - oldRating + newRating) / totalReviews;

            transaction.update(productRef, "averageRating", newAverageRating);
            transaction.update(reviewRef, "rating", newRating);
            transaction.update(reviewRef, "comment", newComment);
            transaction.update(reviewRef, "edited", true);
            transaction.update(reviewRef, "updatedAt", System.currentTimeMillis());

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(WriteReviewActivity.this, "Review updated successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error updating review: ", e);
            Toast.makeText(WriteReviewActivity.this, "Failed to update review.", Toast.LENGTH_SHORT).show();
        });
    }


    private void submitNewReview() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check order status first
        db.collection("orders").document(orderId).get().addOnSuccessListener(orderSnapshot -> {
            if (!orderSnapshot.exists()) {
                Toast.makeText(this, "Order not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            Order order = orderSnapshot.toObject(Order.class);
            if (order == null || !"DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
                Toast.makeText(this, "You can only review products from delivered orders.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verify the product is in the order
            boolean productInOrder = false;
            for (Map<String, Object> item : order.getItems()) {
                if (productId.equals(item.get("productId"))) {
                    productInOrder = true;
                    break;
                }
            }

            if (!productInOrder) {
                Toast.makeText(this, "You can only review products you have purchased.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Now proceed to submit the review
            proceedWithNewReviewSubmission(currentUser);

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching order details", e);
            Toast.makeText(this, "Could not verify order status.", Toast.LENGTH_SHORT).show();
        });
    }

    private void proceedWithNewReviewSubmission(FirebaseUser currentUser) {
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

        Review review = new Review(userId, userName, rating, comment, System.currentTimeMillis(), productId, orderId);

        DocumentReference productRef = db.collection("products").document(productId);
        DocumentReference reviewRef = productRef.collection("reviews").document();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot productSnapshot = transaction.get(productRef);

            long totalReviews = productSnapshot.contains("totalReviews") ? productSnapshot.getLong("totalReviews") : 0;
            double averageRating = productSnapshot.contains("averageRating") ? productSnapshot.getDouble("averageRating") : 0.0;

            double newAverageRating = ((averageRating * totalReviews) + rating) / (totalReviews + 1);
            long newTotalReviews = totalReviews + 1;

            transaction.update(productRef, "averageRating", newAverageRating);
            transaction.update(productRef, "totalReviews", newTotalReviews);
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
