package com.example.shopapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {

    private final List<FavoriteItem> favoriteItemList;
    private final Context context;
    // Giả định bạn cần Product Detail để hiển thị tên, ảnh, và ID
    // Trong thực tế, bạn sẽ cần List<Product> hoặc List<Map<String, Object>>

    public FavoriteAdapter(Context context, List<FavoriteItem> favoriteItemList) {
        this.context = context;
        this.favoriteItemList = favoriteItemList;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item_wish_list.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_wish_list, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        FavoriteItem item = favoriteItemList.get(position);

        // VÍ DỤ: Cần truy vấn Firestore để lấy Product Detail bằng item.productId (nếu có)

        // Placeholder Data
        holder.nameTextView.setText("Rayon Blouse | Striped");
        holder.productIdTextView.setText("Product ID: 479072");
        holder.colorTextView.setText("Color: 04 GRAY");

        // Tải ảnh (Placeholder)
        Picasso.get().load("URL_PLACEHOLDER_IMAGE").into(holder.imageView);

        // Xử lý nút xóa Yêu thích (Tim)
        holder.heartIcon.setOnClickListener(v -> {
            Toast.makeText(context, "Xóa khỏi Yêu thích: " + holder.productIdTextView.getText(), Toast.LENGTH_SHORT).show();
            // TODO: Gửi yêu cầu xóa item.productId khỏi Subcollection favorites
        });
    }

    @Override
    public int getItemCount() {
        return favoriteItemList.size();
    }

    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, heartIcon;
        TextView nameTextView, productIdTextView, colorTextView;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_product_thumb);
            heartIcon = itemView.findViewById(R.id.img_heart);
            nameTextView = itemView.findViewById(R.id.text_name);
            productIdTextView = itemView.findViewById(R.id.text_product_id);
            colorTextView = itemView.findViewById(R.id.text_color);
        }
    }
}