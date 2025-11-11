package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.util.Log;
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

    private static final String TAG = "RegisterActivity";

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

        Log.i(TAG, "onCreate: RegisterActivity initialized");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Log.d(TAG, "Firebase instances initialized");

        // Map views
        initializeViews();

        Log.d(TAG, "All views mapped successfully");

        // Set listeners
        setupListeners();

        Log.i(TAG, "All listeners configured");
    }

    private void initializeViews() {
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
    }

    private void setupListeners() {
        // Toggle password visibility
        tvTogglePassword.setOnClickListener(v -> {
            Log.d(TAG, "Password toggle clicked");
            togglePasswordVisibility(edtPassword, tvTogglePassword);
        });

        tvToggleConfirmPassword.setOnClickListener(v -> {
            Log.d(TAG, "Confirm password toggle clicked");
            togglePasswordVisibility(edtConfirmPassword, tvToggleConfirmPassword);
        });

        // Register button
        btnRegister.setOnClickListener(v -> {
            Log.i(TAG, "Register button clicked");
            registerUser();
        });

        // Back to login
        tvBackToLogin.setOnClickListener(v -> {
            Log.i(TAG, "Back to login clicked");
            navigateToLogin();
        });
    }

    private void registerUser() {
        String fullName = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        Log.d(TAG, "Registration attempt - Email: " + email);

        clearErrors();

        boolean hasError = false;

        // Validate Full Name
        if (TextUtils.isEmpty(fullName)) {
            Log.w(TAG, "Validation: Full name is empty");
            tvFullNameError.setText("Vui lòng nhập họ và tên");
            tvFullNameError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        } else if (!NAME_PATTERN.matcher(fullName).matches()) {
            Log.w(TAG, "Validation: Full name format invalid - " + fullName);
            tvFullNameError.setText("Họ tên phải từ 2-50 ký tự, chỉ chứa chữ cái");
            tvFullNameError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        } else {
            Log.d(TAG, "Validation: Full name valid");
        }

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            Log.w(TAG, "Validation: Email is empty");
            tvEmailError.setText("Vui lòng nhập email");
            tvEmailError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            Log.w(TAG, "Validation: Email format invalid - " + email);
            tvEmailError.setText("Định dạng email không hợp lệ");
            tvEmailError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        } else {
            Log.d(TAG, "Validation: Email valid");
        }

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            Log.w(TAG, "Validation: Password is empty");
            tvPasswordError.setText("Vui lòng nhập mật khẩu");
            tvPasswordError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            Log.w(TAG, "Validation: Password too weak or invalid format");
            tvPasswordError.setText("Mật khẩu phải 8-32 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
            tvPasswordError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        } else {
            Log.d(TAG, "Validation: Password valid");
        }

        // Validate Confirm Password
        if (!password.equals(confirmPassword)) {
            Log.w(TAG, "Validation: Passwords don't match");
            tvConfirmPasswordError.setText("Mật khẩu xác nhận không khớp");
            tvConfirmPasswordError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        } else {
            Log.d(TAG, "Validation: Passwords match");
        }

        // Validate Terms
        if (!cbTerms.isChecked()) {
            Log.w(TAG, "Validation: Terms not accepted");
            tvTermsError.setText("Bạn phải đồng ý với điều khoản sử dụng");
            tvTermsError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        } else {
            Log.d(TAG, "Validation: Terms accepted");
        }

        if (hasError) {
            Log.e(TAG, "Validation failed - User will see error messages");
            showToast("Vui lòng kiểm tra lại thông tin", Toast.LENGTH_SHORT);
            return;
        }

        Log.i(TAG, "All validations passed - Proceeding with registration");
        setLoading(true);

        // Firebase registration
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "Firebase Authentication successful");
                            handleRegistrationSuccess(fullName, email);
                        } else {
                            Log.e(TAG, "Firebase Authentication failed", task.getException());
                            setLoading(false);
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";

                            // Handle specific Firebase errors
                            if (errorMsg.contains("already in use")) {
                                Log.w(TAG, "Email already registered");
                                showToast("Email này đã được đăng ký", Toast.LENGTH_SHORT);
                            } else if (errorMsg.contains("weak password")) {
                                Log.w(TAG, "Password rejected by Firebase");
                                showToast("Mật khẩu quá yếu", Toast.LENGTH_SHORT);
                            } else {
                                showToast("Đăng ký thất bại: " + errorMsg, Toast.LENGTH_LONG);
                            }
                        }
                    }
                });
    }

    private void handleRegistrationSuccess(String fullName, String email) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            Log.d(TAG, "Current user retrieved - UID: " + user.getUid());

            // Update user profile
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile updated successfully");
                        } else {
                            Log.w(TAG, "Failed to update user profile", task.getException());
                        }
                    });

            // Send verification email
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "Verification email sent to: " + email);
                            showToast("Email xác minh đã được gửi", Toast.LENGTH_SHORT);
                        } else {
                            Log.w(TAG, "Failed to send verification email", task.getException());
                            showToast("Không thể gửi email xác minh", Toast.LENGTH_SHORT);
                        }
                    });

            // Save user data to Firestore
            saveUserToFirestore(user.getUid(), fullName, email);
        } else {
            Log.e(TAG, "Current user is null after successful registration");
            setLoading(false);
            showToast("Lỗi: Không thể lấy thông tin người dùng", Toast.LENGTH_SHORT);
        }
    }

    private void saveUserToFirestore(String userId, String fullName, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("gender", "");
        userData.put("status", "pending");
        userData.put("isDisabled", false);

        Log.d(TAG, "Saving user data to Firestore - UID: " + userId);

        db.collection("users").document(userId)
                .set(userData)
                .addOnCompleteListener(task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        Log.i(TAG, "User data saved to Firestore successfully");
                        showToast("Đăng ký thành công! Vui lòng kiểm tra email để xác minh", Toast.LENGTH_LONG);

                        // Navigate to login
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            Log.d(TAG, "Navigating to LoginActivity");
                            navigateToLogin();
                        }, 1500);
                    } else {
                        Log.e(TAG, "Failed to save user data to Firestore", task.getException());
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";
                        showToast("Lỗi lưu thông tin: " + errorMsg, Toast.LENGTH_LONG);
                    }
                });
    }

    private void navigateToLogin() {
        Log.d(TAG, "Starting LoginActivity");
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showToast(String message, int duration) {
        Log.d(TAG, "Showing toast: " + message);
        Toast.makeText(RegisterActivity.this, message, duration).show();
    }

    private void showToast(String message) {
        showToast(message, Toast.LENGTH_SHORT);
    }

    private void clearErrors() {
        tvFullNameError.setVisibility(android.view.View.GONE);
        tvEmailError.setVisibility(android.view.View.GONE);
        tvPasswordError.setVisibility(android.view.View.GONE);
        tvConfirmPasswordError.setVisibility(android.view.View.GONE);
        tvTermsError.setVisibility(android.view.View.GONE);

        Log.d(TAG, "All error messages cleared");
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        Log.d(TAG, "Loading state: " + (loading ? "ON" : "OFF"));
    }

    private void togglePasswordVisibility(EditText editText, TextView toggleView) {
        boolean isPasswordHidden = editText.getTransformationMethod() instanceof PasswordTransformationMethod;

        if (isPasswordHidden) {
            editText.setTransformationMethod(SingleLineTransformationMethod.getInstance());
            toggleView.setText("ẨN");
            Log.d(TAG, "Password visibility: SHOWN");
        } else {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            toggleView.setText("HIỂN THỊ");
            Log.d(TAG, "Password visibility: HIDDEN");
        }

        editText.setSelection(editText.getText().length());
    }
}