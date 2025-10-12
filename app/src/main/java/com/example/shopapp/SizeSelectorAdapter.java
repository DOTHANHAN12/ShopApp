package com.example.shopapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.stream.Collectors;

public class SizeSelectorAdapter extends RecyclerView.Adapter<SizeSelectorAdapter.SizeViewHolder> {

    private final List<String> availableSizes;
    private int selectedPosition = 0;

    public SizeSelectorAdapter(List<ProductVariant> variantsForColor) {
        // Lấy danh sách Size duy nhất từ List<ProductVariant>
        this.availableSizes = variantsForColor.stream()
                .map(v -> v.size)
                .distinct() // Lọc size trùng lặp
                .collect(Collectors.toList());
    }

    @NonNull
    @Override
    public SizeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_size_selector, parent, false);
        return new SizeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SizeViewHolder holder, int position) {
        String size = availableSizes.get(position);
        holder.sizeTextView.setText(size);

        // Xử lý trạng thái Selected/Unselected
        if (position == selectedPosition) {
            holder.sizeTextView.setBackgroundResource(R.drawable.rounded_size_selected);
            holder.sizeTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        } else {
            holder.sizeTextView.setBackgroundResource(R.drawable.rounded_size_unselected);
            holder.sizeTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            // TODO: Gửi sự kiện size được chọn về Activity để tính lại tồn kho/giá
        });
    }

    @Override
    public int getItemCount() {
        return availableSizes.size();
    }

    public static class SizeViewHolder extends RecyclerView.ViewHolder {
        TextView sizeTextView;

        public SizeViewHolder(@NonNull View itemView) {
            super(itemView);
            sizeTextView = itemView.findViewById(R.id.text_size_option);
        }
    }
}