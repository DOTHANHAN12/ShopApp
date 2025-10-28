package com.example.shopapp;

import android.os.Bundle;
import android.util.Log;
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

public class MyReviewsActivity extends AppCompatActivity {

    private static final String TAG = "MyReviewsActivity";

    private RecyclerView recyclerViewMyReviews;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reviews);

        recyclerViewMyReviews = findViewById(R.id.recycler_view_my_reviews);
        recyclerViewMyReviews.setLayoutManager(new LinearLayoutManager(this));

        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewList);
        recyclerViewMyReviews.setAdapter(reviewAdapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        loadMyReviews();
    }

    private void loadMyReviews() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to see your reviews.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();

        db.collectionGroup("reviews")
                .whereEqualTo("userId", userId)
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
                        Log.e(TAG, "Error getting reviews: ", task.getException());
                        Toast.makeText(MyReviewsActivity.this, "Error loading reviews.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
