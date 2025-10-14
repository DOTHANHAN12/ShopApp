package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView profileName, profileEmail;
    private Button btnEditProfile, btnChangePassword, btnLogout;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();

        profileImage = findViewById(R.id.profile_image);
        profileName = findViewById(R.id.profile_name);
        profileEmail = findViewById(R.id.profile_email);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnLogout = findViewById(R.id.btn_logout);

        loadUserProfile();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnEditProfile.setOnClickListener(v -> {
            // Navigate to an EditProfileActivity (to be created)
            Toast.makeText(this, "Edit Profile clicked", Toast.LENGTH_SHORT).show();
        });

        btnChangePassword.setOnClickListener(v -> {
            // Handle password change
            Toast.makeText(this, "Change Password clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                profileName.setText(user.getDisplayName());
            } else {
                profileName.setText("No Name");
            }
            profileEmail.setText(user.getEmail());
            // You can load the profile image using a library like Picasso or Glide
            // if the user has a profile image URL.
        }
    }
}
