package com.example.shopapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity {

    private static final String TAG = "OrderDetailActivity";

    private FirebaseFirestore db;
    private String orderId;
    private ListenerRegistration orderListener;

    // UI Components
    private ImageView btnBack;
    private TextView tvOrderId;
    private TextView tvOrderDate;
    private TextView tvOrderStatus;
    private CardView cardOrderStatus;
    private LinearLayout layoutStatusTimeline;

    private TextView tvShippingName;
    private TextView tvShippingPhone;
    private TextView tvShippingAddress;

    private LinearLayout layoutOrderItems;

    private TextView tvSubtotal;
    private TextView tvDiscount;
    private TextView tvTotal;
    private TextView tvPaymentMethod;

    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup UI
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_order_detail);

        db = FirebaseFirestore.getInstance();

        // Get orderId from intent
        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRealtimeListener();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back_order_detail);
        tvOrderId = findViewById(R.id.tv_order_id);
        tvOrderDate = findViewById(R.id.tv_order_date);
        tvOrderStatus = findViewById(R.id.tv_order_status);
        cardOrderStatus = findViewById(R.id.card_order_status);
        layoutStatusTimeline = findViewById(R.id.layout_status_timeline);

        tvShippingName = findViewById(R.id.tv_shipping_name);
        tvShippingPhone = findViewById(R.id.tv_shipping_phone);
        tvShippingAddress = findViewById(R.id.tv_shipping_address);

        layoutOrderItems = findViewById(R.id.layout_order_items);

        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDiscount = findViewById(R.id.tv_discount);
        tvTotal = findViewById(R.id.tv_total);
        tvPaymentMethod = findViewById(R.id.tv_payment_method);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRealtimeListener() {
        // Real-time listener để update khi status thay đổi
        orderListener = db.collection("orders").document(orderId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to order", error);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        currentOrder = documentSnapshot.toObject(Order.class);
                        if (currentOrder != null) {
                            currentOrder.setOrderId(documentSnapshot.getId());
                            displayOrderDetails(currentOrder);
                        }
                    } else {
                        Toast.makeText(this, "Đơn hàng không tồn tại", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void displayOrderDetails(Order order) {
        // Order Info
        tvOrderId.setText("Đơn hàng #" + orderId.substring(0, Math.min(8, orderId.length())).toUpperCase());

        if (order.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvOrderDate.setText(sdf.format(new Date(order.getCreatedAt())));
        }

        // Order Status with animation
        updateOrderStatus(order.getOrderStatus());

        // Status Timeline
        updateStatusTimeline(order.getOrderStatus());

        // Shipping Address
        Map<String, String> address = order.getShippingAddress();
        if (address != null) {
            tvShippingName.setText(address.get("fullName"));
            tvShippingPhone.setText(address.get("phoneNumber"));
            tvShippingAddress.setText(address.get("fullLocation") + ", " + address.get("streetAddress"));
        }

        // Order Items
        displayOrderItems(order.getItems());

        // Payment Summary
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvSubtotal.setText(currencyFormat.format(order.getSubtotal()));
        tvDiscount.setText("-" + currencyFormat.format(order.getDiscountAmount()));
        tvTotal.setText(currencyFormat.format(order.getTotalAmount()));
        tvPaymentMethod.setText(getPaymentMethodText(order.getPaymentMethod()));
    }

    private void updateOrderStatus(String status) {
        if (status == null) status = "PENDING";

        tvOrderStatus.setText(getStatusText(status));

        // Set color based on status
        int color = getStatusColor(status);
        cardOrderStatus.setCardBackgroundColor(color);
    }

    private void updateStatusTimeline(String currentStatus) {
        layoutStatusTimeline.removeAllViews();

        String[] statuses = {"PENDING", "CONFIRMED", "PROCESSING", "SHIPPING", "DELIVERED"};
        String[] statusTexts = {"Chờ xác nhận", "Đã xác nhận", "Đang xử lý", "Đang giao", "Đã giao"};

        for (int i = 0; i < statuses.length; i++) {
            View timelineItem = getLayoutInflater().inflate(R.layout.item_status_timeline, layoutStatusTimeline, false);

            View statusDot = timelineItem.findViewById(R.id.status_dot);
            View statusLine = timelineItem.findViewById(R.id.status_line);
            TextView tvStatusName = timelineItem.findViewById(R.id.tv_status_name);

            tvStatusName.setText(statusTexts[i]);

            int statusIndex = getStatusIndex(currentStatus);
            int currentIndex = i;

            if (currentIndex <= statusIndex) {
                // Completed status
                statusDot.setBackgroundResource(R.drawable.circle_status_completed);
                tvStatusName.setTextColor(Color.parseColor("#4CAF50"));
                if (i < statuses.length - 1) {
                    statusLine.setBackgroundColor(Color.parseColor("#4CAF50"));
                }
            } else {
                // Pending status
                statusDot.setBackgroundResource(R.drawable.circle_status_pending);
                tvStatusName.setTextColor(Color.parseColor("#CCCCCC"));
                if (i < statuses.length - 1) {
                    statusLine.setBackgroundColor(Color.parseColor("#EEEEEE"));
                }
            }

            // Hide line for last item
            if (i == statuses.length - 1) {
                statusLine.setVisibility(View.GONE);
            }

            layoutStatusTimeline.addView(timelineItem);
        }
    }

    private int getStatusIndex(String status) {
        switch (status) {
            case "PENDING": return 0;
            case "CONFIRMED": return 1;
            case "PROCESSING": return 2;
            case "SHIPPING": return 3;
            case "DELIVERED": return 4;
            case "COMPLETED": return 4;
            default: return 0;
        }
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "⏳ Chờ xác nhận";
            case "CONFIRMED": return "✅ Đã xác nhận";
            case "PROCESSING": return "📦 Đang xử lý";
            case "SHIPPING": return "🚚 Đang giao hàng";
            case "DELIVERED": return "✓ Đã giao hàng";
            case "COMPLETED": return "✓ Hoàn thành";
            case "CANCELLED": return "❌ Đã hủy";
            case "REFUNDED": return "💰 Đã hoàn tiền";
            case "PAID": return "💳 Đã thanh toán";
            case "FAILED_PAYMENT": return "❌ Thanh toán thất bại";
            default: return status;
        }
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "PENDING": return Color.parseColor("#FF9800");
            case "CONFIRMED": return Color.parseColor("#2196F3");
            case "PROCESSING": return Color.parseColor("#9C27B0");
            case "SHIPPING": return Color.parseColor("#00BCD4");
            case "DELIVERED": return Color.parseColor("#4CAF50");
            case "COMPLETED": return Color.parseColor("#4CAF50");
            case "PAID": return Color.parseColor("#4CAF50");
            case "CANCELLED": return Color.parseColor("#F44336");
            case "REFUNDED": return Color.parseColor("#607D8B");
            case "FAILED_PAYMENT": return Color.parseColor("#F44336");
            default: return Color.parseColor("#9E9E9E");
        }
    }

    private String getPaymentMethodText(String method) {
        if (method == null) return "Không xác định";
        switch (method) {
            case "COD": return "💵 Thanh toán khi nhận hàng (COD)";
            case "VNPAY": return "💳 VNPay";
            case "CREDIT_CARD": return "💳 Thẻ tín dụng";
            case "BANK_TRANSFER": return "🏦 Chuyển khoản ngân hàng";
            default: return method;
        }
    }

    private void displayOrderItems(List<Map<String, Object>> items) {
        layoutOrderItems.removeAllViews();

        if (items == null || items.isEmpty()) {
            return;
        }

        for (Map<String, Object> item : items) {
            View itemView = getLayoutInflater().inflate(R.layout.item_order_product, layoutOrderItems, false);

            TextView tvProductName = itemView.findViewById(R.id.tv_product_name);
            TextView tvProductVariant = itemView.findViewById(R.id.tv_product_variant);
            TextView tvProductQuantity = itemView.findViewById(R.id.tv_product_quantity);
            TextView tvProductPrice = itemView.findViewById(R.id.tv_product_price);

            tvProductName.setText((String) item.get("name"));
            tvProductVariant.setText((String) item.get("variant"));
            tvProductQuantity.setText("x" + item.get("quantity"));

            double price = item.get("price") instanceof Number ?
                    ((Number) item.get("price")).doubleValue() : 0;
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            tvProductPrice.setText(currencyFormat.format(price));

            layoutOrderItems.addView(itemView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener when activity is destroyed
        if (orderListener != null) {
            orderListener.remove();
        }
    }
}