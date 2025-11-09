package com.example.shopapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WriteReviewActivity extends AppCompatActivity {

    private static final String TAG = "WriteReviewActivity";
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int MAX_IMAGES = 5;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    private RatingBar ratingBar;
    private EditText editTextComment;
    private MaterialButton btnSubmit;
    private ImageView btnBack, imgProduct;
    private TextView textProductName, textProductVariant, textRatingDescription;
    private CardView btnAddPhoto;
    private LinearLayout layoutImagePreview;
    private CheckBox checkboxAnonymous;

    private String productId;
    private String orderId;
    private String productName;
    private String productVariant;
    private String productImageUrl;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<String> uploadedImageUrls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        getIntentData();
        setupListeners();
        displayProductInfo();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ratingBar = findViewById(R.id.rating_bar_write_review);
        editTextComment = findViewById(R.id.edit_text_review_comment);
        btnSubmit = findViewById(R.id.btn_submit_review);
        imgProduct = findViewById(R.id.img_product);
        textProductName = findViewById(R.id.text_product_name);
        textProductVariant = findViewById(R.id.text_product_variant);
        textRatingDescription = findViewById(R.id.text_rating_description);
        btnAddPhoto = findViewById(R.id.btn_add_photo);
        layoutImagePreview = findViewById(R.id.layout_image_preview);
        checkboxAnonymous = findViewById(R.id.checkbox_anonymous);
    }

    private void getIntentData() {
        productId = getIntent().getStringExtra("PRODUCT_ID");
        orderId = getIntent().getStringExtra("ORDER_ID");
        productName = getIntent().getStringExtra("PRODUCT_NAME");
        productVariant = getIntent().getStringExtra("PRODUCT_VARIANT");
        productImageUrl = getIntent().getStringExtra("PRODUCT_IMAGE");

        if (productId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayProductInfo() {
        if (productName != null) {
            textProductName.setText(productName);
        }
        if (productVariant != null) {
            textProductVariant.setText("Phân loại: " + productVariant);
        }
        if (productImageUrl != null && !productImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(productImageUrl)
                    .placeholder(R.drawable.ic_product_placeholder)
                    .into(imgProduct);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            updateRatingDescription(rating);
        });

        btnAddPhoto.setOnClickListener(v -> openImagePicker());

        btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void updateRatingDescription(float rating) {
        String description;
        if (rating >= 5) {
            description = "⭐ Tuyệt vời";
        } else if (rating >= 4) {
            description = "😊 Hài lòng";
        } else if (rating >= 3) {
            description = "😐 Bình thường";
        } else if (rating >= 2) {
            description = "😕 Không hài lòng";
        } else {
            description = "😞 Rất tệ";
        }
        textRatingDescription.setText(description);
    }

    private void openImagePicker() {
        if (selectedImageUris.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Bạn chỉ có thể thêm tối đa " + MAX_IMAGES + " ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                selectedImageUris.add(imageUri);
                addImagePreview(imageUri);
            }
        }
    }

    private void addImagePreview(Uri imageUri) {
        View imagePreviewView = getLayoutInflater().inflate(R.layout.item_image_preview, layoutImagePreview, false);
        ImageView imgPreview = imagePreviewView.findViewById(R.id.img_preview);
        ImageView btnRemove = imagePreviewView.findViewById(R.id.btn_remove_image);

        Glide.with(this).load(imageUri).centerCrop().into(imgPreview);

        btnRemove.setOnClickListener(v -> {
            selectedImageUris.remove(imageUri);
            layoutImagePreview.removeView(imagePreviewView);
        });

        // Add before the "Add Photo" button
        int addButtonIndex = layoutImagePreview.indexOfChild(btnAddPhoto);
        layoutImagePreview.addView(imagePreviewView, addButtonIndex);
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        String comment = editTextComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nhận xét của bạn", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để đánh giá", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true);
            btnSubmit.setText("GỬI ĐÁNH GIÁ");
            return;
        }

        // Upload images first if any
        if (!selectedImageUris.isEmpty()) {
            uploadImages(() -> saveReviewToFirestore(rating, comment, currentUser));
        } else {
            saveReviewToFirestore(rating, comment, currentUser);
        }
    }

    private void uploadImages(Runnable onComplete) {
        uploadedImageUrls.clear();
        final int[] uploadCount = {0};

        for (Uri imageUri : selectedImageUris) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] data = baos.toByteArray();

                String imagePath = "reviews/" + productId + "/" + System.currentTimeMillis() + ".jpg";
                StorageReference imageRef = storage.getReference().child(imagePath);

                imageRef.putBytes(data)
                        .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    uploadedImageUrls.add(uri.toString());
                                    uploadCount[0]++;
                                    if (uploadCount[0] == selectedImageUris.size()) {
                                        onComplete.run();
                                    }
                                }))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error uploading image", e);
                            uploadCount[0]++;
                            if (uploadCount[0] == selectedImageUris.size()) {
                                onComplete.run();
                            }
                        });
            } catch (IOException e) {
                Log.e(TAG, "Error processing image", e);
                uploadCount[0]++;
                if (uploadCount[0] == selectedImageUris.size()) {
                    onComplete.run();
                }
            }
        }
    }

    private void saveReviewToFirestore(float rating, String comment, FirebaseUser currentUser) {
        String userName = checkboxAnonymous.isChecked() ? "Người dùng ẩn danh" :
                (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Người dùng");

        Review review = new Review(
                currentUser.getUid(),
                userName,
                rating,
                comment,
                System.currentTimeMillis(),
                productId,
                orderId
        );

        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("userId", review.getUserId());
        reviewData.put("userName", review.getUserName());
        reviewData.put("rating", review.getRating());
        reviewData.put("comment", review.getComment());
        reviewData.put("timestamp", review.getTimestamp());
        reviewData.put("productId", review.getProductId());
        reviewData.put("orderId", review.getOrderId());
        reviewData.put("status", "APPROVED"); // Auto approve for now
        reviewData.put("isEdited", false);
        reviewData.put("images", uploadedImageUrls);

        db.collection("products").document(productId).collection("reviews")
                .add(reviewData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Review added successfully with ID: " + documentReference.getId());

                    // Update product rating
                    updateProductRating();

                    Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();

                    // Return to previous activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("review_submitted", true);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding review", e);
                    Toast.makeText(this, "Lỗi khi gửi đánh giá. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("GỬI ĐÁNH GIÁ");
                });
    }

    private void updateProductRating() {
        db.collection("products").document(productId).collection("reviews")
                .whereEqualTo("status", "APPROVED")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        float totalRating = 0;
                        int count = queryDocumentSnapshots.size();

                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Review review = doc.toObject(Review.class);
                            totalRating += review.getRating();
                        }

                        float averageRating = totalRating / count;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("averageRating", averageRating);
                        updates.put("totalReviews", count);

                        db.collection("products").document(productId).update(updates);
                    }
                });
    }
}