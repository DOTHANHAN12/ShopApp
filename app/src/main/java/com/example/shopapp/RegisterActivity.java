package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText edtFullName, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private TextView tvFullNameError, tvEmailError, tvPasswordError, tvConfirmPasswordError, tvTermsError;
    private TextView tvTogglePassword, tvToggleConfirmPassword;
    private android.widget.CheckBox cbTerms;
    private android.widget.ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ View
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

        // Sự kiện nút đăng ký
        btnRegister.setOnClickListener(v -> registerUser());

        // Quay lại trang đăng nhập (MainActivity)
        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
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
        if (TextUtils.isEmpty(fullName)) {
            tvFullNameError.setText("Vui lòng nhập họ tên");
            tvFullNameError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvEmailError.setText("Email không hợp lệ");
            tvEmailError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        }
        if (TextUtils.isEmpty(password)) {
            tvPasswordError.setText("Vui lòng nhập mật khẩu");
            tvPasswordError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        } else if (password.length() < 6) {
            tvPasswordError.setText("Mật khẩu ít nhất 6 ký tự");
            tvPasswordError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        }
        if (!TextUtils.equals(password, confirmPassword)) {
            tvConfirmPasswordError.setText("Mật khẩu xác nhận không khớp");
            tvConfirmPasswordError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        }
        if (!cbTerms.isChecked()) {
            tvTermsError.setText("Bạn cần đồng ý với Điều khoản và Chính sách");
            tvTermsError.setVisibility(android.view.View.VISIBLE);
            hasError = true;
        }

        if (hasError) {
            return;
        }

        setLoading(true);

        // Kiểm tra hợp lệ
        if (fullName.isEmpty()) {
            showToast("Vui lòng nhập họ tên");
            return;
        }
        if (email.isEmpty()) {
            showToast("Vui lòng nhập email");
            return;
        }
        if (password.isEmpty()) {
            showToast("Vui lòng nhập mật khẩu");
            return;
        }
        if (password.length() < 6) {
            showToast("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showToast("Mật khẩu xác nhận không khớp");
            return;
        }

        // Tạo tài khoản Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                // Lưu thêm thông tin user vào Realtime Database
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("fullName", fullName);
                                userData.put("email", email);

                                FirebaseDatabase.getInstance().getReference("users")
                                        .child(user.getUid())
                                        .setValue(userData)
                                        .addOnCompleteListener(saveTask -> {
                                            if (saveTask.isSuccessful()) {
                                                showToast("Đăng ký thành công! Chào mừng " + fullName);
                                                // Quay lại màn chính (MainActivity)
                                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                                finish();
                                            } else {
                                                showToast("Lỗi lưu dữ liệu: " + saveTask.getException().getMessage());
                                            }
                                        });
                            }
                        } else {
                            showToast("Đăng ký thất bại: " + task.getException().getMessage());
                        }
                        setLoading(false);
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
