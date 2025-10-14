package com.example.shopapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavoriteActivity extends AppCompatActivity {

    private static final String TAG = "FavoriteActivity";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private FavoriteAdapter adapter;
    private final List<FavoriteItem> favoriteItemList = new ArrayList<>();

    private TextView textItemCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        // Fix Status Bar icons cho nền trắng
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        mapViews();
        setupRecyclerView();
        loadFavoriteItems();
    }

    private void mapViews() {
        recyclerView = findViewById(R.id.recycler_wish_list);
        textItemCount = findViewById(R.id.text_item_count);
        ImageView imgBack = findViewById(R.id.img_back);

        imgBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        // Lấy userId để truyền vào Adapter
        FirebaseUser user = mAuth.getCurrentUser();
        String userId = (user != null) ? user.getUid() : null;

        if (userId != null) {
            // *** SỬA ĐỔI: TRUYỀN USERID VÀO CONSTRUCTOR CỦA ADAPTER ***
            adapter = new FavoriteAdapter(this, favoriteItemList, userId);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        } else {
            // Xử lý trường hợp người dùng chưa đăng nhập
            Toast.makeText(this, "Vui lòng đăng nhập để xem mục Yêu thích.", Toast.LENGTH_LONG).show();
        }
    }

    private void loadFavoriteItems() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        final String userId = user.getUid();

        // Truy vấn subcollection favorites
        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        favoriteItemList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Document ID là productId
                            FavoriteItem item = document.toObject(FavoriteItem.class);

                            // *** DÒNG SỬA ĐỔI: Lưu Product ID ***
                            item.setProductId(document.getId());

                            favoriteItemList.add(item);
                        }

                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        textItemCount.setText(String.format(Locale.ROOT, "%d ITEM(S)", favoriteItemList.size()));
                    } else {
                        Log.e(TAG, "Lỗi tải Yêu thích: ", task.getException());
                    }
                });
    }
}