package com.example.shopapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * RecyclerView Adapter for displaying products
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private static final String TAG = "ProductAdapter";

    private List<Product> products;
    private Context context;

    public ProductAdapter(List<Product> products, Context context) {
        this.products = products;
        this.context = context;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        try {
            Product product = products.get(position);

            if (product == null) {
                return;
            }

            // Set product name
            if (holder.productName != null) {
                holder.productName.setText(product.name != null ? product.name : "");
            }

            // Set product price
            if (holder.productPrice != null) {
                holder.productPrice.setText(formatPrice(product.basePrice));
            }

            // Set rating
            if (holder.productRating != null) {
                String ratingText = product.averageRating != null
                        ? String.format("%.1f⭐", product.averageRating)
                        : "No rating";
                holder.productRating.setText(ratingText);
            }

            // Set review count
            if (holder.reviewCount != null) {
                String reviewText = product.totalReviews != null
                        ? "(" + product.totalReviews + " reviews)"
                        : "(No reviews)";
                holder.reviewCount.setText(reviewText);
            }

            // Load product image
            if (holder.productImage != null) {
                if (product.mainImage != null && !product.mainImage.isEmpty()) {
                    Picasso.get()
                            .load(product.mainImage)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .fit()
                            .centerCrop()
                            .into(holder.productImage);
                } else {
                    holder.productImage.setImageResource(R.drawable.ic_launcher_foreground);
                }
            }

            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                // Navigate to product detail
                Intent intent = new Intent(context, ProductDetailActivity.class);
                intent.putExtra("PRODUCT_ID", product.productId);
                context.startActivity(intent);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    /**
     * Update product list with new data
     */
    public void updateData(List<Product> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    /**
     * Format price to display string
     */
    private String formatPrice(double price) {
        if (price >= 1000000) {
            return String.format("%.1fM đ", price / 1000000);
        } else if (price >= 1000) {
            return String.format("%.0fK đ", price / 1000);
        }
        return String.format("%.0f đ", price);
    }

    /**
     * ViewHolder for product items
     */
    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName;
        TextView productPrice;
        TextView productRating;
        TextView reviewCount;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            productImage = itemView.findViewById(R.id.img_product);
            productName = itemView.findViewById(R.id.txt_product_name);
            productPrice = itemView.findViewById(R.id.txt_product_price);
            productRating = itemView.findViewById(R.id.txt_product_rating);
            reviewCount = itemView.findViewById(R.id.txt_review_count);

            // Log if views not found
            if (productImage == null) {
                android.util.Log.e("ProductAdapter", "img_product not found in item_product.xml");
            }
            if (productName == null) {
                android.util.Log.e("ProductAdapter", "txt_product_name not found in item_product.xml");
            }
            if (productPrice == null) {
                android.util.Log.e("ProductAdapter", "txt_product_price not found in item_product.xml");
            }
        }
    }
}