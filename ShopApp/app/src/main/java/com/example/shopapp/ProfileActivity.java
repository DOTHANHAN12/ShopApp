package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private CircleImageView profileImage;
    private TextView textUserName, textUserEmail;
    private Button btnEditProfile, btnLogout;
    private LinearLayout layoutMyOrders, layoutShippingAddresses, layoutWishlist, layoutMyReviews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        mapViews();
        loadUserProfile();

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        layoutMyOrders.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, OrderHistoryActivity.class);
            startActivity(intent);
        });

        layoutShippingAddresses.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AddressSelectionActivity.class);
            startActivity(intent);
        });

        layoutWishlist.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FavoriteActivity.class);
            startActivity(intent);
        });

        layoutMyReviews.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MyReviewsActivity.class);
            startActivity(intent);
        });

        ImageView imgBack = findViewById(R.id.img_back);
        imgBack.setOnClickListener(v -> finish());
    }

    private void mapViews() {
        profileImage = findViewById(R.id.profile_image);
        textUserName = findViewById(R.id.text_user_name);
        textUserEmail = findViewById(R.id.text_user_email);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnLogout = findViewById(R.id.btn_logout);
        layoutMyOrders = findViewById(R.id.layout_my_orders);
        layoutShippingAddresses = findViewById(R.id.layout_shipping_addresses);
        layoutWishlist = findViewById(R.id.layout_wishlist);
        layoutMyReviews = findViewById(R.id.layout_my_reviews);
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                User user = document.toObject(User.class);
                                if (user != null) {
                                    textUserName.setText(user.getFullName());
                                    textUserEmail.setText(user.getEmail());
                                    // TODO: Load profile image with Picasso or Glide
                                }
                            } else {
                                Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Failed to load user profile.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
