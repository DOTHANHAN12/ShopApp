package com.example.shopapp;

import android.content.Context;
import android.util.Log;
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
import java.util.Locale;
import java.util.Map;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<CartItem> cartItemList;
    private final Context context;
    private final OnCartActionListener listener;
    private static final int MAX_QUANTITY = 99;

    public interface OnCartActionListener {
        void onItemDeleted(CartItem item);
        void onQuantityChanged(CartItem item, int newQuantity);
        void onCartUpdated();
    }

    public CartAdapter(Context context, List<CartItem> cartItemList, OnCartActionListener listener) {
        this.context = context;
        this.cartItemList = cartItemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItemList.get(position);

        Product product = item.getProductDetails();
        ProductVariant variant = item.getVariantDetails();

        // --- LOGIC HIỂN THỊ DỮ LIỆU THỰC TẾ ---

        if (product != null) {
            holder.nameTextView.setText(product.getName());

            // *** SỬA ĐỔI: LẤY ẢNH ĐẦU TIÊN CỦA MÀU SẮC ĐÃ CHỌN ***
            String imageUrl = product.getMainImage(); // Dùng tạm mainImage làm fallback

            if (variant != null && variant.getColor() != null && product.getColorImages() != null) {
                String colorName = variant.getColor();
                Map<String, List<String>> colorImages = product.getColorImages();

                if (colorImages.containsKey(colorName)) {
                    List<String> images = colorImages.get(colorName);
                    if (images != null && !images.isEmpty()) {
                        // Lấy ảnh đầu tiên của màu sắc đó
                        imageUrl = images.get(0);
                    }
                }
            }

            // Tải ảnh
            Picasso.get().load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .into(holder.imageView);
        } else {
            holder.nameTextView.setText("Sản phẩm bị lỗi/Đang tải");
            holder.imageView.setImageResource(R.drawable.ic_broken_image);
        }

        // Màu sắc và Size
        if (variant != null) {
            holder.colorSizeTextView.setText(String.format("Color: %s | Size: %s", variant.getColor(), variant.getSize()));
        } else {
            holder.colorSizeTextView.setText("Biến thể không xác định");
        }

        // Số lượng, Giá, và Tổng phụ
        holder.quantityTextView.setText(String.valueOf(item.getQuantity()));
        holder.priceTextView.setText(String.format(Locale.getDefault(), "%,.0f VND", item.getPriceAtTimeOfAdd()));

        double subtotal = item.getPriceAtTimeOfAdd() * item.getQuantity();
        holder.subtotalTextView.setText(String.format(Locale.getDefault(), "SUBTOTAL: %,.0f VND", subtotal));
        // --- KẾT THÚC LOGIC HIỂN THỊ DỮ LIỆU THỰC TẾ ---


        // *** Xử lý nút Xóa (X) ***
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemDeleted(item);
            }
        });

        // *** Xử lý Tăng số lượng (+) ***
        holder.btnQuantityPlus.setOnClickListener(v -> {
            int currentQuantity = item.getQuantity();
            // Lấy tồn kho thực tế của biến thể để kiểm tra
            long maxStock = (variant != null) ? variant.getQuantity() : MAX_QUANTITY;

            if (currentQuantity < maxStock && currentQuantity < MAX_QUANTITY) {
                int newQuantity = currentQuantity + 1;
                if (listener != null) {
                    listener.onQuantityChanged(item, newQuantity);
                }
            } else {
                Toast.makeText(context, "Số lượng đã đạt giới hạn tồn kho hoặc tối đa.", Toast.LENGTH_SHORT).show();
            }
        });

        // *** Xử lý Giảm số lượng (-) ***
        holder.btnQuantityMinus.setOnClickListener(v -> {
            int currentQuantity = item.getQuantity();
            if (currentQuantity > 1) {
                int newQuantity = currentQuantity - 1;
                if (listener != null) {
                    listener.onQuantityChanged(item, newQuantity);
                }
            } else if (currentQuantity == 1) {
                Toast.makeText(context, "Bấm nút Xóa (X) để loại bỏ sản phẩm.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameTextView, colorSizeTextView, quantityTextView, priceTextView, subtotalTextView;
        ImageView deleteButton;
        TextView btnQuantityMinus, btnQuantityPlus;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_cart_product);
            nameTextView = itemView.findViewById(R.id.text_cart_product_name);
            colorSizeTextView = itemView.findViewById(R.id.text_cart_color_size);
            quantityTextView = itemView.findViewById(R.id.text_cart_quantity);
            priceTextView = itemView.findViewById(R.id.text_cart_price);
            subtotalTextView = itemView.findViewById(R.id.text_cart_subtotal);
            deleteButton = itemView.findViewById(R.id.btn_delete_item);

            btnQuantityMinus = itemView.findViewById(R.id.btn_quantity_minus);
            btnQuantityPlus = itemView.findViewById(R.id.btn_quantity_plus);
        }
    }
}