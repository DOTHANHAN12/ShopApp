package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OrderSuccessActivity extends AppCompatActivity {

    private TextView textOrderId;
    private Button btnContinueShopping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_success);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        textOrderId = findViewById(R.id.text_order_success_id);
        btnContinueShopping = findViewById(R.id.btn_continue_shopping);

        // Lấy ID đơn hàng nếu có
        String orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId != null) {
            textOrderId.setText("Mã đơn hàng của bạn: #" + orderId);
        } else {
            textOrderId.setText("Đặt hàng thành công!");
        }

        // Chuyển về màn hình chính (hoặc Home Activity)
        btnContinueShopping.setOnClickListener(v -> {
            // Thay thế MainActivity.class bằng Activity chính của bạn
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}