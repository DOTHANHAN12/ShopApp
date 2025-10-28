package com.example.shopapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ProductImageSliderAdapter extends RecyclerView.Adapter<ProductImageSliderAdapter.ImageViewHolder> {

    private final List<String> imageUrls;
    private Product currentProduct;

    public ProductImageSliderAdapter(List<String> imageUrls, Product currentProduct) {
        this.imageUrls = imageUrls;
        this.currentProduct = currentProduct;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Layout: item_product_image_slide.xml (Bạn phải tạo file này)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_image_slide, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String url = imageUrls.get(position);

        // Tải ảnh chi tiết
        Picasso.get()
                .load(url)
                .error(R.drawable.ic_launcher_foreground)
                .into(holder.imageView);

        // Hiển thị chi tiết nhỏ (ví dụ: Product ID) trên ảnh
        if (holder.textProductId != null && currentProduct != null) {
            holder.textProductId.setText(String.format("Product ID: %s", currentProduct.productId));
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textProductId;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            // Giả định ID:
            imageView = itemView.findViewById(R.id.img_slide);
            textProductId = itemView.findViewById(R.id.text_product_id_overlay);
        }
    }
}