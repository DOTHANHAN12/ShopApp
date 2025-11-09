package com.example.shopapp;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviewList;

    public ReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
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

    static class ReviewViewHolder extends RecyclerView.ViewHolder {

        private RatingBar ratingBar;
        private TextView textUserName;
        private TextView textReviewComment;
        private TextView textReviewTimestamp;
        private RecyclerView recyclerReviewImages;
        private ReviewImageAdapter imageAdapter;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ratingBar = itemView.findViewById(R.id.rating_bar_review);
            textUserName = itemView.findViewById(R.id.text_user_name);
            textReviewComment = itemView.findViewById(R.id.text_review_comment);
            textReviewTimestamp = itemView.findViewById(R.id.text_review_timestamp);
            recyclerReviewImages = itemView.findViewById(R.id.recycler_review_images);
            
            // Setup image recycler view
            recyclerReviewImages.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            imageAdapter = new ReviewImageAdapter();
            recyclerReviewImages.setAdapter(imageAdapter);
        }

        public void bind(Review review) {
            ratingBar.setRating(review.getRating());
            textUserName.setText(review.getUserName());
            textReviewComment.setText(review.getComment());
            textReviewTimestamp.setText(formatTimestamp(review.getTimestamp()));
            
            // Handle images
            if (review.getImages() != null && !review.getImages().isEmpty()) {
                recyclerReviewImages.setVisibility(View.VISIBLE);
                imageAdapter.setImages(review.getImages());
            } else {
                recyclerReviewImages.setVisibility(View.GONE);
            }
        }

        private String formatTimestamp(long timestamp) {
            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(date);
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
