package com.example.shopapp;

import android.content.Context;
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

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder> {

    private final List<Recommendation> recommendationList;
    private final Context context;

    public RecommendationAdapter(List<Recommendation> recommendationList, Context context) {
        this.recommendationList = recommendationList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_recommendation, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        Recommendation rec = recommendationList.get(position);

        holder.nameTextView.setText(rec.name);
        holder.sizeTextView.setText(rec.sizeRange);
        holder.priceTextView.setText(String.format(Locale.getDefault(), "%,.0f VND", rec.price));

        if (rec.imageUrl != null && !rec.imageUrl.isEmpty()) {
            Picasso.get()
                    .load(rec.imageUrl)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.imageView);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", rec.productId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return recommendationList.size();
    }

    public static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameTextView;
        TextView sizeTextView;
        TextView priceTextView;

        public RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_recommendation_thumb);
            nameTextView = itemView.findViewById(R.id.text_rec_product_name);
            sizeTextView = itemView.findViewById(R.id.text_rec_product_size);
            priceTextView = itemView.findViewById(R.id.text_rec_product_price);
        }
    }
}
