package com.example.shopapp;

import android.content.Intent;
import android.graphics.Paint; // Cần import Paint
import android.util.Log;
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
import java.util.concurrent.TimeUnit;

public class FeaturedProductAdapter extends RecyclerView.Adapter<FeaturedProductAdapter.FeaturedProductViewHolder> {

    @FunctionalInterface
    public interface OfferValidator {
        boolean isOfferValid(OfferDetails offer);
    }

    private final List<Product> featuredProducts;
    private final OfferValidator offerValidator;

    public FeaturedProductAdapter(List<Product> featuredProducts, OfferValidator offerValidator) {
        this.featuredProducts = featuredProducts;
        this.offerValidator = offerValidator;
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
        holder.bind(product, offerValidator);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getProductId());
            v.getContext().startActivity(intent);
        });
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

        public void bind(Product product, OfferValidator offerValidator) {
            textTitle.setText(product.getName());
            textDescription.setText(product.getDesc());

            double basePrice = product.getBasePrice();
            double displayPrice = basePrice;
            boolean hasValidOffer = false;

            if (product.getIsOfferStatus() && offerValidator.isOfferValid(product.getOffer())) {
                hasValidOffer = true;
                OfferDetails offer = product.getOffer();
                if (offer != null && offer.getOfferValue() != null) {
                    double discount = offer.getOfferValue() / 100.0;
                    displayPrice = basePrice * (1.0 - discount);

                    // ✅ SỬA LỖI: Tính toán ngày hết hạn từ timestamp
                    if (offer.getEndDate() != null) {
                        long diffInMillis = offer.getEndDate() - System.currentTimeMillis();
                        if (diffInMillis > 0) {
                            long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
                            textOfferDetails.setText(String.format(Locale.getDefault(), "%d%% OFF, Còn %d ngày",
                                    offer.getOfferValue(),
                                    diffInDays + 1)); // +1 để bao gồm cả ngày hiện tại
                        } else {
                            textOfferDetails.setText(String.format(Locale.getDefault(), "%d%% OFF", offer.getOfferValue()));
                        }
                    } else {
                        textOfferDetails.setText(String.format(Locale.getDefault(), "%d%% OFF", offer.getOfferValue()));
                    }
                } else {
                     textOfferDetails.setText("Ưu đãi đặc biệt!");
                }
                textDisclaimer.setText("Sản phẩm số lượng có hạn.");
            } else {
                textOfferDetails.setText("Mua ngay giá tốt!");
                textDisclaimer.setText("");
            }

            String currentPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", displayPrice);
            textCurrentPrice.setText(currentPriceFormatted);

            if (hasValidOffer) {
                String originalPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", basePrice);
                textOriginalPrice.setText(originalPriceFormatted);
                textOriginalPrice.setPaintFlags(
                        textOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            } else {
                textOriginalPrice.setText("");
                textOriginalPrice.setPaintFlags(0);
            }

            if (hasValidOffer) {
                textLimitedOfferBadge.setVisibility(View.VISIBLE);
            } else {
                textLimitedOfferBadge.setVisibility(View.GONE);
            }

            if (product.getMainImage() != null && !product.getMainImage().isEmpty()) {
                Picasso.get()
                        .load(product.getMainImage())
                        .error(R.drawable.ic_launcher_foreground)
                        .into(imgProduct);
            }
        }
    }
}
