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
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

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
        Log.d(TAG, "Binding ViewHolder for order: " + order.getOrderId() + " with status: " + order.getOrderStatus());
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textOrderId, textOrderStatus, textOrderDate, textOrderTotal;
        private final Button btnCancelOrder;
        private final FirebaseFirestore db = FirebaseFirestore.getInstance();

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            textOrderId = itemView.findViewById(R.id.text_order_id);
            textOrderStatus = itemView.findViewById(R.id.text_order_status);
            textOrderDate = itemView.findViewById(R.id.text_order_date);
            textOrderTotal = itemView.findViewById(R.id.text_order_total);
            btnCancelOrder = itemView.findViewById(R.id.btn_cancel_order);
        }

        public void bind(Order order) {
            textOrderId.setText("Mã đơn: " + order.getOrderId());
            textOrderStatus.setText(order.getOrderStatus());

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (order.getOrderDate() != null) {
                textOrderDate.setText("Ngày đặt: " + sdf.format(order.getOrderDate()));
            } else {
                textOrderDate.setText("Ngày đặt: Không xác định");
            }
            textOrderTotal.setText(String.format(Locale.getDefault(), "Tổng tiền: %,.0f VND", order.getTotalAmount()));

            if ("PENDING".equalsIgnoreCase(order.getOrderStatus())) {
                btnCancelOrder.setVisibility(View.VISIBLE);
                btnCancelOrder.setOnClickListener(v -> cancelOrder(order, getAdapterPosition()));
            } else {
                btnCancelOrder.setVisibility(View.GONE);
            }
        }

        private void cancelOrder(Order order, int position) {
            db.collection("orders").document(order.getOrderId())
                    .update("orderStatus", "CANCELLED")
                    .addOnSuccessListener(aVoid -> {
                        order.setOrderStatus("CANCELLED");
                        notifyItemChanged(position);
                        Toast.makeText(context, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Lỗi khi hủy đơn hàng", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error cancelling order", e);
                    });
        }
    }
}
