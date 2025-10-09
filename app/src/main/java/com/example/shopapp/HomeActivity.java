package com.example.shopapp;


import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.WindowManager;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Ẩn thanh Action Bar (Thanh tiêu đề của Activity)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 2. [QUAN TRỌNG] BỎ FLAG_FULLSCREEN.
        //    Chỉ cho phép nội dung vẽ dưới Status Bar mà không ẩn nó.
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        // 3. Đặt màu Status Bar là trong suốt
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        setContentView(R.layout.activity_home);
    }
}