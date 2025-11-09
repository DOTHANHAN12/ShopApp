package com.example.shopapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    private static final String TAG = "OrderHistoryAdapter";

    private final List<Order> orderList;
    private final Context context;

    public OrderHistoryAdapter(List<Order> orderList, Context context) {
        this.orderList = orderList;
        this.context = context;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardOrderItem;
        private final TextView textOrderId, textOrderStatus, textOrderDate, textOrderTotal;
        private final CardView cardStatusBadge;
        private final Button btnCancelOrder, btnRetryPayment;
        private final RecyclerView productsRecyclerView;
        private final FirebaseFirestore db = FirebaseFirestore.getInstance();

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardOrderItem = itemView.findViewById(R.id.card_order_item);
            textOrderId = itemView.findViewById(R.id.text_order_id);
            textOrderStatus = itemView.findViewById(R.id.text_order_status);
            textOrderDate = itemView.findViewById(R.id.text_order_date);
            textOrderTotal = itemView.findViewById(R.id.text_order_total);
            cardStatusBadge = itemView.findViewById(R.id.card_status_badge);
            btnCancelOrder = itemView.findViewById(R.id.btn_cancel_order);
            btnRetryPayment = itemView.findViewById(R.id.btn_retry_payment);
            productsRecyclerView = itemView.findViewById(R.id.recycler_view_order_products);
        }

        public void bind(Order order) {
            // Order ID
            String shortId = order.getOrderId() != null ?
                    order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())).toUpperCase() : "N/A";
            textOrderId.setText("Đơn hàng #" + shortId);

            // Order Status with color
            String statusText = getStatusText(order.getOrderStatus());
            textOrderStatus.setText(statusText);
            cardStatusBadge.setCardBackgroundColor(getStatusColor(order.getOrderStatus()));

            // Order Date
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String dateText = order.getCreatedAt() != null ?
                    sdf.format(new Date(order.getCreatedAt())) : "N/A";
            textOrderDate.setText(dateText);

            // Order Total
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            textOrderTotal.setText(currencyFormat.format(order.getTotalAmount()));

            // Products List
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                productsRecyclerView.setVisibility(View.VISIBLE);
                productsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                OrderProductAdapter productAdapter = new OrderProductAdapter(
                        context,
                        order.getItems(),
                        order.getOrderStatus(),
                        order.getOrderId()
                );
                productsRecyclerView.setAdapter(productAdapter);
            } else {
                productsRecyclerView.setVisibility(View.GONE);
            }

            // Click to view details
            cardOrderItem.setOnClickListener(v -> {
                Intent intent = new Intent(context, OrderDetailActivity.class);
                intent.putExtra("ORDER_ID", order.getOrderId());
                context.startActivity(intent);
            });

            // Button: Hủy đơn (for PENDING, CONFIRMED, PROCESSING)
            if ("PENDING".equalsIgnoreCase(order.getOrderStatus()) ||
                    "CONFIRMED".equalsIgnoreCase(order.getOrderStatus()) ||
                    "PROCESSING".equalsIgnoreCase(order.getOrderStatus())) {
                btnCancelOrder.setVisibility(View.VISIBLE);
                btnCancelOrder.setOnClickListener(v -> cancelOrder(order, getAdapterPosition()));
            } else {
                btnCancelOrder.setVisibility(View.GONE);
            }

            // Button: Thanh toán lại (for FAILED_PAYMENT)
            if ("FAILED_PAYMENT".equalsIgnoreCase(order.getOrderStatus()) &&
                    "VNPAY".equalsIgnoreCase(order.getPaymentMethod())) {
                btnRetryPayment.setVisibility(View.VISIBLE);
                btnRetryPayment.setOnClickListener(v -> retryPayment(order));
            } else {
                btnRetryPayment.setVisibility(View.GONE);
            }
        }

        private String getStatusText(String status) {
            if (status == null) return "Không xác định";
            switch (status.toUpperCase()) {
                case "PENDING": return "Chờ xác nhận";
                case "CONFIRMED": return "Đã xác nhận";
                case "PROCESSING": return "Đang xử lý";
                case "SHIPPING": return "Đang giao";
                case "DELIVERED": return "Đã giao";
                case "COMPLETED": return "Hoàn thành";
                case "CANCELLED": return "Đã hủy";
                case "REFUNDED": return "Đã hoàn tiền";
                case "PAID": return "Đã thanh toán";
                case "FAILED_PAYMENT": return "Thanh toán thất bại";
                default: return status;
            }
        }

        private int getStatusColor(String status) {
            if (status == null) return Color.parseColor("#9E9E9E");
            switch (status.toUpperCase()) {
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

        private void cancelOrder(Order order, int position) {
            db.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentReference orderRef = db.collection("orders").document(order.getOrderId());

                // ✅ BƯỚC 1: ĐỌC TẤT CẢ dữ liệu trước (READ phase)
                List<DocumentSnapshot> productSnapshots = new ArrayList<>();
                for (Map<String, Object> item : order.getItems()) {
                    String productId = (String) item.get("productId");
                    if (productId != null) {
                        DocumentReference productRef = db.collection("products").document(productId);
                        DocumentSnapshot productSnapshot = transaction.get(productRef);
                        productSnapshots.add(productSnapshot);
                    }
                }

                // ✅ BƯỚC 2: GHI dữ liệu (WRITE phase)
                int index = 0;
                for (Map<String, Object> item : order.getItems()) {
                    String productId = (String) item.get("productId");
                    String variantId = (String) item.get("variantId");
                    long quantityToRestore = ((Number) item.get("quantity")).longValue();

                    if (productId == null || variantId == null || quantityToRestore <= 0) {
                        index++;
                        continue;
                    }

                    DocumentReference productRef = db.collection("products").document(productId);
                    DocumentSnapshot productSnapshot = productSnapshots.get(index);

                    if (productSnapshot.exists()) {
                        List<Map<String, Object>> variants = (List<Map<String, Object>>) productSnapshot.get("variants");
                        if (variants != null) {
                            boolean variantUpdated = false;
                            for (Map<String, Object> variant : variants) {
                                if (variantId.equals(variant.get("variantId"))) {
                                    long currentQuantity = ((Number) variant.get("quantity")).longValue();
                                    variant.put("quantity", currentQuantity + quantityToRestore);
                                    variantUpdated = true;
                                    break;
                                }
                            }
                            if (variantUpdated) {
                                transaction.update(productRef, "variants", variants);
                            }
                        }
                    }
                    index++;
                }

                // Cuối cùng update order status
                transaction.update(orderRef, "orderStatus", "CANCELLED");
                return null;

            }).addOnSuccessListener(aVoid -> {
                order.setOrderStatus("CANCELLED");
                notifyItemChanged(position);
                Toast.makeText(context, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(context, "Lỗi khi hủy đơn hàng", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error cancelling order", e);
            });
        }

        private void retryPayment(Order order) {
            // Tạo lại payment URL với VNPay
            String currentTxnRef = "ORDER_" + System.currentTimeMillis();
            long amount = (long) order.getTotalAmount() * 100;

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", VNPayConfig.vnp_Version);
            vnp_Params.put("vnp_Command", VNPayConfig.vnp_Command);
            vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", currentTxnRef);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan lai don hang " + order.getOrderId());
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            String paymentUrl = VNPayConfig.getPaymentUrl(vnp_Params);

            // Mở VNPayActivity với order hiện tại
            Intent intent = new Intent(context, VNPayRetryActivity.class);
            intent.putExtra("payment_url", paymentUrl);
            intent.putExtra("order_id", order.getOrderId());
            context.startActivity(intent);
        }
    }
}