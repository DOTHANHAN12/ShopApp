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
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<CartItem> cartItemList;
    private final Context context;
    // Interface để gửi sự kiện thay đổi/xóa về CartActivity
    private final OnCartActionListener listener;

    public interface OnCartActionListener {
        void onItemDeleted(CartItem item);
        void onQuantityChanged(CartItem item, int newQuantity);
    }

    public CartAdapter(Context context, List<CartItem> cartItemList, OnCartActionListener listener) {
        this.context = context;
        this.cartItemList = cartItemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item_cart.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItemList.get(position);

        // GIẢ ĐỊNH: Dữ liệu sản phẩm chi tiết (name, image) được lấy từ CartItem
        // Trong thực tế, bạn cần truy vấn Product Detail bằng item.productId

        holder.nameTextView.setText("Rayon Blouse | Striped"); // Placeholder
        holder.colorSizeTextView.setText(String.format("Color: 04 GRAY | Size: %s", item.variantId.substring(6, 7)));
        holder.quantityTextView.setText(String.valueOf(item.quantity));
        holder.priceTextView.setText(String.format(Locale.getDefault(), "%,.0f VND", item.priceAtTimeOfAdd));
        holder.subtotalTextView.setText(String.format(Locale.getDefault(), "SUBTOTAL: %,.0f VND", item.priceAtTimeOfAdd * item.quantity));

        // Tải ảnh (Placeholder)
        Picasso.get().load("URL_PLACEHOLDER_IMAGE").into(holder.imageView);

        // Xử lý nút Xóa (X)
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemDeleted(item);
            }
        });

        // Xử lý thay đổi số lượng (Cần thêm logic Dropdown hoặc +/- buttons)
        // Hiện tại chỉ là TextView, cần thay bằng Spinner hoặc custom buttons
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameTextView, colorSizeTextView, quantityTextView, priceTextView, subtotalTextView;
        ImageView deleteButton;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_cart_product);
            nameTextView = itemView.findViewById(R.id.text_cart_product_name);
            colorSizeTextView = itemView.findViewById(R.id.text_cart_color_size);
            quantityTextView = itemView.findViewById(R.id.text_cart_quantity);
            priceTextView = itemView.findViewById(R.id.text_cart_price);
            subtotalTextView = itemView.findViewById(R.id.text_cart_subtotal);
            deleteButton = itemView.findViewById(R.id.btn_delete_item);
        }
    }
}