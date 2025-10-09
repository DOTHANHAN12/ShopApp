package com.example.shopapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity để hiển thị trang sản phẩm Reversible Parka.
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Thiết lập layout sử dụng file activity_home.xml
        setContentView(R.layout.activity_home);

        // Phần này hiện tại không cần thêm logic UI nào,
        // nó chỉ đơn giản là hiển thị giao diện.
        // Nếu cần tương tác, bạn sẽ viết code tại đây.
    }
}