package com.example.shopapp;

import android.content.Context;
import android.content.Intent; // Import Intent
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {

    private final List<FavoriteItem> favoriteItemList;
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Biến lưu trữ ID người dùng
    private final String userId;

    // *** SỬA ĐỔI CONSTRUCTOR: Nhận userId ***
    public FavoriteAdapter(Context context, List<FavoriteItem> favoriteItemList, String userId) {
        this.context = context;
        this.favoriteItemList = favoriteItemList;
        this.userId = userId;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wish_list, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        FavoriteItem item = favoriteItemList.get(position);
        final String productId = item.getProductId();

        // Đặt trạng thái tải (Loading state)
        holder.nameTextView.setText("Đang tải...");
        holder.productIdTextView.setText("Product ID: " + productId);
        holder.colorTextView.setText("");

        // *** LOGIC TRUY VẤN CHI TIẾT SẢN PHẨM ***
        if (productId != null) {
            db.collection("products").document(productId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Product product = documentSnapshot.toObject(Product.class);

                            if (product != null) {
                                holder.nameTextView.setText(product.getName());
                                holder.colorTextView.setText(String.format(Locale.ROOT, "Price: $%.2f", product.getBasePrice()));

                                Picasso.get().load(product.getMainImage())
                                        .placeholder(R.drawable.ic_placeholder) // Giả định drawable tồn tại
                                        .error(R.drawable.ic_broken_image) // Giả định drawable tồn tại
                                        .into(holder.imageView);
                            }
                        } else {
                            // Sản phẩm bị xóa khỏi kho hàng
                            holder.nameTextView.setText("Sản phẩm đã bị xóa");
                            holder.colorTextView.setText("Không có sẵn.");
                            holder.imageView.setImageResource(R.drawable.ic_broken_image);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FavoriteAdapter", "Lỗi tải chi tiết SP cho ID: " + productId, e);
                        holder.nameTextView.setText("Lỗi tải thông tin");
                        holder.colorTextView.setText("Vui lòng thử lại.");
                        holder.imageView.setImageResource(R.drawable.ic_broken_image);
                    });
        }

        // *** LOGIC CLICK VÀO ITEM ĐỂ XEM CHI TIẾT ***
        holder.itemView.setOnClickListener(v -> {
            if (productId != null) {
                // Tạo Intent để chuyển sang DetailActivity
                // THAY THẾ DetailActivity.class bằng Activity chi tiết sản phẩm thực tế của bạn
                Intent intent = new Intent(context, ProductDetailActivity.class);

                // Gắn Product ID
                intent.putExtra("PRODUCT_ID", productId);

                context.startActivity(intent);
            }
        });

        // *** LOGIC XÓA SẢN PHẨM (BẤM VÀO ICON TIM) ***
        holder.heartIcon.setOnClickListener(v -> {
            if (productId != null && userId != null) {
                deleteFavoriteItem(productId, position);
            } else {
                Toast.makeText(context, "Lỗi: Không tìm thấy ID.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Phương thức xóa sản phẩm khỏi Firestore và Adapter
    private void deleteFavoriteItem(String productId, int position) {
        db.collection("users").document(userId).collection("favorites").document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Đã xóa sản phẩm khỏi Yêu thích.", Toast.LENGTH_SHORT).show();

                    // Cập nhật danh sách và giao diện
                    favoriteItemList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, favoriteItemList.size());
                })
                .addOnFailureListener(e -> {
                    Log.e("FavoriteAdapter", "Lỗi xóa sản phẩm: " + e.getMessage());
                    Toast.makeText(context, "Lỗi xóa sản phẩm. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
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