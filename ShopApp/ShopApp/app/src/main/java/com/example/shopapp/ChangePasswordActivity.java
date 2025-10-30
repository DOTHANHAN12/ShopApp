package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private ImageView imgBack;
    private TextInputEditText editCurrentPassword, editNewPassword, editConfirmPassword;
    private TextInputLayout inputLayoutCurrentPassword, inputLayoutNewPassword, inputLayoutConfirmPassword;
    private Button btnChangePassword;
    private ProgressBar progressBar;
    private TextView textForgotPassword;

    // Password requirement indicators
    private ImageView iconLength, iconUppercase, iconNumber;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // User not logged in, redirect to login
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Set click listeners
        setClickListeners();

        // Setup password validation
        setupPasswordValidation();
    }

    private void initViews() {
        imgBack = findViewById(R.id.img_back);
        editCurrentPassword = findViewById(R.id.edit_current_password);
        editNewPassword = findViewById(R.id.edit_new_password);
        editConfirmPassword = findViewById(R.id.edit_confirm_password);
        inputLayoutCurrentPassword = findViewById(R.id.input_layout_current_password);
        inputLayoutNewPassword = findViewById(R.id.input_layout_new_password);
        inputLayoutConfirmPassword = findViewById(R.id.input_layout_confirm_password);
        btnChangePassword = findViewById(R.id.btn_change_password);
        progressBar = findViewById(R.id.progress_bar);
        textForgotPassword = findViewById(R.id.text_forgot_password);

        // Password requirement indicators
        iconLength = findViewById(R.id.icon_length);
        iconUppercase = findViewById(R.id.icon_uppercase);
        iconNumber = findViewById(R.id.icon_number);
    }

    private void setClickListeners() {
        imgBack.setOnClickListener(v -> onBackPressed());

        textForgotPassword.setOnClickListener(v -> {
            // Navigate to forgot password activity
            Intent intent = new Intent(ChangePasswordActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(v -> {
            String currentPassword = editCurrentPassword.getText().toString().trim();
            String newPassword = editNewPassword.getText().toString().trim();
            String confirmPassword = editConfirmPassword.getText().toString().trim();

            if (validateInputs(currentPassword, newPassword, confirmPassword)) {
                changePassword(currentPassword, newPassword);
            }
        });
    }

    private void setupPasswordValidation() {
        editNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validatePasswordStrength(String password) {
        // Check length (minimum 8 characters)
        boolean hasLength = password.length() >= 8;
        updateIndicator(iconLength, hasLength);

        // Check uppercase
        boolean hasUppercase = password.matches(".*[A-Z].*");
        updateIndicator(iconUppercase, hasUppercase);

        // Check number
        boolean hasNumber = password.matches(".*[0-9].*");
        updateIndicator(iconNumber, hasNumber);
    }

    private void updateIndicator(ImageView icon, boolean isValid) {
        if (isValid) {
            icon.setImageResource(android.R.drawable.checkbox_on_background);
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            icon.setImageResource(R.drawable.ic_close);
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }

    private boolean validateInputs(String currentPassword, String newPassword, String confirmPassword) {
        boolean isValid = true;

        // Validate current password
        if (TextUtils.isEmpty(currentPassword)) {
            inputLayoutCurrentPassword.setError("Vui lòng nhập mật khẩu hiện tại");
            isValid = false;
        } else {
            inputLayoutCurrentPassword.setError(null);
        }

        // Validate new password
        if (TextUtils.isEmpty(newPassword)) {
            inputLayoutNewPassword.setError("Vui lòng nhập mật khẩu mới");
            isValid = false;
        } else if (newPassword.length() < 8) {
            inputLayoutNewPassword.setError("Mật khẩu phải có ít nhất 8 ký tự");
            isValid = false;
        } else if (!newPassword.matches(".*[A-Z].*")) {
            inputLayoutNewPassword.setError("Mật khẩu phải chứa ít nhất 1 chữ hoa");
            isValid = false;
        } else if (!newPassword.matches(".*[0-9].*")) {
            inputLayoutNewPassword.setError("Mật khẩu phải chứa ít nhất 1 số");
            isValid = false;
        } else if (newPassword.equals(currentPassword)) {
            inputLayoutNewPassword.setError("Mật khẩu mới phải khác mật khẩu hiện tại");
            isValid = false;
        } else {
            inputLayoutNewPassword.setError(null);
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            inputLayoutConfirmPassword.setError("Vui lòng xác nhận mật khẩu mới");
            isValid = false;
        } else if (!confirmPassword.equals(newPassword)) {
            inputLayoutConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        } else {
            inputLayoutConfirmPassword.setError(null);
        }

        return isValid;
    }

    private void changePassword(String currentPassword, String newPassword) {
        showLoading(true);

        // Re-authenticate user with current password
        String email = currentUser.getEmail();
        if (email == null) {
            showLoading(false);
            Toast.makeText(this, "Lỗi: Không tìm thấy email người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    // Re-authentication successful, now update password
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(unused1 -> {
                                showLoading(false);
                                showSuccessDialog();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(ChangePasswordActivity.this,
                                        "Lỗi khi đổi mật khẩu: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);

                    // Current password is incorrect
                    inputLayoutCurrentPassword.setError("Mật khẩu hiện tại không đúng");
                    editCurrentPassword.requestFocus();

                    Toast.makeText(ChangePasswordActivity.this,
                            "Mật khẩu hiện tại không chính xác",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Thành công!")
                .setMessage("Mật khẩu của bạn đã được thay đổi thành công.")
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnChangePassword.setEnabled(false);
            btnChangePassword.setAlpha(0.5f);
        } else {
            progressBar.setVisibility(View.GONE);
            btnChangePassword.setEnabled(true);
            btnChangePassword.setAlpha(1f);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}