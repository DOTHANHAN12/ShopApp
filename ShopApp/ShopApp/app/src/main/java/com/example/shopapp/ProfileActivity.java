package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final int EDIT_PROFILE_REQUEST = 1;

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
        setupNavigation();
        loadUserProfile();

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivityForResult(intent, EDIT_PROFILE_REQUEST);
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
            userButton.setOnClickListener(v -> {
                // Already on profile, do nothing or refresh
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
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
                                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                        Picasso.get().load(user.getProfileImageUrl()).into(profileImage);
                                    }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
            loadUserProfile();
        }
    }
}
