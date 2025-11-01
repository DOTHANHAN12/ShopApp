package com.example.shopapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
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

    private static final String TAG = "SubCategoryAdapter"; // Tag for logging

    private final List<SubCategory> subCategoryList;
    private final String currentCategory;
    private final Context context;

    private static final String SHOW_ALL_TYPE = SubCategory.SHOW_ALL_TYPE;

    public SubCategoryAdapter(List<SubCategory> subCategoryList, String currentCategory, Context context) {
        this.subCategoryList = subCategoryList;
        this.currentCategory = currentCategory;
        this.context = context;
    }

    @NonNull
    @Override
    public SubCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sub_category, parent, false);
        return new SubCategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubCategoryViewHolder holder, int position) {
        try {
            SubCategory subCategory = subCategoryList.get(position);

            Log.d(TAG, "Binding position: " + position + ", Name: " + subCategory.name);

            if (holder.nameTextView == null) {
                Log.e(TAG, "FATAL: nameTextView is null at position " + position + ". Check R.id.text_sub_category_name in item_sub_category.xml");
            } else {
                holder.nameTextView.setText(subCategory.name);
            }

            if (holder.iconImageView == null) {
                Log.e(TAG, "FATAL: iconImageView is null at position " + position + ". Check R.id.img_sub_category in item_sub_category.xml");
            } else {
                if (subCategory.imageUrl != null && !subCategory.imageUrl.isEmpty()) {
                    Picasso.get()
                            .load(subCategory.imageUrl)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(holder.iconImageView);
                } else {
                    holder.iconImageView.setImageResource(R.drawable.ic_launcher_foreground);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ProductListActivity.class);
                intent.putExtra("CATEGORY_KEY", currentCategory);

                if (!subCategory.name.equals(SHOW_ALL_TYPE)) {
                    intent.putExtra("TYPE_KEY", subCategory.name.toUpperCase(Locale.ROOT));
                }

                context.startActivity(intent);
            });
        } catch (Exception e) {
            // Catch any unexpected crash and log it
            Log.e(TAG, "CRASH in onBindViewHolder at position " + position, e);
        }
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

            // Log results of findViewById right away
            if (iconImageView == null) {
                Log.e(TAG, "ViewHolder Error: img_sub_category not found in layout!");
            }
            if (nameTextView == null) {
                Log.e(TAG, "ViewHolder Error: text_sub_category_name not found in layout! This will cause a crash.");
            }
        }
    }
}
