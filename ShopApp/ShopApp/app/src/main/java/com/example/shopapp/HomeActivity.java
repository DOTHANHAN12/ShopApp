package com.example.shopapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ViewPager2 viewPagerFeaturedProducts;
    private FeaturedProductAdapter adapter;
    private final List<Product> featuredProductsList = new ArrayList<>();

    private TextView tabWomen, tabMen, tabKids, tabBaby;
    private TextView currentSelectedTab;
    private View searchButton;
    private ImageView iconProfile;
//    private Button btnTestNotification;

    // Khai báo Icons Header
    private ImageView iconNotification;
    private ImageView iconFavorite;
    private ImageView iconCart;
    private FrameLayout notificationBadgeContainer;
    private TextView tvNotificationBadge;

    // --- Trình khởi chạy yêu cầu quyền thông báo ---
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Đã cấp quyền gửi thông báo!", Toast.LENGTH_SHORT).show();
                    subscribeToNotificationTopics(); // Đăng ký topic sau khi có quyền
                } else {
                    Toast.makeText(this, "Bạn đã từ chối quyền, sẽ không nhận được thông báo.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SETUP UI CƠ BẢN
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bắt đầu quy trình yêu cầu quyền và đăng ký topic
        askNotificationPermissionAndSubscribe();

        // 1. Ánh xạ Views và Icons Header
        mapViews();
        setupHeaderIcons();

        // Thêm listener cho nút test
//        btnTestNotification.setOnClickListener(v -> sendTestNotification());

        // 2. Khởi tạo Adapter và ViewPager2
        adapter = new FeaturedProductAdapter(featuredProductsList, this::isOfferValid);
        viewPagerFeaturedProducts.setAdapter(adapter);

        // 3. Thiết lập Listener cho các Tab
        setupCategoryTabs();

        // 4. Thiết lập Listener cho nút Search ở Footer
        setupSearchButton();

        // 5. Khởi tạo trạng thái UI cho tab mặc định và tải dữ liệu ban đầu
        String initialCategory = "MEN";
        updateCategoryUI(initialCategory);
        loadFeaturedProductsList(initialCategory);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update notification badge when returning to home
        updateNotificationBadge();
    }

    private void askNotificationPermissionAndSubscribe() {
        // Chỉ áp dụng cho Android 13 (API 33) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                subscribeToNotificationTopics();
            }
        } else {
            subscribeToNotificationTopics();
        }
    }

    private void subscribeToNotificationTopics() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            // ⭐️ Subscribe topic RIÊNG của user (để nhận thông báo đơn hàng)
            String userTopic = "user_" + userId;
            FirebaseMessaging.getInstance().subscribeToTopic(userTopic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "✅ Subscribed to user-specific topic: " + userTopic);
                        } else {
                            Log.e(TAG, "❌ Failed to subscribe to user-specific topic: " + userTopic, task.getException());
                        }
                    });

            // ⭐️ Subscribe topic BROADCAST (để nhận giảm giá, khuyến mại)
            // MyFirebaseMessagingService sẽ kiểm tra type để quyết định hiển thị push hay không
            FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "✅ Subscribed to 'all_users' broadcast topic");
                        } else {
                            Log.e(TAG, "❌ Failed to subscribe to 'all_users' topic", task.getException());
                        }
                    });

        } else {
            Log.w(TAG, "⚠️ User not logged in, cannot subscribe to notification topics.");
        }
    }


//    private void sendTestNotification() {
//        Intent intent = new Intent(this, HomeActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
//                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
//
//        String channelId = "fcm_default_channel";
//        NotificationCompat.Builder notificationBuilder =
//                new NotificationCompat.Builder(this, channelId)
//                        .setSmallIcon(R.drawable.ic_notification)
//                        .setContentTitle("Thông báo kiểm tra (Local)")
//                        .setContentText("Nếu bạn thấy thông báo này, nghĩa là quyền và icon đã đúng.")
//                        .setAutoCancel(true)
//                        .setPriority(NotificationCompat.PRIORITY_HIGH)
//                        .setContentIntent(pendingIntent);
//
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(channelId,
//                    "Thông báo chung",
//                    NotificationManager.IMPORTANCE_HIGH);
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
//                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
//            notificationManager.notify(1, notificationBuilder.build());
//            Toast.makeText(this, "Đã gửi thông báo local!", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(this, "Chưa được cấp quyền gửi thông báo.", Toast.LENGTH_LONG).show();
//        }
//    }

    private void mapViews() {
        viewPagerFeaturedProducts = findViewById(R.id.view_pager_featured_products);

        tabWomen = findViewById(R.id.tab_women);
        tabMen = findViewById(R.id.tab_men);
        tabKids = findViewById(R.id.tab_kids);
        tabBaby = findViewById(R.id.tab_baby);
//        btnTestNotification = findViewById(R.id.btn_test_notification);

        searchButton = findViewById(R.id.btn_search_footer);

        iconNotification = findViewById(R.id.ic_notification);
        iconFavorite = findViewById(R.id.ic_favorite);
        iconCart = findViewById(R.id.ic_cart);
        iconProfile = findViewById(R.id.nav_profile);

        currentSelectedTab = tabMen;
    }

    private void setupHeaderIcons() {
        // NÚT YÊU THÍCH (Favorite)
        if (iconFavorite != null) {
            iconFavorite.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() != null) {
                    startActivity(new Intent(this, FavoriteActivity.class));
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để xem mục Yêu thích.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                }
            });
        }

        // NÚT GIỎ HÀNG (Cart)
        if (iconCart != null) {
            iconCart.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() != null) {
                    startActivity(new Intent(this, CartActivity.class));
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để xem Giỏ hàng.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                }
            });
        }

        // NÚT THÔNG BÁO (Notification) - CẬP NHẬT
        if (iconNotification != null) {
            iconNotification.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() != null) {
                    Intent intent = new Intent(this, NotificationActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để xem thông báo.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                }
            });

            // Update badge initially
            updateNotificationBadge();
        }

        // NÚT PROFILE
        if (iconProfile != null) {
            iconProfile.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() != null) {
                    startActivity(new Intent(this, ProfileActivity.class));
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để xem thông tin cá nhân.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                }
            });
        }
    }

    private void updateNotificationBadge() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int unreadCount = queryDocumentSnapshots.size();

                    // Bạn có thể thêm badge UI vào layout nếu muốn
                    // Ví dụ: hiển thị số lượng thông báo chưa đọc
                    Log.d(TAG, "Unread notifications: " + unreadCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting unread count", e);
                });
    }

    private void setupCategoryTabs() {
        View.OnClickListener tabClickListener = view -> {
            String category = ((TextView) view).getText().toString();
            updateCategoryUI(category);
            loadFeaturedProductsList(category);
        };

        tabWomen.setOnClickListener(tabClickListener);
        tabMen.setOnClickListener(tabClickListener);
        tabKids.setOnClickListener(tabClickListener);
        tabBaby.setOnClickListener(tabClickListener);
    }

    private void setupSearchButton() {
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> {
                String categoryToSearch = (currentSelectedTab != null) ? currentSelectedTab.getText().toString() : "MEN";
                Intent intent = new Intent(HomeActivity.this, CategorySearchActivity.class);
                intent.putExtra("CATEGORY_KEY", categoryToSearch);
                startActivity(intent);
            });
        }
    }

    private void updateCategoryUI(String newCategory) {
        if (currentSelectedTab != null) {
            currentSelectedTab.setAlpha(0.7f);
            currentSelectedTab.setPaintFlags(currentSelectedTab.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            currentSelectedTab.setTextSize(16);
        }

        switch (newCategory) {
            case "WOMEN": currentSelectedTab = tabWomen; break;
            case "MEN": currentSelectedTab = tabMen; break;
            case "KIDS": currentSelectedTab = tabKids; break;
            case "BABY": currentSelectedTab = tabBaby; break;
            default: return;
        }

        currentSelectedTab.setAlpha(1.0f);
        currentSelectedTab.setPaintFlags(currentSelectedTab.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        currentSelectedTab.setTextSize(18);
    }

    private boolean isOfferValid(OfferDetails offer) {
        if (offer == null || offer.getStartDate() == null || offer.getEndDate() == null) {
            return false;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = sdf.parse(offer.getStartDate());
            Date endDate = sdf.parse(offer.getEndDate());
            Date currentDate = new Date();
            return !currentDate.before(startDate) && !currentDate.after(endDate);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing offer dates", e);
            return false;
        }
    }

    private void loadFeaturedProductsList(String category) {
        db.collection("products")
                .whereEqualTo("isFeatured", true)
                .whereEqualTo("category", category)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        featuredProductsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Product product = new Product();
                                product.setProductId(document.getString("productId"));
                                product.setName(document.getString("name"));
                                product.setDesc(document.getString("desc"));

                                if (document.get("basePrice") instanceof Number) {
                                    product.setBasePrice(((Number) document.get("basePrice")).doubleValue());
                                }

                                product.setMainImage(document.getString("mainImage"));
                                product.setCategory(document.getString("category"));
                                product.setType(document.getString("type"));
                                product.setStatus(document.getString("status"));

                                if (document.getBoolean("isOffer") != null) {
                                    product.setIsOfferStatus(document.getBoolean("isOffer"));
                                }

                                if (document.get("averageRating") instanceof Number) {
                                    product.setAverageRating(((Number) document.get("averageRating")).doubleValue());
                                }

                                product.setTotalReviews(document.getLong("totalReviews"));

                                if (document.getBoolean("isFeatured") != null) {
                                    product.setFeatured(document.getBoolean("isFeatured"));
                                }

                                product.setColorImages((Map<String, List<String>>) document.get("colorImages"));
                                product.setCreatedAt(document.getLong("createdAt"));
                                product.setUpdatedAt(document.getLong("updatedAt"));

                                if (document.contains("offer")) {
                                    product.setOffer(document.get("offer", OfferDetails.class));
                                }

                                if (document.contains("variants")) {
                                    List<Map<String, Object>> variantMaps = (List<Map<String, Object>>) document.get("variants");
                                    List<ProductVariant> variants = new ArrayList<>();
                                    for (Map<String, Object> map : variantMaps) {
                                        ProductVariant variant = new ProductVariant();
                                        variant.setVariantId((String) map.get("variantId"));
                                        variant.setSize((String) map.get("size"));
                                        variant.setColor((String) map.get("color"));
                                        variant.setQuantity((Long) map.get("quantity"));

                                        if (map.get("price") instanceof Number) {
                                            variant.setPrice(((Number) map.get("price")).doubleValue());
                                        }

                                        if (map.containsKey("status")) {
                                            variant.setStatus((String) map.get("status"));
                                        }
                                        variants.add(variant);
                                    }
                                    product.setVariants(variants);
                                }
                                featuredProductsList.add(product);
                            } catch (Exception e) {
                                Log.e(TAG, "Could not parse product", e);
                            }
                        }

                        if (!featuredProductsList.isEmpty()) {
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.w(TAG, "Không tìm thấy sản phẩm nổi bật nào cho danh mục " + category);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi tải danh sách sản phẩm nổi bật.", task.getException());
                    }
                });
    }
}