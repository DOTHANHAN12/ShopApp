package com.example.shopapp;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationModel> notificationList;
    private OnNotificationClickListener clickListener;
    private OnNotificationDeleteListener deleteListener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationModel notification);
    }

    public interface OnNotificationDeleteListener {
        void onNotificationDelete(NotificationModel notification);
    }

    public NotificationAdapter(List<NotificationModel> notificationList,
                               OnNotificationClickListener clickListener,
                               OnNotificationDeleteListener deleteListener) {
        this.notificationList = notificationList;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = notificationList.get(position);

        // Set title and body
        holder.tvTitle.setText(notification.getTitle());
        holder.tvBody.setText(notification.getBody());
        holder.tvTime.setText(notification.getTimeAgo());

        // Set icon
        holder.ivIcon.setImageResource(notification.getIconResource());

        // Load image if available
        if (notification.getImageUrl() != null && !notification.getImageUrl().isEmpty()) {
            holder.ivImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(notification.getImageUrl())
                    .centerCrop()
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }

        // Style based on read status
        if (notification.isRead()) {
            holder.cardView.setCardBackgroundColor(
                    holder.itemView.getContext().getResources().getColor(android.R.color.white)
            );
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.viewUnreadIndicator.setVisibility(View.GONE);
        } else {
            holder.cardView.setCardBackgroundColor(
                    holder.itemView.getContext().getResources().getColor(R.color.notification_unread_bg)
            );
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.viewUnreadIndicator.setVisibility(View.VISIBLE);
        }

        // Click listeners
        holder.cardView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onNotificationClick(notification);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onNotificationDelete(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivIcon;
        ImageView ivImage;
        TextView tvTitle;
        TextView tvBody;
        TextView tvTime;
        ImageView btnDelete;
        View viewUnreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_notification);
            ivIcon = itemView.findViewById(R.id.iv_notification_icon);
            ivImage = itemView.findViewById(R.id.iv_notification_image);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvBody = itemView.findViewById(R.id.tv_notification_body);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
            btnDelete = itemView.findViewById(R.id.btn_delete_notification);
            viewUnreadIndicator = itemView.findViewById(R.id.view_unread_indicator);
        }
    }
}
