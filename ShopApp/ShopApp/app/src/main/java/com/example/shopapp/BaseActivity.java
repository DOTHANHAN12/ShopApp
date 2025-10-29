package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_base);

        // Header buttons
        ImageView cartButton = findViewById(R.id.ic_cart);
        if (cartButton != null) {
            cartButton.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }

        // Footer buttons
        ImageView homeButton = findViewById(R.id.nav_home_cs);
        if (homeButton != null) {
            homeButton.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        }

        ImageView userButton = findViewById(R.id.nav_user_cs);
        if (userButton != null) {
            userButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        FrameLayout container = findViewById(R.id.fragment_container);
        if (container != null) {
            getLayoutInflater().inflate(layoutResID, container, true);
        }
    }
}
