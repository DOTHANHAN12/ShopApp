package com.example.shopapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText edtEmail, edtPassword;
    private Button btnRegister, btnLogin;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
            return;
        }

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ email và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                if (user.isEmailVerified()) {
                                    checkUserStatus(user.getUid());
                                } else {
                                    showVerificationDialog(user);
                                }
                            }
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Đăng nhập thất bại: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void checkUserStatus(String userId) {
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                if (user.isDisabled()) {
                                    mAuth.signOut();
                                    Toast.makeText(LoginActivity.this, "Tài khoản của bạn đã bị khóa.", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                if ("pending".equalsIgnoreCase(user.getStatus())) {
                                    db.collection("users").document(userId).update("status", "active")
                                            .addOnCompleteListener(updateTask -> {
                                                if (updateTask.isSuccessful()) {
                                                    loginSuccess(userId);
                                                } else {
                                                    mAuth.signOut();
                                                    Toast.makeText(LoginActivity.this, "Lỗi cập nhật trạng thái tài khoản.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } else if ("active".equalsIgnoreCase(user.getStatus())) {
                                    loginSuccess(userId);
                                } else {
                                     mAuth.signOut();
                                     Toast.makeText(LoginActivity.this, "Trạng thái tài khoản không hợp lệ.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                             mAuth.signOut();
                             Toast.makeText(LoginActivity.this, "Không tìm thấy thông tin người dùng.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                         mAuth.signOut();
                         Toast.makeText(LoginActivity.this, "Lỗi khi kiểm tra thông tin: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginSuccess(String userId) {
        db.collection("users").document(userId).update("lastLogin", System.currentTimeMillis());
        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void showVerificationDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Xác minh Email");
        builder.setMessage("Vui lòng xác minh tài khoản của bạn trước khi đăng nhập. Bạn có muốn gửi lại email xác minh không?");
        builder.setPositiveButton("Gửi lại", (dialog, which) -> {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Email xác minh đã được gửi lại.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Không thể gửi lại email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
                mAuth.signOut();
            });
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> {
            dialog.dismiss();
            mAuth.signOut();
        });
        builder.setCancelable(false);
        builder.create().show();
    }
}
