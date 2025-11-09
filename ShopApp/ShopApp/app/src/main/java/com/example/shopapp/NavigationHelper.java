package com.example.shopapp;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Helper class để quản lý header và footer navigation buttons
 * Tự động tìm và setup các nút navigation trong các layout khác nhau
 */
public class NavigationHelper {
    private static final String TAG = "NavigationHelper";
    private final AppCompatActivity activity;
    private final FirebaseAuth mAuth;

    public NavigationHelper(AppCompatActivity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Setup tất cả các navigation buttons (header và footer)
     */
    public void setupNavigation() {
        setupHeaderButtons();
        setupFooterButtons();
        setupBackButton();
    }

    /**
     * Setup các nút trong header (cart, favorite, notification)
     */
    public void setupHeaderButtons() {
        // Cart button - có nhiều ID khác nhau
        setupCartButton();
        
        // Favorite button
        setupFavoriteButton();
        
        // Notification button
        setupNotificationButton();
    }

    /**
     * Setup các nút trong footer (home, search, profile)
     */
    public void setupFooterButtons() {
        // Home button
        ImageView homeButton = activity.findViewById(R.id.nav_home_cs);
        if (homeButton != null) {
            homeButton.setOnClickListener(v -> {
                Log.d(TAG, "Home button clicked");
                navigateToHome();
            });
        }

        // Profile/User button
        ImageView userButton = activity.findViewById(R.id.nav_user_cs);
        if (userButton != null) {
            userButton.setOnClickListener(v -> {
                Log.d(TAG, "User button clicked");
                navigateToProfile();
            });
        }

        // Search button (nếu có)
        View searchButton = activity.findViewById(R.id.btn_search_footer);
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> {
                Log.d(TAG, "Search button clicked");
                // Có thể mở search activity nếu cần
            });
        }
    }

    /**
     * Setup nút back với nhiều ID khác nhau
     */
    public void setupBackButton() {
        ImageView backButton = findViewByMultipleIds(
                R.id.img_back,
                R.id.btn_back,
                R.id.img_back_arrow,
                R.id.img_back_cart,
                R.id.img_back_list,
                R.id.img_back_voucher,
                R.id.btn_back_notification,
                R.id.btn_back_order_detail
        );
        
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked");
                activity.finish();
            });
        }
    }

    /**
     * Setup cart button với nhiều ID khác nhau
     */
    private void setupCartButton() {
        ImageView cartButton = findViewByMultipleIds(
                R.id.ic_cart,
                R.id.img_cart
        );
        
        if (cartButton != null) {
            cartButton.setOnClickListener(v -> {
                Log.d(TAG, "Cart button clicked");
                navigateToCart();
            });
        }
    }

    /**
     * Setup favorite button
     */
    private void setupFavoriteButton() {
        // Tìm favorite button với nhiều ID khác nhau
        ImageView favoriteButton = findViewByMultipleIds(
                R.id.img_favorite,
                R.id.ic_favorite
        );
        if (favoriteButton != null) {
            favoriteButton.setOnClickListener(v -> {
                Log.d(TAG, "Favorite button clicked");
                navigateToFavorite();
            });
        }
    }

    /**
     * Setup notification button
     */
    private void setupNotificationButton() {
        // Tìm notification button với nhiều ID khác nhau
        ImageView notificationButton = findViewByMultipleIds(
                R.id.img_notification,
                R.id.ic_notification
        );
        if (notificationButton != null) {
            notificationButton.setOnClickListener(v -> {
                Log.d(TAG, "Notification button clicked");
                navigateToNotification();
            });
        }
    }

    /**
     * Tìm view bằng nhiều ID khác nhau (thử từng ID cho đến khi tìm thấy)
     */
    private ImageView findViewByMultipleIds(int... ids) {
        for (int id : ids) {
            ImageView view = activity.findViewById(id);
            if (view != null) {
                return view;
            }
        }
        return null;
    }

    /**
     * Navigate to Home
     */
    private void navigateToHome() {
        activity.startActivity(new Intent(activity, HomeActivity.class));
        // Không finish để có thể quay lại
    }

    /**
     * Navigate to Cart (có kiểm tra đăng nhập)
     */
    private void navigateToCart() {
        if (mAuth.getCurrentUser() != null) {
            activity.startActivity(new Intent(activity, CartActivity.class));
        } else {
            Toast.makeText(activity, "Vui lòng đăng nhập để xem Giỏ hàng.", Toast.LENGTH_SHORT).show();
            activity.startActivity(new Intent(activity, LoginActivity.class));
        }
    }

    /**
     * Navigate to Profile
     */
    private void navigateToProfile() {
        if (mAuth.getCurrentUser() != null) {
            activity.startActivity(new Intent(activity, ProfileActivity.class));
        } else {
            Toast.makeText(activity, "Vui lòng đăng nhập để xem Hồ sơ.", Toast.LENGTH_SHORT).show();
            activity.startActivity(new Intent(activity, LoginActivity.class));
        }
    }

    /**
     * Navigate to Favorite (có kiểm tra đăng nhập)
     */
    private void navigateToFavorite() {
        if (mAuth.getCurrentUser() != null) {
            activity.startActivity(new Intent(activity, FavoriteActivity.class));
        } else {
            Toast.makeText(activity, "Vui lòng đăng nhập để xem mục Yêu thích.", Toast.LENGTH_SHORT).show();
            activity.startActivity(new Intent(activity, LoginActivity.class));
        }
    }

    /**
     * Navigate to Notification (có kiểm tra đăng nhập)
     */
    private void navigateToNotification() {
        if (mAuth.getCurrentUser() != null) {
            activity.startActivity(new Intent(activity, NotificationActivity.class));
        } else {
            Toast.makeText(activity, "Vui lòng đăng nhập để xem thông báo.", Toast.LENGTH_SHORT).show();
            activity.startActivity(new Intent(activity, LoginActivity.class));
        }
    }

    /**
     * Ẩn header và footer (dùng cho các màn hình đặc biệt)
     */
    public void hideHeaderAndFooter() {
        View headerLayout = activity.findViewById(R.id.header_layout);
        if (headerLayout != null) {
            headerLayout.setVisibility(View.GONE);
        }

        View footerLayout = activity.findViewById(R.id.footer_layout);
        if (footerLayout != null) {
            footerLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Hiện header và footer
     */
    public void showHeaderAndFooter() {
        View headerLayout = activity.findViewById(R.id.header_layout);
        if (headerLayout != null) {
            headerLayout.setVisibility(View.VISIBLE);
        }

        View footerLayout = activity.findViewById(R.id.footer_layout);
        if (footerLayout != null) {
            footerLayout.setVisibility(View.VISIBLE);
        }
    }
}

