package com.example.shopapp;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> productList;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        // 1. Gán dữ liệu văn bản
        holder.nameTextView.setText(product.name);
        holder.typeTextView.setText(product.type);

        String currentPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", product.currentPrice);
        String originalPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", product.originalPrice);

        holder.currentPriceTextView.setText(currentPriceFormatted);

        if (product.currentPrice < product.originalPrice) {
            holder.originalPriceTextView.setText(originalPriceFormatted);
            holder.originalPriceTextView.setPaintFlags(
                    holder.originalPriceTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        } else {
            holder.originalPriceTextView.setText("");
            holder.originalPriceTextView.setPaintFlags(0);
        }

        // 2. Tải ảnh bằng Picasso - SỬ DỤNG TRƯỜNG mainImage MỚI
        if (product.mainImage != null && !product.mainImage.isEmpty()) {
            Picasso.get()
                    .load(product.mainImage)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.thumbnailImageView);
        }

        // 3. Xử lý click (Ví dụ: Mở màn hình chi tiết sản phẩm)
        holder.itemView.setOnClickListener(v -> {
            // Logic chuyển sang ProductDetailActivity, truyền product.productId
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImageView;
        TextView nameTextView;
        TextView typeTextView;
        TextView currentPriceTextView;
        TextView originalPriceTextView;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.img_product_thumb);
            nameTextView = itemView.findViewById(R.id.text_product_name_list);
            typeTextView = itemView.findViewById(R.id.text_product_type_list);
            currentPriceTextView = itemView.findViewById(R.id.text_current_price_list);
            originalPriceTextView = itemView.findViewById(R.id.text_original_price_list);
        }
    }
}