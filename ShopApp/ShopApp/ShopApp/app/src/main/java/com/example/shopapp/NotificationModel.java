package com.example.shopapp;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NotificationModel {

    private String notificationId;
    private String userId;              // ID của user nhận thông báo
    private String title;                // Tiêu đề thông báo
    private String body;                 // Nội dung thông báo
    private String imageUrl;             // URL ảnh (nếu có)
    private String type;                 // ORDER, PROMOTION, SYSTEM, DELIVERY, REVIEW, etc
    private String actionType;           // OPEN_SCREEN, OPEN_URL, DEEP_LINK, NONE
    private String actionData;           // Data cho action (orderId, productId, url, etc)
    private boolean isRead;              // Đã đọc chưa
    private int priority;                // 0: LOW, 1: NORMAL, 2: HIGH

    @ServerTimestamp
    private Date timestamp;              // Thời gian tạo (auto by Firestore)

    private String senderName;           // Tên người/hệ thống gửi
    private String icon;                 // Icon type: bell, cart, star, truck, gift
    private Map<String, Object> extraData; // Dữ liệu bổ sung dạng JSON

    // Constructor không tham số (bắt buộc cho Firestore)
    public NotificationModel() {
        this.extraData = new HashMap<>();
        this.isRead = false;
        this.priority = 1; // NORMAL
        this.icon = "bell";
        this.actionType = "NONE";
    }

    // Constructor đầy đủ
    public NotificationModel(String notificationId, String userId, String title, String body,
                             String type, String actionType, String actionData) {
        this();
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.actionType = actionType;
        this.actionData = actionData;
    }

    // Getters and Setters
    @PropertyName("notificationId")
    public String getNotificationId() {
        return notificationId;
    }

    @PropertyName("notificationId")
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("title")
    public String getTitle() {
        return title;
    }

    @PropertyName("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @PropertyName("body")
    public String getBody() {
        return body;
    }

    @PropertyName("body")
    public void setBody(String body) {
        this.body = body;
    }

    @PropertyName("imageUrl")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("type")
    public String getType() {
        return type;
    }

    @PropertyName("type")
    public void setType(String type) {
        this.type = type;
    }

    @PropertyName("actionType")
    public String getActionType() {
        return actionType;
    }

    @PropertyName("actionType")
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    @PropertyName("actionData")
    public String getActionData() {
        return actionData;
    }

    @PropertyName("actionData")
    public void setActionData(String actionData) {
        this.actionData = actionData;
    }

    @PropertyName("isRead")
    public boolean isRead() {
        return isRead;
    }

    @PropertyName("isRead")
    public void setRead(boolean read) {
        isRead = read;
    }

    @PropertyName("priority")
    public int getPriority() {
        return priority;
    }

    @PropertyName("priority")
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @PropertyName("timestamp")
    public Date getTimestamp() {
        return timestamp;
    }

    @PropertyName("timestamp")
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @PropertyName("senderName")
    public String getSenderName() {
        return senderName;
    }

    @PropertyName("senderName")
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @PropertyName("icon")
    public String getIcon() {
        return icon;
    }

    @PropertyName("icon")
    public void setIcon(String icon) {
        this.icon = icon;
    }

    @PropertyName("extraData")
    public Map<String, Object> getExtraData() {
        return extraData;
    }

    @PropertyName("extraData")
    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }

    // Helper method để thêm extra data
    public void addExtraData(String key, Object value) {
        if (this.extraData == null) {
            this.extraData = new HashMap<>();
        }
        this.extraData.put(key, value);
    }

    // Helper method để lấy thời gian hiển thị
    public String getTimeAgo() {
        if (timestamp == null) return "Vừa xong";

        long diff = System.currentTimeMillis() - timestamp.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " ngày trước";
        } else if (hours > 0) {
            return hours + " giờ trước";
        } else if (minutes > 0) {
            return minutes + " phút trước";
        } else {
            return "Vừa xong";
        }
    }

    // Helper method để lấy icon resource
    public int getIconResource() {
        if (icon == null) return R.drawable.ic_notification;

        switch (icon.toLowerCase()) {
            case "cart":
                return R.drawable.ic_shopping_bag;
            case "star":
                return R.drawable.ic_favorite;
            case "truck":
            case "delivery":
                return R.drawable.ic_notification; // Có thể thay bằng truck icon
            case "gift":
            case "promotion":
                return R.drawable.ic_notification; // Có thể thay bằng gift icon
            default:
                return R.drawable.ic_notification;
        }
    }
}