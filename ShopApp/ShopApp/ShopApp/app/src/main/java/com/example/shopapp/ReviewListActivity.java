package com.example.shopapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReviewListActivity extends AppCompatActivity {

    private static final String TAG = "ReviewListActivity";

    private RecyclerView recyclerViewAllReviews;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;

    private FirebaseFirestore db;
    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_list);

        recyclerViewAllReviews = findViewById(R.id.recycler_view_all_reviews);
        recyclerViewAllReviews.setLayoutManager(new LinearLayoutManager(this));

        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewList);
        recyclerViewAllReviews.setAdapter(reviewAdapter);

        db = FirebaseFirestore.getInstance();

        productId = getIntent().getStringExtra("PRODUCT_ID");

        if (productId == null) {
            Toast.makeText(this, "Product ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadAllReviews();
    }

    private void loadAllReviews() {
        db.collection("products").document(productId).collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reviewList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Review review = document.toObject(Review.class);
                            reviewList.add(review);
                        }
                        reviewAdapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        Toast.makeText(ReviewListActivity.this, "Error loading reviews.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
