package com.example.shopapp;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ColorSelectorAdapter extends RecyclerView.Adapter<ColorSelectorAdapter.ColorViewHolder> {

    private final List<String> colorNames;
    private int selectedPosition = 0;
    private final OnColorSelectedListener listener;

    public interface OnColorSelectedListener {
        void onColorSelected(String colorName);
    }

    public ColorSelectorAdapter(List<String> colorNames, OnColorSelectedListener listener) {
        this.colorNames = colorNames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_selector, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String colorName = colorNames.get(position);

        int colorCode = getColorCode(colorName);

        // Thiết lập màu sắc cho vòng tròn
        GradientDrawable drawable = (GradientDrawable) holder.colorCircle.getBackground();
        drawable.setColor(colorCode);

        // Xử lý trạng thái selected/unselected
        if (position == selectedPosition) {
            holder.border.setVisibility(View.VISIBLE);
        } else {
            holder.border.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            listener.onColorSelected(colorName);
        });
    }

    @Override
    public int getItemCount() {
        return colorNames.size();
    }

    public static class ColorViewHolder extends RecyclerView.ViewHolder {
        View colorCircle;
        View border;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorCircle = itemView.findViewById(R.id.color_circle);
            border = itemView.findViewById(R.id.color_circle_border);
        }
    }

    // HÀM ÁNH XẠ MÀU
    private int getColorCode(String colorName) {
        try {
            // Thử phân tích mã Hex nếu có
            return Color.parseColor(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Xử lý các tên màu phổ biến nếu không phải là mã Hex
            switch (colorName.toUpperCase()) {
                case "BLACK":
                case "08 DARK GRAY": // Xử lý trường hợp màu từ mẫu
                    return Color.parseColor("#444444");
                case "NAVY": return Color.parseColor("#1B293C");
                case "WHITE": return Color.WHITE;
                case "GRAY": return Color.GRAY;
                case "RED": return Color.RED;
                default: return Color.LTGRAY;
            }
        }
    }
}