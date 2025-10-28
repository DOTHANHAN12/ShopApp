package com.example.shopapp;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editEmail;
    private Button btnSendResetEmail;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        editEmail = findViewById(R.id.edit_email);
        btnSendResetEmail = findViewById(R.id.btn_send_reset_email);
        mAuth = FirebaseAuth.getInstance();

        btnSendResetEmail.setOnClickListener(v -> sendPasswordResetEmail());

        ImageView imgBack = findViewById(R.id.img_back);
        imgBack.setOnClickListener(v -> finish());
    }

    private void sendPasswordResetEmail() {
        String email = editEmail.getText().toString().trim();

        if (email.isEmpty()) {
            editEmail.setError("Email không được để trống");
            editEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Vui lòng nhập email hợp lệ");
            editEmail.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Đã gửi liên kết đặt lại mật khẩu đến email của bạn.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
