package com.example.shopapp;

import android.content.Context; // <--- THÊM IMPORT NÀY
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

public class SubCategoryAdapter extends RecyclerView.Adapter<SubCategoryAdapter.SubCategoryViewHolder> {

    private final List<SubCategory> subCategoryList;
    private final String currentCategory;
    private final Context context; // KHAI BÁO CONTEXT

    // Lấy giá trị SHOW_ALL_TYPE từ Model
    private static final String SHOW_ALL_TYPE = SubCategory.SHOW_ALL_TYPE;

    // SỬA LỖI: Constructor phải nhận Context
    public SubCategoryAdapter(List<SubCategory> subCategoryList, String currentCategory, Context context) {
        this.subCategoryList = subCategoryList;
        this.currentCategory = currentCategory;
        this.context = context; // Gán Context
    }

    @NonNull
    @Override
    public SubCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sub_category, parent, false);
        return new SubCategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubCategoryViewHolder holder, int position) {
        SubCategory subCategory = subCategoryList.get(position);

        holder.nameTextView.setText(subCategory.name);

        // Tải ảnh từ URL
        if (subCategory.imageUrl != null && !subCategory.imageUrl.isEmpty()) {
            Picasso.get()
                    .load(subCategory.imageUrl)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.iconImageView);
        } else {
            holder.iconImageView.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // 3. Xử lý click item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductListActivity.class);
            // Category đã là chữ hoa từ Activity trước đó (ví dụ: WOMEN)
            intent.putExtra("CATEGORY_KEY", currentCategory);

            if (!subCategory.name.equals(SHOW_ALL_TYPE)) {
                // TRUYỀN TYPE SANG CHỮ HOA ĐỂ KHỚP VỚI LOGIC LỌC
                intent.putExtra("TYPE_KEY", subCategory.name.toUpperCase(Locale.ROOT));
            }

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return subCategoryList.size();
    }

    public static class SubCategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView nameTextView;

        public SubCategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.img_sub_category);
            nameTextView = itemView.findViewById(R.id.text_sub_category_name);
        }
    }
}