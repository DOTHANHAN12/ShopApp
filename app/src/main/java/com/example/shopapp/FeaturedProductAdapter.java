package com.example.shopapp;

import android.content.Intent;
import android.graphics.Paint; // Cần import Paint
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

        // THIẾT LẬP LISTENER CLICK VÀ TRUYỀN ID
        holder.itemView.setOnClickListener(v -> {
            // Chuyển sang ProductDetailActivity
            Intent intent = new Intent(v.getContext(), ProductDetailActivity.class);
            // Truyền Product ID (Document ID)
            intent.putExtra("PRODUCT_ID", product.getProductId());
            v.getContext().startActivity(intent);
        });
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
            textTitle.setText(product.getName());

            // Sử dụng trường 'desc' cho mô tả
            textDescription.setText(product.getDesc());

            // -----------------------------------------------------------
            // LOGIC TÍNH TOÁN VÀ GÁN GIÁ MỚI
            // -----------------------------------------------------------

            double basePrice = product.getBasePrice();
            double displayPrice = basePrice;
            boolean hasValidOffer = false;

            if (product.getIsOfferStatus() && product.getOffer() != null) {
                long now = System.currentTimeMillis();
                OfferDetails offer = product.getOffer();

                // Kiểm tra thời gian khuyến mãi còn hiệu lực
                if (now >= offer.getStartDate() && now <= offer.getEndDate()) {
                    hasValidOffer = true;
                    double discount = offer.getDiscountPercent() / 100.0;
                    displayPrice = basePrice * (1.0 - discount);

                    // Hiển thị chi tiết khuyến mãi
                    textOfferDetails.setText(String.format(Locale.getDefault(), "%d%% OFF, Hết hạn %d ngày",
                            offer.getDiscountPercent(),
                            (offer.getEndDate() - now) / (1000 * 60 * 60 * 24) // Tính số ngày còn lại
                    ));
                    textDisclaimer.setText("Sản phẩm số lượng có hạn.");
                }
            } else {
                textOfferDetails.setText("Mua ngay giá tốt!");
                textDisclaimer.setText("");
            }

            // GÁN GIÁ HIỆN TẠI
            String currentPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", displayPrice);
            textCurrentPrice.setText(currentPriceFormatted);

            // GÁN GIÁ GỐC VÀ GẠCH NGANG
            if (hasValidOffer) {
                String originalPriceFormatted = String.format(Locale.getDefault(), "%,.0f VND", basePrice);
                textOriginalPrice.setText(originalPriceFormatted);
                // Thêm hiệu ứng gạch ngang
                textOriginalPrice.setPaintFlags(
                        textOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            } else {
                // Xóa giá gốc nếu không có khuyến mãi hợp lệ
                textOriginalPrice.setText("");
                textOriginalPrice.setPaintFlags(0);
            }
            // -----------------------------------------------------------


            // Cập nhật Badge "LIMITED OFFER"
            if (hasValidOffer) {
                textLimitedOfferBadge.setVisibility(View.VISIBLE);
            } else {
                textLimitedOfferBadge.setVisibility(View.GONE);
            }

            // Tải Ảnh bằng Picasso (Sử dụng trường mainImage mới)
            if (product.getMainImage() != null && !product.getMainImage().isEmpty()) {
                Picasso.get()
                        .load(product.getMainImage())
                        .error(R.drawable.ic_launcher_foreground)
                        .into(imgProduct);
            }
        }
    }
}