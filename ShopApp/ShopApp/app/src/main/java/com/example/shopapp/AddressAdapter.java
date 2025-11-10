package com.example.shopapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    private final Context context;
    private final List<ShippingAddress> addressList;
    private final OnAddressActionListener listener;

    private String currentSelectedAddressId;

    public interface OnAddressActionListener {
        void onAddressSelected(ShippingAddress selectedAddress);
        void onEditClicked(ShippingAddress address);
    }

    public AddressAdapter(Context context, List<ShippingAddress> addressList, OnAddressActionListener listener, String currentSelectedAddressId) {
        this.context = context;
        this.addressList = addressList;
        this.listener = listener;
        this.currentSelectedAddressId = currentSelectedAddressId;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shipping_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        ShippingAddress address = addressList.get(position);

        holder.textNamePhone.setText(String.format("%s | (+84) %s", address.getFullName(), address.getPhoneNumber()));

        holder.textStreet.setText(address.getStreetAddress());
        holder.textLocation.setText(address.getFullLocation());

        // 1. Kiểm tra trạng thái dấu chấm (RadioButton)
        boolean isChecked = false;
        if (address.getDocumentId() != null && address.getDocumentId().equals(currentSelectedAddressId)) {
            isChecked = true;
        } else if (currentSelectedAddressId == null && address.getIsDefault()) {
            isChecked = true;
        }
        holder.radioButton.setChecked(isChecked);

        // 2. Hiển thị Label "Mặc định" (màu đỏ, ở trên bên cạnh tên)
        if (address.getIsDefault()) {
            holder.textDefaultTag.setVisibility(View.VISIBLE);
            holder.textDefaultTag.setText("Mặc định");
        } else {
            holder.textDefaultTag.setVisibility(View.GONE);
        }

        // 3. Hiển thị Loại Địa Chỉ (viền đỏ, ở dưới)
        if (address.getAddressType() != null && !address.getAddressType().isEmpty()) {
            holder.textAddressType.setVisibility(View.VISIBLE);

            if ("Nhà Riêng".equals(address.getAddressType())) {
                holder.textAddressType.setText("🏠 Nhà Riêng");
            } else if ("Văn Phòng".equals(address.getAddressType())) {
                holder.textAddressType.setText("🏢 Văn Phòng");
            } else {
                holder.textAddressType.setText(address.getAddressType());
            }
        } else {
            holder.textAddressType.setVisibility(View.GONE);
        }

        // Xử lý sự kiện khi click vào toàn bộ item
        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                ShippingAddress clickedAddress = addressList.get(currentPosition);

                currentSelectedAddressId = clickedAddress.getDocumentId();
                notifyDataSetChanged();

                listener.onAddressSelected(clickedAddress);
            }
        });

        // Xử lý sự kiện click vào nút Sửa
        holder.textEdit.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                listener.onEditClicked(addressList.get(currentPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        public RadioButton radioButton;
        public TextView textNamePhone;
        public TextView textStreet;
        public TextView textLocation;
        public TextView textEdit;
        public TextView textDefaultTag;
        public TextView textAddressType; // ✅ THÊM

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);

            radioButton = itemView.findViewById(R.id.radio_select_address);
            textNamePhone = itemView.findViewById(R.id.text_name_phone);
            textStreet = itemView.findViewById(R.id.text_street_address);
            textLocation = itemView.findViewById(R.id.text_location);
            textEdit = itemView.findViewById(R.id.btn_edit_address);
            textDefaultTag = itemView.findViewById(R.id.label_default);
            textAddressType = itemView.findViewById(R.id.text_address_type  ); // ✅ THÊM

            radioButton.setClickable(false);
            radioButton.setFocusable(false);
        }
    }
}