package com.example.shopapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
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
            Log.e("ProductAdapter", "Error parsing offer dates", e);
            return false;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        // 1. Gán dữ liệu văn bản
        holder.nameTextView.setText(product.getName());
        holder.typeTextView.setText(product.getType());

        // -----------------------------------------------------------
        // LOGIC TÍNH TOÁN VÀ GÁN GIÁ MỚI
        // -----------------------------------------------------------

        double basePrice = product.getBasePrice();
        double displayPrice = basePrice;
        boolean isDiscounted = false;

        // Kiểm tra xem khuyến mãi có hợp lệ không
        if (product.getIsOfferStatus() && isOfferValid(product.getOffer())) {
            double discount = product.getOffer().getOfferValue() / 100.0;
            displayPrice = basePrice * (1.0 - discount);
            isDiscounted = true;
        }

        // Định dạng và gán Giá Hiển thị (Giá sau giảm hoặc Giá gốc)
        String currentPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", displayPrice);
        holder.currentPriceTextView.setText(currentPriceFormatted);

        // Gán giá gốc và gạch ngang
        if (isDiscounted) {
            String originalPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", basePrice);

            holder.originalPriceTextView.setText(originalPriceFormatted);
            // Thêm hiệu ứng gạch ngang
            holder.originalPriceTextView.setPaintFlags(
                    holder.originalPriceTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        } else {
            // Xóa giá gốc nếu không có khuyến mãi hợp lệ
            holder.originalPriceTextView.setText("");
            holder.originalPriceTextView.setPaintFlags(0);
        }

        // -----------------------------------------------------------

        // 2. Tải ảnh bằng Picasso - SỬ DỤNG TRƯỜNG mainImage
        if (product.getMainImage() != null && !product.getMainImage().isEmpty()) {
            Picasso.get()
                    .load(product.getMainImage())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.thumbnailImageView);
        }

        // 3. XỬ LÝ CLICK: Mở màn hình chi tiết sản phẩm và truyền ID
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            // Truyền Product ID của sản phẩm được click
            intent.putExtra("PRODUCT_ID", product.getProductId());
            context.startActivity(intent);
        });

        // 4. XỬ LÝ NÚT YÊU THÍCH
        checkFavoriteStatus(product.getProductId(), holder.favoriteImageView);

        holder.favoriteImageView.setOnClickListener(v -> {
            toggleFavorite(product.getProductId(), holder.favoriteImageView);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    private void toggleFavorite(String productId, ImageView favoriteImageView) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để thêm vào Yêu thích.", Toast.LENGTH_SHORT).show();
            context.startActivity(new Intent(context, LoginActivity.class));
            return;
        }

        db.collection("users").document(user.getUid()).collection("favorites").document(productId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            // Bỏ yêu thích
                            db.collection("users").document(user.getUid()).collection("favorites").document(productId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        updateFavoriteIcon(false, favoriteImageView);
                                    });
                        } else {
                            // Thêm vào yêu thích
                            db.collection("users").document(user.getUid()).collection("favorites").document(productId)
                                    .set(new FavoriteItem(System.currentTimeMillis()))
                                    .addOnSuccessListener(aVoid -> {
                                        updateFavoriteIcon(true, favoriteImageView);
                                    });
                        }
                    }
                });
    }

    private void checkFavoriteStatus(String productId, ImageView favoriteImageView) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            updateFavoriteIcon(false, favoriteImageView);
            return;
        }

        db.collection("users").document(user.getUid()).collection("favorites").document(productId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        updateFavoriteIcon(document != null && document.exists(), favoriteImageView);
                    }
                });
    }

    private void updateFavoriteIcon(boolean isFavorited, ImageView favoriteImageView) {
        if (isFavorited) {
            favoriteImageView.setImageResource(R.drawable.ic_favorite_filled);
            favoriteImageView.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimaryDark), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            favoriteImageView.setImageResource(R.drawable.ic_favorite_outline);
            favoriteImageView.clearColorFilter();
        }
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImageView;
        ImageView favoriteImageView;
        TextView nameTextView;
        TextView typeTextView;
        TextView currentPriceTextView;
        TextView originalPriceTextView;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.img_product_thumb);
            favoriteImageView = itemView.findViewById(R.id.img_favorite_list);
            nameTextView = itemView.findViewById(R.id.text_product_name_list);
            typeTextView = itemView.findViewById(R.id.text_product_type_list);
            currentPriceTextView = itemView.findViewById(R.id.text_current_price_list);
            originalPriceTextView = itemView.findViewById(R.id.text_original_price_list);
        }
    }
}
