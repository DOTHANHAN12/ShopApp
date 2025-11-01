package com.example.shopapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FilterSortBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "FilterSortBottomSheet";

    // Price filter
    private SeekBar seekBarMinPrice, seekBarMaxPrice;
    private TextView txtMinPrice, txtMaxPrice;

    // Rating filter
    private RadioGroup radioGroupRating;

    // Sort options
    private RadioGroup radioGroupSort;

    // Action buttons
    private Button btnApply, btnReset;

    // Filter data
    private int minPrice = 0;
    private int maxPrice = 10000000; // Max price in Vietnamese Dong
    private int selectedRating = 0; // 0 = all, 1 = 1+, 2 = 2+, 3 = 3+, 4 = 4+, 5 = 5 stars
    private String sortBy = "NEWEST"; // Default sort

    // Listener for applying filters
    private FilterApplyListener filterApplyListener;

    public interface FilterApplyListener {
        void onFiltersApplied(int minPrice, int maxPrice, int minRating, String sortBy);
        void onFiltersReset();
    }

    public void setFilterApplyListener(FilterApplyListener listener) {
        this.filterApplyListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter_sort, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        seekBarMinPrice = view.findViewById(R.id.seekbar_min_price);
        seekBarMaxPrice = view.findViewById(R.id.seekbar_max_price);
        txtMinPrice = view.findViewById(R.id.txt_min_price);
        txtMaxPrice = view.findViewById(R.id.txt_max_price);
        radioGroupRating = view.findViewById(R.id.radio_group_rating);
        radioGroupSort = view.findViewById(R.id.radio_group_sort);
        btnApply = view.findViewById(R.id.btn_apply_filters);
        btnReset = view.findViewById(R.id.btn_reset_filters);

        // Setup seekbars
        setupPriceSeekbars();

        // Setup rating radio group
        radioGroupRating.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_all_ratings) selectedRating = 0;
            else if (checkedId == R.id.radio_1plus) selectedRating = 1;
            else if (checkedId == R.id.radio_2plus) selectedRating = 2;
            else if (checkedId == R.id.radio_3plus) selectedRating = 3;
            else if (checkedId == R.id.radio_4plus) selectedRating = 4;
            else if (checkedId == R.id.radio_5star) selectedRating = 5;
        });

        // Setup sort radio group
        radioGroupSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_newest) sortBy = "NEWEST";
            else if (checkedId == R.id.radio_price_low) sortBy = "PRICE_LOW_TO_HIGH";
            else if (checkedId == R.id.radio_price_high) sortBy = "PRICE_HIGH_TO_LOW";
            else if (checkedId == R.id.radio_rating) sortBy = "RATING";
            else if (checkedId == R.id.radio_popular) sortBy = "POPULAR";
        });

        // Apply button
        btnApply.setOnClickListener(v -> applyFilters());

        // Reset button
        btnReset.setOnClickListener(v -> resetFilters());
    }

    private void setupPriceSeekbars() {
        // Min price seekbar (0 to 5,000,000)
        seekBarMinPrice.setMax(5000000);
        seekBarMinPrice.setProgress(minPrice);
        seekBarMinPrice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minPrice = progress;
                if (minPrice > maxPrice) {
                    maxPrice = minPrice;
                    seekBarMaxPrice.setProgress(minPrice);
                }
                updatePriceDisplay();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Max price seekbar (0 to 10,000,000)
        seekBarMaxPrice.setMax(10000000);
        seekBarMaxPrice.setProgress(maxPrice);
        seekBarMaxPrice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxPrice = progress;
                if (maxPrice < minPrice) {
                    minPrice = maxPrice;
                    seekBarMinPrice.setProgress(maxPrice);
                }
                updatePriceDisplay();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updatePriceDisplay();
    }

    private void updatePriceDisplay() {
        txtMinPrice.setText(formatPrice(minPrice));
        txtMaxPrice.setText(formatPrice(maxPrice));
    }

    private String formatPrice(int price) {
        if (price >= 1000000) {
            return String.format("%.1f M", price / 1000000.0);
        } else if (price >= 1000) {
            return String.format("%.0f K", price / 1000.0);
        } else {
            return String.valueOf(price);
        }
    }

    private void applyFilters() {
        if (filterApplyListener != null) {
            filterApplyListener.onFiltersApplied(minPrice, maxPrice, selectedRating, sortBy);
        }
        dismiss();
    }

    private void resetFilters() {
        minPrice = 0;
        maxPrice = 10000000;
        selectedRating = 0;
        sortBy = "NEWEST";

        seekBarMinPrice.setProgress(minPrice);
        seekBarMaxPrice.setProgress(maxPrice);
        radioGroupRating.check(R.id.radio_all_ratings);
        radioGroupSort.check(R.id.radio_newest);

        updatePriceDisplay();

        if (filterApplyListener != null) {
            filterApplyListener.onFiltersReset();
        }
        dismiss();
    }
}