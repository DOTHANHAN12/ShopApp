package com.example.shopapp;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MyReviewsAdapter extends RecyclerView.Adapter<MyReviewsAdapter.ReviewViewHolder> {

    private static final long EDIT_TIME_LIMIT_HOURS = 12;
    private final List<Review> reviewList;
    private final OnReviewActionListener listener;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnReviewActionListener {
        void onEditReview(Review review);
        void onViewProduct(Review review);
        void onViewDetails(Review review);
        void onDeleteReview(Review review);
    }

    public MyReviewsAdapter(List<Review> reviewList, OnReviewActionListener listener) {
        this.reviewList = reviewList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgProduct;
        private final ImageView btnViewProduct;
        private final TextView textProductName;
        private final TextView textOrderId;
        private final RatingBar ratingBar;
        private final TextView textRatingValue;
        private final TextView textTimestamp;
        private final TextView textComment;
        private final TextView btnExpand;
        private final TextView badgeEdited;
        private final LinearLayout btnEditReview;
        private final LinearLayout btnViewDetails;
        private final LinearLayout btnDeleteReview;
        private final LinearLayout layoutEditTime;
        private final TextView textEditTimeRemaining;

        private boolean isExpanded = false;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.img_product);
            btnViewProduct = itemView.findViewById(R.id.btn_view_product);
            textProductName = itemView.findViewById(R.id.text_product_name);
            textOrderId = itemView.findViewById(R.id.text_order_id);
            ratingBar = itemView.findViewById(R.id.rating_bar_review);
            textRatingValue = itemView.findViewById(R.id.text_rating_value);
            textTimestamp = itemView.findViewById(R.id.text_review_timestamp);
            textComment = itemView.findViewById(R.id.text_review_comment);
            btnExpand = itemView.findViewById(R.id.btn_expand);
            badgeEdited = itemView.findViewById(R.id.badge_edited);
            btnEditReview = itemView.findViewById(R.id.btn_edit_review);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
            btnDeleteReview = itemView.findViewById(R.id.btn_delete_review);
            layoutEditTime = itemView.findViewById(R.id.layout_edit_time);
            textEditTimeRemaining = itemView.findViewById(R.id.text_edit_time_remaining);
        }

        public void bind(Review review) {
            Context context = itemView.getContext();

            // Set rating
            ratingBar.setRating(review.getRating());
            textRatingValue.setText(String.format(Locale.getDefault(), "%.1f", review.getRating()));

            // Set timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            textTimestamp.setText(sdf.format(new Date(review.getTimestamp())));

            // Set order ID
            textOrderId.setText(String.format("Đơn: #%s",
                    review.getOrderId() != null ? review.getOrderId().substring(0, Math.min(8, review.getOrderId().length())) : "N/A"));

            // Set comment
            textComment.setText(review.getComment());

            // Setup expand/collapse
            textComment.post(() -> {
                int lineCount = textComment.getLineCount();
                if (lineCount > 3) {
                    btnExpand.setVisibility(View.VISIBLE);
                    textComment.setMaxLines(isExpanded ? Integer.MAX_VALUE : 3);
                    btnExpand.setText(isExpanded ? "Thu gọn" : "Xem thêm");
                } else {
                    btnExpand.setVisibility(View.GONE);
                    textComment.setMaxLines(Integer.MAX_VALUE);
                }
            });

            btnExpand.setOnClickListener(v -> {
                isExpanded = !isExpanded;
                textComment.setMaxLines(isExpanded ? Integer.MAX_VALUE : 3);
                btnExpand.setText(isExpanded ? "Thu gọn" : "Xem thêm");
            });

            // Show edited badge
            if (review.isEdited()) {
                badgeEdited.setVisibility(View.VISIBLE);
            } else {
                badgeEdited.setVisibility(View.GONE);
            }

            // Load product info
            loadProductInfo(review.getProductId());

            // Setup edit time remaining
            setupEditTimeInfo(review);

            // Setup action buttons
            btnViewProduct.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onViewProduct(reviewList.get(pos));
                }
            });

            btnEditReview.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onEditReview(reviewList.get(pos));
                }
            });

            btnViewDetails.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onViewDetails(reviewList.get(pos));
                }
            });

            btnDeleteReview.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onDeleteReview(reviewList.get(pos));
                }
            });
        }

        private void loadProductInfo(String productId) {
            if (productId == null) return;

            db.collection("products").document(productId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Product product = documentSnapshot.toObject(Product.class);
                            if (product != null) {
                                textProductName.setText(product.getName());

                                // Load product image
                                if (!TextUtils.isEmpty(product.getMainImage())) {
                                    Picasso.get()
                                            .load(product.getMainImage())
                                            .placeholder(R.drawable.ic_placeholder)
                                            .error(R.drawable.ic_broken_image)
                                            .into(imgProduct);
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        textProductName.setText("Không tải được thông tin sản phẩm");
                    });
        }

        private void setupEditTimeInfo(Review review) {
            if (review.isEdited()) {
                // Already edited - cannot edit again
                layoutEditTime.setVisibility(View.GONE);
                return;
            }

            long currentTime = System.currentTimeMillis();
            long reviewTime = review.getTimestamp();
            long hoursDifference = TimeUnit.MILLISECONDS.toHours(currentTime - reviewTime);

            if (hoursDifference >= EDIT_TIME_LIMIT_HOURS) {
                // Time expired
                layoutEditTime.setVisibility(View.GONE);
            } else {
                // Show remaining time
                layoutEditTime.setVisibility(View.VISIBLE);
                long hoursRemaining = EDIT_TIME_LIMIT_HOURS - hoursDifference;

                if (hoursRemaining > 1) {
                    textEditTimeRemaining.setText(String.format(Locale.getDefault(),
                            "Còn %d giờ để chỉnh sửa", hoursRemaining));
                } else {
                    long minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(
                            (EDIT_TIME_LIMIT_HOURS * 60 * 60 * 1000) - (currentTime - reviewTime)
                    );
                    textEditTimeRemaining.setText(String.format(Locale.getDefault(),
                            "Còn %d phút để chỉnh sửa", minutesRemaining));
                }
            }
        }
    }
}