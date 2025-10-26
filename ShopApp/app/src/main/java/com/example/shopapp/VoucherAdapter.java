package com.example.shopapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private final Context context;
    private final List<Voucher> voucherList;
    private final OnVoucherClickListener listener;
    private Voucher selectedVoucher; // <-- Voucher đang được chọn
    private final double currentSubtotal;

    public VoucherAdapter(Context context, List<Voucher> voucherList, OnVoucherClickListener listener, double currentSubtotal) {
        this.context = context;
        this.voucherList = voucherList;
        this.listener = listener;
        this.currentSubtotal = currentSubtotal;
    }

    public void setSelectedVoucher(Voucher voucher) {
        // Luôn lưu trữ Voucher object được chọn (hoặc null)
        this.selectedVoucher = voucher;
        // Bắt buộc phải thông báo để RecyclerView vẽ lại
        notifyDataSetChanged();
    }

    // Phương thức này được sử dụng trong Activity để xem voucher nào đang được chọn
    public Voucher getSelectedVoucher() {
        return selectedVoucher;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        final Voucher voucher = voucherList.get(position);

        // Định dạng UI
        String discountText;
        if ("PERCENT".equals(voucher.getDiscountType())) {
            discountText = "Giảm " + voucher.getDiscountValue() + "% tối đa 100k";
        } else {
            discountText = String.format(Locale.getDefault(), "Giảm tới đa %,.0f₫", voucher.getDiscountValue());
        }
        String conditionText = String.format(Locale.getDefault(), "Đơn tối thiểu %,.0f₫", voucher.getMinOrderValue());

        holder.textDiscount.setText(discountText);
        holder.textCondition.setText(conditionText);

        if ("SHIPPING".equals(voucher.getDiscountType())) {
            holder.imgVoucherIcon.setImageResource(R.drawable.ic_freeship);
        } else {
            holder.imgVoucherIcon.setImageResource(R.drawable.ic_discount);
        }

        // --- Logic Hiển thị Trạng thái (Quan trọng nhất) ---
        boolean isEligible = currentSubtotal >= voucher.getMinOrderValue();

        // 1. Logic Dấu tích: Kiểm tra bằng Code VÀ Object reference
        boolean isCurrentVoucherSelected = (selectedVoucher != null && selectedVoucher.getCode().equals(voucher.getCode()));

        if (isCurrentVoucherSelected) {
            holder.imgCheck.setVisibility(View.VISIBLE);
            // Có thể thêm hiệu ứng viền cho CardView nếu muốn
        } else {
            holder.imgCheck.setVisibility(View.GONE);
        }
        // ----------------------------------------------------

        if (isEligible) {
            holder.textNotAvailable.setVisibility(View.GONE);
            holder.textCondition.setTextColor(ContextCompat.getColor(context, R.color.grey_dark));
            holder.voucherCard.setAlpha(1.0f);

            // Gắn Listener (Khả dụng)
            holder.itemView.setOnClickListener(v -> listener.onVoucherClicked(voucher));

        } else {
            // Voucher không khả dụng (chưa đạt giá trị đơn hàng tối thiểu)
            holder.textNotAvailable.setVisibility(View.VISIBLE);
            holder.textNotAvailable.setText("Chưa đạt giá trị đơn hàng tối thiểu");
            holder.textCondition.setTextColor(ContextCompat.getColor(context, R.color.grey_medium));
            holder.imgCheck.setVisibility(View.GONE); // KHÔNG TÍCH NẾU KHÔNG KHẢ DỤNG
            holder.voucherCard.setAlpha(0.5f);

            // Gắn Listener (Không khả dụng)
            holder.itemView.setOnClickListener(v -> {
                Toast.makeText(context, holder.textNotAvailable.getText().toString(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    public static class VoucherViewHolder extends RecyclerView.ViewHolder {
        CardView voucherCard;
        TextView textDiscount;
        TextView textCondition;
        TextView textNotAvailable;
        ImageView imgCheck;
        ImageView imgVoucherIcon;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            voucherCard = (CardView) itemView;

            // Tìm kiếm ID bên trong itemView
            textDiscount = itemView.findViewById(R.id.text_voucher_discount_value);
            textCondition = itemView.findViewById(R.id.text_voucher_condition);
            textNotAvailable = itemView.findViewById(R.id.text_voucher_not_available);
            imgCheck = itemView.findViewById(R.id.img_voucher_check);
            imgVoucherIcon = itemView.findViewById(R.id.img_voucher_icon);
        }
    }

    public interface OnVoucherClickListener {
        void onVoucherClicked(Voucher voucher);
    }
}