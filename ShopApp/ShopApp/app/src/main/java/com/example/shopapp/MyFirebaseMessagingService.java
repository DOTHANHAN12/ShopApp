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
        String actionType = "NONE";
        String actionData = "";
        String icon = "bell";
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

        // Lấy từ data payload (quan trọng cho app xử lý)
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
            if (data.containsKey("actionType") && data.get("actionType") != null)
                actionType = data.get("actionType");
            if (data.containsKey("actionData") && data.get("actionData") != null)
                actionData = data.get("actionData");
            if (data.containsKey("icon") && data.get("icon") != null)
                icon = data.get("icon");
            if (data.containsKey("priority") && data.get("priority") != null) {
                try {
                    priority = Integer.parseInt(data.get("priority"));
                } catch (NumberFormatException e) {
                    priority = 1;
                }
            }

            Log.d(TAG, "Data payload: " + data.toString());
            Log.d(TAG, "Image URL: " + imageUrl);
        }

        // Lưu vào Firestore
        saveNotificationToFirestore(title, body, imageUrl, type, actionType, actionData, icon, priority, remoteMessage.getData());

        // Hiển thị system notification với hình ảnh
        sendNotification(title, body, imageUrl, priority);
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
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved to Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving FCM token", e));
        }
    }

    private void saveNotificationToFirestore(String title, String body, String imageUrl,
                                             String type, String actionType, String actionData,
                                             String icon, int priority, Map<String, String> extraData) {
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "User not logged in, notification not saved");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String notificationId = db.collection("notifications").document().getId();

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("notificationId", notificationId);
        notificationData.put("userId", userId);
        notificationData.put("title", title != null ? title : "");
        notificationData.put("body", body != null ? body : "");
        notificationData.put("imageUrl", imageUrl != null && !imageUrl.isEmpty() ? imageUrl : "");
        notificationData.put("type", type != null ? type : "SYSTEM");
        notificationData.put("actionType", actionType != null ? actionType : "NONE");
        notificationData.put("actionData", actionData != null && !actionData.isEmpty() ? actionData : "");
        notificationData.put("icon", icon != null ? icon : "bell");
        notificationData.put("priority", priority);
        notificationData.put("isRead", false);
        notificationData.put("timestamp", FieldValue.serverTimestamp());
        notificationData.put("senderName", "Hệ thống");

        Map<String, Object> extra = new HashMap<>();
        if (extraData != null && !extraData.isEmpty()) {
            for (Map.Entry<String, String> entry : extraData.entrySet()) {
                if (entry.getValue() != null) {
                    extra.put(entry.getKey(), entry.getValue());
                }
            }
        }
        notificationData.put("extraData", extra);

        db.collection("notifications")
                .document(notificationId)
                .set(notificationData)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Notification saved to Firestore: " + notificationId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error saving notification to Firestore", e));
    }

    private void sendNotification(String messageTitle, String messageBody, String imageUrl, int priority) {
        if (messageTitle == null || messageBody == null) {
            Log.d(TAG, "Cannot send notification, title or body is null");
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

        // Nếu có imageUrl, download và hiển thị với BigPictureStyle
        if (imageUrl != null && !imageUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    Bitmap bitmap = getBitmapFromURL(imageUrl);
                    if (bitmap != null) {
                        notificationBuilder.setLargeIcon(bitmap);
                        notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .bigLargeIcon((Bitmap) null) // Hide large icon when expanded
                                .setSummaryText(messageBody));

                        showNotification(notificationBuilder.build(), channelId, priority);
                    } else {
                        // Nếu không download được ảnh, hiện notification không có ảnh
                        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody));
                        showNotification(notificationBuilder.build(), channelId, priority);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image for notification", e);
                    // Fallback: Hiện notification không có ảnh
                    notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody));
                    showNotification(notificationBuilder.build(), channelId, priority);
                }
            }).start();
        } else {
            // Không có ảnh, dùng BigTextStyle
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody));
            showNotification(notificationBuilder.build(), channelId, priority);
        }
    }

    private void showNotification(android.app.Notification notification, String channelId, int priority) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = priority >= 2 ? NotificationManager.IMPORTANCE_HIGH :
                    NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(channelId,
                    "Thông báo chung",
                    importance);
            channel.setDescription("Kênh thông báo cho các thông báo quan trọng");
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }

    /**
     * Download bitmap từ URL
     */
    private Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(10000); // 10 seconds timeout
            connection.setReadTimeout(10000);
            connection.connect();

            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            connection.disconnect();

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error downloading image: " + strURL, e);
            return null;
        }
    }
}