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
        // Layout item_product.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);

        // Logic xử lý click item nếu có
        holder.itemView.setOnClickListener(v -> {
            // Ví dụ: Chuyển sang màn hình chi tiết sản phẩm
            // Intent intent = new Intent(context, ProductDetailActivity.class);
            // intent.putExtra("PRODUCT_ID", product.productId);
            // context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        // Sửa tên biến để dễ quản lý hơn, nhưng quan trọng là ID ánh xạ
        private final ImageView imgProduct;
        private final TextView textName;
        private final TextView textType;
        private final TextView textCurrentPrice;
        private final TextView textOriginalPrice;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            // ÁNH XẠ ĐÚNG THEO ID TRONG item_product.xml BẠN VỪA GỬI
            imgProduct = itemView.findViewById(R.id.img_product_thumb); // ID ảnh
            textName = itemView.findViewById(R.id.text_product_name_list); // ID tên
            textType = itemView.findViewById(R.id.text_product_type_list); // ID loại (type)
            textCurrentPrice = itemView.findViewById(R.id.text_current_price_list); // ID giá hiện tại
            textOriginalPrice = itemView.findViewById(R.id.text_original_price_list); // ID giá gốc

            // Thêm gạch ngang cho giá gốc
            textOriginalPrice.setPaintFlags(textOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        public void bind(Product product) {
            textName.setText(product.name);
            textType.setText(product.type);

            // Gán Giá Hiện Tại
            String currentPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", product.currentPrice);
            textCurrentPrice.setText(currentPriceFormatted);

            // Gán Giá Gốc
            String originalPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", product.originalPrice);
            textOriginalPrice.setText(originalPriceFormatted);

            // Tải Ảnh bằng Picasso (Sử dụng mainImage cho danh sách sản phẩm)
            if (product.images != null && product.images.mainImage != null && !product.images.mainImage.isEmpty()) {
                Picasso.get()
                        .load(product.images.mainImage)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(imgProduct);
            } else {
                imgProduct.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
    }
}