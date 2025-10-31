package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageView imgBack;
    private TextInputEditText editEmail;
    private TextInputLayout inputLayoutEmail;
    private Button btnSendResetEmail;
    private ProgressBar progressBar;
    private LinearLayout layoutBackToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();

        // Set click listeners
        setClickListeners();
    }

    private void initViews() {
        imgBack = findViewById(R.id.img_back);
        editEmail = findViewById(R.id.edit_email);
        inputLayoutEmail = findViewById(R.id.input_layout_email);
        btnSendResetEmail = findViewById(R.id.btn_send_reset_email);
        progressBar = findViewById(R.id.progress_bar);
        layoutBackToLogin = findViewById(R.id.layout_back_to_login);
    }

    private void setClickListeners() {
        imgBack.setOnClickListener(v -> onBackPressed());

        layoutBackToLogin.setOnClickListener(v -> {
            // Navigate to login activity
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnSendResetEmail.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();

            if (validateEmail(email)) {
                checkEmailExistsAndSendReset(email);
            }
        });
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            inputLayoutEmail.setError("Vui lòng nhập địa chỉ email");
            editEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputLayoutEmail.setError("Địa chỉ email không hợp lệ");
            editEmail.requestFocus();
            return false;
        }

        inputLayoutEmail.setError(null);
        return true;
    }

    private void checkEmailExistsAndSendReset(String email) {
        showLoading(true);

        // First, check if email exists in Firestore users collection
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Email not found in database
                        showLoading(false);
                        inputLayoutEmail.setError("Email này chưa được đăng ký");
                        editEmail.requestFocus();
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Không tìm thấy tài khoản với email này",
                                Toast.LENGTH_LONG).show();
                    } else {
                        // Email exists, proceed to send reset email
                        sendPasswordResetEmail(email);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    showLoading(false);

                    // Show success dialog or toast
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Email đặt lại mật khẩu đã được gửi! Vui lòng kiểm tra hộp thư của bạn.",
                            Toast.LENGTH_LONG).show();

                    // Optionally, navigate back to login
                    new android.os.Handler().postDelayed(() -> {
                        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }, 2000);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);

                    // Handle specific Firebase Auth errors
                    String errorMessage = "Không thể gửi email đặt lại mật khẩu";

                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("no user record")) {
                            errorMessage = "Email này chưa được đăng ký";
                        } else if (e.getMessage().contains("network")) {
                            errorMessage = "Lỗi kết nối mạng. Vui lòng thử lại";
                        }
                    }

                    Toast.makeText(ForgotPasswordActivity.this,
                            errorMessage,
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSendResetEmail.setEnabled(false);
            btnSendResetEmail.setAlpha(0.5f);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSendResetEmail.setEnabled(true);
            btnSendResetEmail.setAlpha(1f);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}