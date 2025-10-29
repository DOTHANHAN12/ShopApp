package com.example.shopapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

public class OrderProductAdapter extends RecyclerView.Adapter<OrderProductAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> products;
    private final String orderStatus;
    private final String orderId;

    public OrderProductAdapter(Context context, List<Map<String, Object>> products, String orderStatus, String orderId) {
        this.context = context;
        this.products = products;
        this.orderStatus = orderStatus;
        this.orderId = orderId;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Map<String, Object> product = products.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productName;
        private final TextView productQuantity;
        private final Button btnWriteReview;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.image_product);
            productName = itemView.findViewById(R.id.text_product_name);
            productQuantity = itemView.findViewById(R.id.text_product_quantity);
            btnWriteReview = itemView.findViewById(R.id.btn_write_review);
        }

        public void bind(Map<String, Object> product) {
            productName.setText((String) product.get("name"));
            Object quantity = product.get("quantity");
            if (quantity instanceof Long) {
                productQuantity.setText("Số lượng: " + quantity);
            } else if (quantity instanceof Double) {
                productQuantity.setText("Số lượng: " + ((Double) quantity).intValue());
            } else {
                productQuantity.setText("Số lượng: N/A");
            }

            String imageUrl = (String) product.get("imageUrl");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .into(productImage);
            } else {
                productImage.setImageResource(R.drawable.placeholder_image);
            }

            // Show 'Write Review' button only if the order is DELIVERED
            if ("DELIVERED".equalsIgnoreCase(orderStatus)) {
                btnWriteReview.setVisibility(View.VISIBLE);
                btnWriteReview.setOnClickListener(v -> {
                    Intent intent = new Intent(context, WriteReviewActivity.class);
                    intent.putExtra("PRODUCT_ID", (String) product.get("productId"));
                    intent.putExtra("ORDER_ID", orderId);
                    context.startActivity(intent);
                });
            } else {
                btnWriteReview.setVisibility(View.GONE);
            }
        }
    }
}
