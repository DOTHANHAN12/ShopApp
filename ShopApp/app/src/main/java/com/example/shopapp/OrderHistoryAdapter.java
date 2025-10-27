package com.example.shopapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    private static final String TAG = "OrderHistoryAdapter"; // Thêm TAG để lọc log

    private final List<Order> orderList;

    public OrderHistoryAdapter(List<Order> orderList) {
        this.orderList = orderList;
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
        Log.d(TAG, "Binding ViewHolder cho đơn hàng: " + order.getOrderId());
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textOrderId, textOrderStatus, textOrderDate, textOrderTotal;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            textOrderId = itemView.findViewById(R.id.text_order_id);
            textOrderStatus = itemView.findViewById(R.id.text_order_status);
            textOrderDate = itemView.findViewById(R.id.text_order_date);
            textOrderTotal = itemView.findViewById(R.id.text_order_total);
        }

        public void bind(Order order) {
            textOrderId.setText("Mã đơn: " + order.getOrderId());
            textOrderStatus.setText(order.getStatus());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (order.getOrderDate() != null) {
                textOrderDate.setText("Ngày đặt: " + sdf.format(order.getOrderDate()));
            } else {
                textOrderDate.setText("Ngày đặt: Không xác định");
                Log.w(TAG, "Đơn hàng " + order.getOrderId() + " không có ngày đặt.");
            }
            textOrderTotal.setText(String.format(Locale.getDefault(), "Tổng tiền: %,.0f VND", order.getTotalAmount()));
        }
    }
}
