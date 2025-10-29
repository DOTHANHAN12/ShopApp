package com.example.shopapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date; // Import đã được thêm
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        private final TextView textOrderId, textOrderStatus, textOrderDate, textOrderTotal;
        private final Button btnCancelOrder;
        private final RecyclerView productsRecyclerView;
        private final FirebaseFirestore db = FirebaseFirestore.getInstance();

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            textOrderId = itemView.findViewById(R.id.text_order_id);
            textOrderStatus = itemView.findViewById(R.id.text_order_status);
            textOrderDate = itemView.findViewById(R.id.text_order_date);
            textOrderTotal = itemView.findViewById(R.id.text_order_total);
            btnCancelOrder = itemView.findViewById(R.id.btn_cancel_order);
            productsRecyclerView = itemView.findViewById(R.id.recycler_view_order_products);
        }

        public void bind(Order order) {
            textOrderId.setText("Mã đơn: " + order.getOrderId());
            textOrderStatus.setText(order.getOrderStatus());

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            textOrderDate.setText(order.getCreatedAt() != null ? sdf.format(new Date(order.getCreatedAt())) : "N/A");
            textOrderTotal.setText(String.format(Locale.getDefault(), "Tổng tiền: %,.0f VND", order.getTotalAmount()));

            if (order.getItems() != null && !order.getItems().isEmpty()) {
                productsRecyclerView.setVisibility(View.VISIBLE);
                productsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
                OrderProductAdapter productAdapter = new OrderProductAdapter(context, order.getItems(), order.getOrderStatus(), order.getOrderId());
                productsRecyclerView.setAdapter(productAdapter);
            } else {
                productsRecyclerView.setVisibility(View.GONE);
            }

            if ("PENDING".equalsIgnoreCase(order.getOrderStatus()) || "PROCESSING".equalsIgnoreCase(order.getOrderStatus())) {
                btnCancelOrder.setVisibility(View.VISIBLE);
                btnCancelOrder.setOnClickListener(v -> cancelOrder(order, getAdapterPosition()));
            } else {
                btnCancelOrder.setVisibility(View.GONE);
            }
        }

        private void cancelOrder(Order order, int position) {
            db.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentReference orderRef = db.collection("orders").document(order.getOrderId());

                for (Map<String, Object> item : order.getItems()) {
                    String productId = (String) item.get("productId");
                    String variantId = (String) item.get("variantId");
                    long quantityToRestore = ((Number) item.get("quantity")).longValue();

                    if (productId == null || variantId == null || quantityToRestore <= 0) continue;

                    DocumentReference productRef = db.collection("products").document(productId);
                    DocumentSnapshot productSnapshot = transaction.get(productRef);

                    List<Map<String, Object>> variants = (List<Map<String, Object>>) productSnapshot.get("variants");
                    if (variants != null) {
                        boolean variantUpdated = false;
                        for (Map<String, Object> variant : variants) {
                            if (variantId.equals(variant.get("variantId"))) {
                                long currentQuantity = (long) variant.get("quantity");
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

                transaction.update(orderRef, "orderStatus", "CANCELLED");
                return null;

            }).addOnSuccessListener(aVoid -> {
                order.setOrderStatus("CANCELLED");
                notifyItemChanged(position);
                Toast.makeText(context, "Đã hủy đơn hàng và hoàn kho thành công.", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(context, "Lỗi khi hủy đơn hàng.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error cancelling order", e);
            });
        }
    }
}
