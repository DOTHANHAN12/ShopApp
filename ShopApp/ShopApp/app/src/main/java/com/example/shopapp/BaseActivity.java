package com.example.shopapp;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base Activity với header và footer navigation được quản lý tự động
 * Các activity extend từ class này sẽ tự động có navigation buttons hoạt động
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected NavigationHelper navigationHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_base);

        // Khởi tạo NavigationHelper và setup navigation
        navigationHelper = new NavigationHelper(this);
        navigationHelper.setupNavigation();
    }

    @Override
    public void setContentView(int layoutResID) {
        FrameLayout container = findViewById(R.id.fragment_container);
        if (container != null) {
            getLayoutInflater().inflate(layoutResID, container, true);
            // Setup lại navigation sau khi inflate layout để đảm bảo tìm được các view
            if (navigationHelper != null) {
                navigationHelper.setupNavigation();
            }
        }
    }

    /**
     * Ẩn header và footer (dùng cho các màn hình đặc biệt)
     */
    protected void hideHeaderAndFooter() {
        if (navigationHelper != null) {
            navigationHelper.hideHeaderAndFooter();
        }
    }

    /**
     * Hiện header và footer
     */
    protected void showHeaderAndFooter() {
        if (navigationHelper != null) {
            navigationHelper.showHeaderAndFooter();
        }
    }
}
