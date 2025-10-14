package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText edtFullName, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private TextView tvFullNameError, tvEmailError, tvPasswordError, tvConfirmPasswordError, tvTermsError;
    private TextView tvTogglePassword, tvToggleConfirmPassword;
    private CheckBox cbTerms;
    private ProgressBar progressBar;

    // Regex patterns
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L} ]{2,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,32}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Map views
        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        tvFullNameError = findViewById(R.id.tvFullNameError);
        tvEmailError = findViewById(R.id.tvEmailError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError);
        tvTermsError = findViewById(R.id.tvTermsError);
        tvTogglePassword = findViewById(R.id.tvTogglePassword);
        tvToggleConfirmPassword = findViewById(R.id.tvToggleConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        progressBar = findViewById(R.id.progressBar);

        // Toggle password visibility
        tvTogglePassword.setOnClickListener(v -> togglePasswordVisibility(edtPassword, tvTogglePassword));
        tvToggleConfirmPassword.setOnClickListener(v -> togglePasswordVisibility(edtConfirmPassword, tvToggleConfirmPassword));

        // Register button
        btnRegister.setOnClickListener(v -> registerUser());

        // Back to login
        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        clearErrors();

        boolean hasError = false;

        if (TextUtils.isEmpty(fullName) || !NAME_PATTERN.matcher(fullName).matches()) {
            tvFullNameError.setText("Họ tên không hợp lệ");
            hasError = true;
        }

        if (TextUtils.isEmpty(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            tvEmailError.setText("Email không hợp lệ");
            hasError = true;
        }

        if (TextUtils.isEmpty(password) || !PASSWORD_PATTERN.matcher(password).matches()) {
            tvPasswordError.setText("Mật khẩu quá yếu");
            hasError = true;
        }

        if (!password.equals(confirmPassword)) {
            tvConfirmPasswordError.setText("Mật khẩu xác nhận không khớp");
            hasError = true;
        }

        if (!cbTerms.isChecked()) {
            tvTermsError.setText("Bạn phải đồng ý với điều khoản");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 1. Update Firebase Auth display name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                            user.updateProfile(profileUpdates);

                            // 2. Create user data map for Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("fullName", fullName);
                            userData.put("email", email);
                            userData.put("createdAt", System.currentTimeMillis());
                            userData.put("gender", ""); // Placeholder
                            userData.put("phoneNumber", ""); // Placeholder

                            Map<String, String> addressData = new HashMap<>();
                            addressData.put("city", "");
                            addressData.put("street", "");
                            addressData.put("zipCode", "");
                            userData.put("defaultAddress", addressData);

                            // 3. Save to Firestore
                            db.collection("users").document(user.getUid())
                                    .set(userData)
                                    .addOnCompleteListener(saveTask -> {
                                        setLoading(false);
                                        if (saveTask.isSuccessful()) {
                                            showToast("Đăng ký thành công!");
                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                            finish();
                                        } else {
                                            showToast("Lỗi lưu thông tin: " + saveTask.getException().getMessage());
                                        }
                                    });
                        }
                    } else {
                        setLoading(false);
                        showToast("Đăng ký thất bại: " + task.getException().getMessage());
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void clearErrors() {
        tvFullNameError.setVisibility(android.view.View.GONE);
        tvEmailError.setVisibility(android.view.View.GONE);
        tvPasswordError.setVisibility(android.view.View.GONE);
        tvConfirmPasswordError.setVisibility(android.view.View.GONE);
        tvTermsError.setVisibility(android.view.View.GONE);
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void togglePasswordVisibility(EditText editText, TextView toggleView) {
        boolean isPasswordHidden = editText.getTransformationMethod() instanceof PasswordTransformationMethod;
        if (isPasswordHidden) {
            editText.setTransformationMethod(SingleLineTransformationMethod.getInstance());
            toggleView.setText("ẨN");
        } else {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            toggleView.setText("HIỂN THỊ");
        }
        editText.setSelection(editText.getText().length());
    }
}
