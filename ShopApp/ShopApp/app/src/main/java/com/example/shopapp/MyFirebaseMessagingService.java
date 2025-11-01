package com.example.shopapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = "";
        String body = "";
        String imageUrl = "";
        String type = "SYSTEM";
        int priority = 1;

        // Lấy từ notification payload
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle() != null ?
                    remoteMessage.getNotification().getTitle() : "";
            body = remoteMessage.getNotification().getBody() != null ?
                    remoteMessage.getNotification().getBody() : "";

            if (remoteMessage.getNotification().getImageUrl() != null) {
                imageUrl = remoteMessage.getNotification().getImageUrl().toString();
            }

            Log.d(TAG, "Notification Title: " + title);
            Log.d(TAG, "Notification Body: " + body);
        }

        // Lấy từ data payload (quan trọng hơn)
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();

            if (data.containsKey("title") && data.get("title") != null)
                title = data.get("title");
            if (data.containsKey("body") && data.get("body") != null)
                body = data.get("body");
            if (data.containsKey("imageUrl") && data.get("imageUrl") != null)
                imageUrl = data.get("imageUrl");
            if (data.containsKey("type") && data.get("type") != null)
                type = data.get("type");
            if (data.containsKey("priority") && data.get("priority") != null) {
                try {
                    priority = Integer.parseInt(data.get("priority"));
                } catch (NumberFormatException e) {
                    priority = 1;
                }
            }
            Log.d(TAG, "Data payload: " + data.toString());
        }

        // ⭐️ FIX: Kiểm tra xem user hiện tại có được phép nhận thông báo này không
        if (!shouldShowNotification(remoteMessage.getData(), type)) {
            return; // Nếu không phải người nhận, không làm gì cả
        }

        // ✅ CHỈ hiển thị thông báo đẩy. Không lưu vào DB nữa vì backend đã làm.
        sendNotification(title, body, imageUrl, priority);
    }

    /**
     * Kiểm tra xem người dùng hiện tại có được phép nhận thông báo này không.
     */
    private boolean shouldShowNotification(Map<String, String> data, String type) {
        // Thông báo chung (SYSTEM) luôn hiển thị
        if (!"ORDER".equals(type)) {
            Log.d(TAG, "Notification is not type ORDER, showing to user.");
            return true;
        }

        // Thông báo đơn hàng (ORDER) cần kiểm tra userId
        String payloadUserId = data.get("userId");
        if (payloadUserId == null || payloadUserId.isEmpty()) {
            Log.w(TAG, "ORDER notification is missing userId in data payload. Not showing.");
            return false; // Không có userId, không hiển thị
        }

        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "No user is logged in. Not showing notification.");
            return false; // Người dùng chưa đăng nhập
        }

        String currentUserId = mAuth.getCurrentUser().getUid();

        // So sánh userId trong thông báo với người dùng hiện tại
        boolean isOwner = payloadUserId.equals(currentUserId);
        if (isOwner) {
            Log.d(TAG, "User is the owner of the order. Showing notification.");
        } else {
            Log.w(TAG, "User (" + currentUserId + ") is not the owner of the order (" + payloadUserId + "). Not showing notification.");
        }

        return isOwner;
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", token);
            updates.put("tokenUpdatedAt", System.currentTimeMillis());

            db.collection("users")
                    .document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated in Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating FCM token", e));
        }
    }

    private void sendNotification(String messageTitle, String messageBody, String imageUrl, int priority) {
        if (messageTitle == null || messageBody == null || messageTitle.isEmpty() || messageBody.isEmpty()) {
            Log.d(TAG, "Cannot send notification, title or body is empty");
            return;
        }

        Intent intent = new Intent(this, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "fcm_default_channel";

        int notifPriority = priority >= 2 ? NotificationCompat.PRIORITY_HIGH :
                priority == 1 ? NotificationCompat.PRIORITY_DEFAULT :
                        NotificationCompat.PRIORITY_LOW;

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setPriority(notifPriority)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Tạo channel nếu cần (cho Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = priority >= 2 ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, "Thông báo chung", importance);
            notificationManager.createNotificationChannel(channel);
        }

        // Xử lý tải ảnh nền
        if (imageUrl != null && !imageUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    Bitmap bitmap = getBitmapFromURL(imageUrl);
                    if (bitmap != null) {
                        notificationBuilder.setLargeIcon(bitmap);
                        notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .bigLargeIcon((Bitmap) null));
                    }
                    notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image for notification", e);
                    notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
                }
            }).start();
        } else {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        }
    }

    private Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error downloading image: " + strURL, e);
            return null;
        }
    }
}
