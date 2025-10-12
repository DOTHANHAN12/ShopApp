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
            textTitle.setText(product.name);
            textDescription.setText(product.desc);

            String currentPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", product.currentPrice);
            String originalPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", product.originalPrice);
            textCurrentPrice.setText(currentPriceFormatted);
            textOriginalPrice.setText(originalPriceFormatted);

            textOfferDetails.setText(product.offerDetails);
            textDisclaimer.setText(product.extraInfo);

            if (product.isOffer) {
                textLimitedOfferBadge.setVisibility(View.VISIBLE);
            } else {
                textLimitedOfferBadge.setVisibility(View.GONE);
            }

            // Tải Ảnh bằng Picasso - SỬ DỤNG TRƯỜNG mainImage MỚI
            if (product.mainImage != null && !product.mainImage.isEmpty()) {
                Picasso.get()
                        .load(product.mainImage)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(imgProduct);
            }
        }
    }
}