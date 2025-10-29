package com.example.shopapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    private CircleImageView profileImage;
    private TextView textChangePhoto;
    private TextInputEditText editFullName, editPhoneNumber, editAddress;
    private RadioGroup radioGroupGender;
    private RadioButton radioMale, radioFemale, radioOther;
    private Button btnSave;

    private Uri imageUri;
    private String currentProfileImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        mapViews();
        setupNavigation();
        loadUserProfile();

        textChangePhoto.setOnClickListener(v -> openFileChooser());
        btnSave.setOnClickListener(v -> saveProfileChanges());
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
            userButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        ImageView backButton = findViewById(R.id.img_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void mapViews() {
        profileImage = findViewById(R.id.profile_image);
        textChangePhoto = findViewById(R.id.text_change_photo);
        editFullName = findViewById(R.id.edit_full_name);
        editPhoneNumber = findViewById(R.id.edit_phone_number);
        editAddress = findViewById(R.id.edit_address);
        radioGroupGender = findViewById(R.id.radio_group_gender);
        radioMale = findViewById(R.id.radio_male);
        radioFemale = findViewById(R.id.radio_female);
        radioOther = findViewById(R.id.radio_other);
        btnSave = findViewById(R.id.btn_save);
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        editFullName.setText(user.getFullName());
                        editPhoneNumber.setText(user.getPhoneNumber());

                        if (user.getDefaultAddress() != null) {
                            editAddress.setText(user.getDefaultAddress().get("fullAddress"));
                        }

                        currentProfileImageUrl = user.getProfileImageUrl();
                        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                            Picasso.get().load(currentProfileImageUrl).into(profileImage);
                        }

                        String gender = user.getGender();
                        if (gender != null) {
                            if (gender.equalsIgnoreCase("Nam")) {
                                radioMale.setChecked(true);
                            } else if (gender.equalsIgnoreCase("Nữ")) {
                                radioFemale.setChecked(true);
                            } else {
                                radioOther.setChecked(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Picasso.get().load(imageUri).into(profileImage);
        }
    }

    private void saveProfileChanges() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        String fullName = editFullName.getText().toString().trim();
        String phoneNumber = editPhoneNumber.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        if (fullName.isEmpty()) {
            editFullName.setError("Họ và tên không được để trống");
            editFullName.requestFocus();
            return;
        }

        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedGenderId);
        String gender = selectedRadioButton.getText().toString();

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phoneNumber", phoneNumber);
        updates.put("gender", gender);

        Map<String, String> addressMap = new HashMap<>();
        addressMap.put("fullAddress", address);
        updates.put("defaultAddress", addressMap);

        if (imageUri != null) {
            StorageReference fileReference = storageRef.child("profile_images/" + userId + ".jpg");
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        updates.put("profileImageUrl", imageUrl);
                        updateUserFirestore(userId, updates);
                    }))
                    .addOnFailureListener(e -> Toast.makeText(EditProfileActivity.this, "Lỗi tải lên hình ảnh", Toast.LENGTH_SHORT).show());
        } else {
            updates.put("profileImageUrl", currentProfileImageUrl);
            updateUserFirestore(userId, updates);
        }
    }

    private void updateUserFirestore(String userId, Map<String, Object> updates) {
        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ thành công", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật hồ sơ", Toast.LENGTH_SHORT).show());
    }
}
