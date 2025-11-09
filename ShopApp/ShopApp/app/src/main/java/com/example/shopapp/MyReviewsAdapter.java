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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        private final TextView textReviewStatus;
        private final View statusIndicator;
        private final RecyclerView recyclerReviewImages;
        private ReviewImageAdapter imageAdapter;

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
            textReviewStatus = itemView.findViewById(R.id.text_review_status);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            recyclerReviewImages = itemView.findViewById(R.id.recycler_review_images);
            
            // Setup image recycler view
            recyclerReviewImages.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            imageAdapter = new ReviewImageAdapter();
            recyclerReviewImages.setAdapter(imageAdapter);
        }

        public void bind(Review review) {
            Context context = itemView.getContext();

            // Set status
            String status = review.getStatus() != null ? review.getStatus() : Review.Status.PENDING.name();
            textReviewStatus.setText("Trạng thái: " + getStatusLabel(status));
            int statusColor = getStatusColor(context, status);
            textReviewStatus.setTextColor(statusColor);
            if (statusIndicator != null) {
                statusIndicator.getBackground().setTint(statusColor);
            }

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
            
            // Handle images
            if (review.getImages() != null && !review.getImages().isEmpty()) {
                recyclerReviewImages.setVisibility(View.VISIBLE);
                imageAdapter.setImages(review.getImages());
            } else {
                recyclerReviewImages.setVisibility(View.GONE);
            }

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

        private String getStatusLabel(String status) {
            if (status == null) return "Chờ duyệt";
            switch (status) {
                case "APPROVED":
                    return "Đã phê duyệt";
                case "REJECTED":
                    return "Bị từ chối";
                case "PENDING":
                default:
                    return "Chờ duyệt";
            }
        }

        private int getStatusColor(Context context, String status) {
            if (status == null) return ContextCompat.getColor(context, R.color.color_status_pending);
            switch (status) {
                case "APPROVED":
                    return ContextCompat.getColor(context, R.color.color_status_completed);
                case "REJECTED":
                    return ContextCompat.getColor(context, R.color.color_status_rejected);
                case "PENDING":
                default:
                    return ContextCompat.getColor(context, R.color.color_status_pending);
            }
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
    
    // Inner adapter for review images
    static class ReviewImageAdapter extends RecyclerView.Adapter<ReviewImageAdapter.ImageViewHolder> {
        private List<String> imageUrls = new ArrayList<>();
        
        public void setImages(List<String> images) {
            this.imageUrls = images != null ? images : new ArrayList<>();
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_review_image, parent, false);
            return new ImageViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            String imageUrl = imageUrls.get(position);
            if (!TextUtils.isEmpty(imageUrl)) {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_broken_image)
                        .into(holder.imageView);
            }
        }
        
        @Override
        public int getItemCount() {
            return imageUrls.size();
        }
        
        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            
            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.img_review_image);
            }
        }
    }
}
