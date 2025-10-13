package com.example.shopapp;

import android.content.Context;
import android.content.Intent;
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
        holder.nameTextView.setText(product.getName());
        holder.typeTextView.setText(product.getType());

        // -----------------------------------------------------------
        // LOGIC TÍNH TOÁN VÀ GÁN GIÁ MỚI
        // -----------------------------------------------------------

        double basePrice = product.getBasePrice();
        double displayPrice = basePrice;
        boolean isDiscounted = false;

        // Kiểm tra xem khuyến mãi có hợp lệ không
        if (product.getIsOfferStatus() && product.getOffer() != null) {
            long now = System.currentTimeMillis();
            OfferDetails offer = product.getOffer();

            // Chỉ tính giảm giá nếu khuyến mãi đang diễn ra (kiểm tra thời gian)
            if (now >= offer.getStartDate() && now <= offer.getEndDate()) {
                double discount = offer.getDiscountPercent() / 100.0;
                displayPrice = basePrice * (1.0 - discount);
                isDiscounted = true;
            }
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