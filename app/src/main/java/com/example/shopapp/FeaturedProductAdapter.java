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
import java.util.Locale;

public class FeaturedProductAdapter extends RecyclerView.Adapter<FeaturedProductAdapter.FeaturedProductViewHolder> {

    private final List<Product> featuredProducts;

    public FeaturedProductAdapter(List<Product> featuredProducts) {
        this.featuredProducts = featuredProducts;
    }

    @NonNull
    @Override
    public FeaturedProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout mới: item_featured_product.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured_product, parent, false);
        return new FeaturedProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeaturedProductViewHolder holder, int position) {
        Product product = featuredProducts.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return featuredProducts.size();
    }

    // ViewHolder để giữ các View của mỗi sản phẩm
    public static class FeaturedProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgProduct;
        private final TextView textTitle;
        private final TextView textCurrentPrice;
        private final TextView textOriginalPrice;
        private final TextView textDescription;
        private final TextView textOfferDetails;
        private final TextView textDisclaimer;
        private final TextView textLimitedOfferBadge;

        public FeaturedProductViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các Views từ item_featured_product.xml
            imgProduct = itemView.findViewById(R.id.img_product);
            textTitle = itemView.findViewById(R.id.text_product_title);
            textCurrentPrice = itemView.findViewById(R.id.text_current_price);
            textOriginalPrice = itemView.findViewById(R.id.text_original_price);
            textDescription = itemView.findViewById(R.id.text_description);
            textOfferDetails = itemView.findViewById(R.id.text_offer_details);
            textDisclaimer = itemView.findViewById(R.id.text_disclaimer);
            textLimitedOfferBadge = itemView.findViewById(R.id.text_limited_offer_badge);
        }

        public void bind(Product product) {
            // Gán Tên
            textTitle.setText(product.name);

            // Sử dụng trường 'desc' cho mô tả
            textDescription.setText(product.desc);

            // Gán Giá
            String currentPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", product.currentPrice);
            String originalPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", product.originalPrice);
            textCurrentPrice.setText(currentPriceFormatted);
            textOriginalPrice.setText(originalPriceFormatted);

            // Gán Thông tin Khuyến mãi
            textOfferDetails.setText(product.offerDetails);
            textDisclaimer.setText(product.extraInfo);

            // Cập nhật Badge "LIMITED OFFER"
            if (product.isOffer) {
                textLimitedOfferBadge.setVisibility(View.VISIBLE);
            } else {
                textLimitedOfferBadge.setVisibility(View.GONE);
            }

            // Tải Ảnh bằng Picasso
            if (product.imageUrl != null && !product.imageUrl.isEmpty()) {
                Picasso.get()
                        .load(product.imageUrl)
                        // Lấy Context từ itemView
                        .error(R.drawable.ic_launcher_foreground)
                        .into(imgProduct);
            }
        }
    }
}