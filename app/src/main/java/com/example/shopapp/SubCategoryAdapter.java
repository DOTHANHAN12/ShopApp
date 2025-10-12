package com.example.shopapp;

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

public class SubCategoryAdapter extends RecyclerView.Adapter<SubCategoryAdapter.SubCategoryViewHolder> {

    private final List<SubCategory> subCategoryList;
    private final String currentCategory;

    // Lấy giá trị SHOW_ALL_TYPE từ Model
    private static final String SHOW_ALL_TYPE = SubCategory.SHOW_ALL_TYPE;

    public SubCategoryAdapter(List<SubCategory> subCategoryList, String currentCategory) {
        this.subCategoryList = subCategoryList;
        this.currentCategory = currentCategory;
    }

    @NonNull
    @Override
    public SubCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Layout cần được tạo: item_sub_category.xml
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

        // Xử lý click item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProductListActivity.class);
            intent.putExtra("CATEGORY_KEY", currentCategory);

            // Logic Show All: KHÔNG truyền TYPE_KEY nếu là SHOW ALL
            if (!subCategory.name.equals(SHOW_ALL_TYPE)) {
                intent.putExtra("TYPE_KEY", subCategory.name);
            }

            v.getContext().startActivity(intent);
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