package com.example.shopapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SizeSelectorAdapter extends RecyclerView.Adapter<SizeSelectorAdapter.SizeViewHolder> {

    // INTERFACE BẮT BUỘC ĐỂ GỬI DỮ LIỆU TỒN KHO VÀ GIÁ VỀ ACTIVITY
    public interface OnSizeSelectedListener {
        void onSizeSelected(ProductVariant selectedVariant);
    }

    private final OnSizeSelectedListener listener;

    private final List<ProductVariant> uniqueVariants;
    private int selectedPosition = 0;

    public SizeSelectorAdapter(List<ProductVariant> variantsForColor, OnSizeSelectedListener listener) {
        this.listener = listener;
        this.uniqueVariants = filterUniqueVariants(variantsForColor);
    }

    private List<ProductVariant> filterUniqueVariants(List<ProductVariant> allVariants) {
        // Lọc size duy nhất (chỉ giữ variant đầu tiên cho mỗi size)
        return new ArrayList<>(allVariants.stream()
                .collect(Collectors.toMap(
                        v -> v.size.toUpperCase(),
                        v -> v,
                        (existing, replacement) -> existing
                ))
                .values());
    }

    /** Kích hoạt size đầu tiên và gửi data về Activity */
    public void selectInitialVariant() {
        if (!uniqueVariants.isEmpty() && listener != null) {
            listener.onSizeSelected(uniqueVariants.get(0));
        }
    }

    @NonNull
    @Override
    public SizeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_size_selector, parent, false);
        return new SizeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SizeViewHolder holder, int position) {
        ProductVariant variant = uniqueVariants.get(position);

        holder.sizeTextView.setText(variant.size);

        // Vô hiệu hóa tùy chọn nếu tồn kho = 0
        if (variant.quantity <= 0) {
            holder.sizeTextView.setAlpha(0.5f); // Làm mờ
            holder.sizeTextView.setEnabled(false);
            holder.sizeTextView.setBackgroundResource(R.drawable.rounded_size_disabled); // Cần tạo drawable này
            holder.sizeTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray));
        } else {
            holder.sizeTextView.setAlpha(1.0f);
            holder.sizeTextView.setEnabled(true);

            // Xử lý trạng thái Selected/Unselected
            if (position == selectedPosition) {
                holder.sizeTextView.setBackgroundResource(R.drawable.rounded_size_selected);
                holder.sizeTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            } else {
                holder.sizeTextView.setBackgroundResource(R.drawable.rounded_size_unselected);
                holder.sizeTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
            }
        }


        holder.itemView.setOnClickListener(v -> {
            if (variant.quantity > 0) {
                int previousSelected = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);

                // GỬI DATA VỀ ACTIVITY
                if (listener != null) {
                    listener.onSizeSelected(variant);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return uniqueVariants.size();
    }

    public static class SizeViewHolder extends RecyclerView.ViewHolder {
        TextView sizeTextView;

        public SizeViewHolder(@NonNull View itemView) {
            super(itemView);
            sizeTextView = itemView.findViewById(R.id.text_size_option);
        }
    }
}