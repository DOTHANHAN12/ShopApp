package com.example.shopapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class EditAddressActivity extends AppCompatActivity {

    private static final String TAG = "EditAddressActivity";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private EditText editFullName, editPhoneNumber, editCityDistrict, editStreetAddress;
    private Switch switchDefault;
    private Button btnSave, btnDelete;
    private TextView labelAddressTypeHome, labelAddressTypeOffice;

    private String addressDocId = null;
    private String userId;
    private String selectedAddressType = "Nhà Riêng";

    private boolean isInitiallyDefault = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_address);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = user.getUid();

        mapViews();

        addressDocId = getIntent().getStringExtra("ADDRESS_DOC_ID");
        if (addressDocId != null) {
            // Tải địa chỉ cũ (sẽ gọi setupListeners() sau khi tải xong)
            loadExistingAddress(addressDocId);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            // Thêm mới (Gọi setupListeners ngay)
            btnDelete.setVisibility(View.GONE);
            setupListeners();
        }
    }

    private void mapViews() {
        editFullName = findViewById(R.id.edit_full_name);
        editPhoneNumber = findViewById(R.id.edit_phone_number);
        editCityDistrict = findViewById(R.id.edit_city_district);
        editStreetAddress = findViewById(R.id.edit_street_address);

        switchDefault = findViewById(R.id.switch_set_default);
        btnSave = findViewById(R.id.btn_save_address);
        btnDelete = findViewById(R.id.btn_delete_address);

        labelAddressTypeHome = findViewById(R.id.label_address_type_home);
        labelAddressTypeOffice = findViewById(R.id.label_address_type_office);

        findViewById(R.id.img_back_arrow).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveAddress());
        btnDelete.setOnClickListener(v -> deleteAddress());

        // Xử lý chọn loại địa chỉ
        labelAddressTypeHome.setOnClickListener(v -> selectAddressType("Nhà Riêng", labelAddressTypeHome, labelAddressTypeOffice));
        labelAddressTypeOffice.setOnClickListener(v -> selectAddressType("Văn Phòng", labelAddressTypeOffice, labelAddressTypeHome));

        // Logic sửa lỗi Switch: Ngăn chặn tắt switch nếu nó là địa chỉ mặc định ban đầu
        switchDefault.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            // Biến cờ để ngăn vòng lặp vô tận khi setChecked(true)
            private boolean isSettingCheckedProgrammatically = false;

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isSettingCheckedProgrammatically) {
                    return; // Thoát nếu đang set programmatically
                }

                // CHỈ kiểm tra nếu địa chỉ này đã là mặc định TỪ BAN ĐẦU và người dùng cố gắng TẮT
                if (isInitiallyDefault && !isChecked) {

                    // Vô hiệu hóa thao tác TẮT an toàn (KHẮC PHỤC VÒNG LẶP)
                    isSettingCheckedProgrammatically = true;
                    buttonView.setChecked(true); // Set lại về TRUE
                    isSettingCheckedProgrammatically = false;

                    Toast.makeText(EditAddressActivity.this,
                            "Không thể hủy địa chỉ mặc định hiện tại. Vui lòng chọn địa chỉ khác làm mặc định mới trước.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        // Khởi tạo trạng thái chọn loại địa chỉ (đảm bảo style màu đỏ ban đầu)
        selectAddressType(selectedAddressType, labelAddressTypeHome, labelAddressTypeOffice);
    }

    // Logic chọn loại địa chỉ (Nhà Riêng/Văn Phòng)
    private void selectAddressType(String type, TextView selectedView, TextView unselectedView) {
        selectedAddressType = type;

        // *** KHẮC PHỤC LỖI: Sử dụng ContextCompat.getColor ***
        selectedView.setBackgroundResource(R.drawable.rounded_red_border);
        selectedView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));

        unselectedView.setBackgroundResource(R.drawable.rounded_gray_border);
        unselectedView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
    }

    // Tải dữ liệu địa chỉ cũ khi sửa
    private void loadExistingAddress(String docId) {
        db.collection("users").document(userId).collection("addresses").document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Cần có class ShippingAddress (không được cung cấp) để hoạt động,
                        // giả sử nó có các getter tương ứng với fields trong Firestore.
                        // ShippingAddress address = documentSnapshot.toObject(ShippingAddress.class);

                        String fullName = documentSnapshot.getString("fullName");
                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        String cityProvince = documentSnapshot.getString("cityProvince");
                        String district = documentSnapshot.getString("district");
                        String ward = documentSnapshot.getString("ward");
                        String streetAddress = documentSnapshot.getString("streetAddress");
                        String addressType = documentSnapshot.getString("addressType");
                        Boolean isDefault = documentSnapshot.getBoolean("isDefault");

                        editFullName.setText(fullName);
                        editPhoneNumber.setText(phoneNumber);
                        editCityDistrict.setText(String.format("%s, %s, %s", cityProvince, district, ward));
                        editStreetAddress.setText(streetAddress);

                        // KHẮC PHỤC LỖI: Lưu và SET trạng thái mặc định
                        isInitiallyDefault = isDefault != null && isDefault;
                        switchDefault.setChecked(isInitiallyDefault);

                        // Cập nhật loại địa chỉ
                        String type = addressType;
                        if ("Văn Phòng".equals(type)) {
                            selectAddressType("Văn Phòng", labelAddressTypeOffice, labelAddressTypeHome);
                        } else {
                            selectAddressType("Nhà Riêng", labelAddressTypeHome, labelAddressTypeOffice);
                        }
                    }
                    setupListeners(); // Setup Listener sau khi dữ liệu được load
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải địa chỉ cũ.", Toast.LENGTH_SHORT).show();
                    setupListeners(); // Vẫn setup listener ngay cả khi lỗi tải (để người dùng có thể thêm mới)
                });
    }

    // Lưu/Cập nhật địa chỉ
    private void saveAddress() {
        String fullName = editFullName.getText().toString().trim();
        String phoneNumber = editPhoneNumber.getText().toString().trim();
        String cityDistrictWard = editCityDistrict.getText().toString().trim();
        String streetAddress = editStreetAddress.getText().toString().trim();
        boolean isDefault = switchDefault.isChecked();

        if (fullName.isEmpty() || phoneNumber.isEmpty() || cityDistrictWard.isEmpty() || streetAddress.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chia tách City/District/Ward
        String[] parts = cityDistrictWard.split(",\\s*");
        String city = parts.length > 0 ? parts[0] : "";
        String district = parts.length > 1 ? parts[1] : "";
        String ward = parts.length > 2 ? parts[2] : "";

        // Tạo đối tượng Map để lưu
        Map<String, Object> addressData = new HashMap<>();
        addressData.put("fullName", fullName);
        addressData.put("phoneNumber", phoneNumber);
        addressData.put("cityProvince", city);
        addressData.put("district", district);
        addressData.put("ward", ward);
        addressData.put("streetAddress", streetAddress);
        addressData.put("addressType", selectedAddressType);
        addressData.put("isDefault", isDefault);

        DocumentReference docRef;
        if (addressDocId == null) {
            docRef = db.collection("users").document(userId).collection("addresses").document();
        } else {
            docRef = db.collection("users").document(userId).collection("addresses").document(addressDocId);
        }

        // LOGIC QUAN TRỌNG: XỬ LÝ KHI ĐẶT LÀM MẶC ĐỊNH

        if (isDefault) {
            // Trường hợp 1: Đặt địa chỉ này làm mặc định (hoặc nó đã là mặc định)

            db.collection("users").document(userId).collection("addresses")
                    .whereEqualTo("isDefault", true)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        WriteBatch batch = db.batch();
                        DocumentSnapshot oldDefaultDoc = null;

                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            oldDefaultDoc = querySnapshot.getDocuments().get(0);
                        }

                        // Vô hiệu hóa địa chỉ mặc định cũ (nếu có và không phải là chính nó)
                        if (oldDefaultDoc != null && !oldDefaultDoc.getId().equals(docRef.getId())) {
                            batch.update(oldDefaultDoc.getReference(), "isDefault", false);
                        }

                        batch.set(docRef, addressData); // Đặt/Cập nhật địa chỉ hiện tại là mặc định

                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, (addressDocId == null ? "Thêm mới" : "Cập nhật") + " địa chỉ mặc định thành công!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi lưu batch mặc định: ", e);
                                    Toast.makeText(this, "Lỗi lưu địa chỉ.", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi tìm kiếm địa chỉ cũ: ", e);
                        Toast.makeText(this, "Lỗi lưu địa chỉ.", Toast.LENGTH_SHORT).show();
                    });

        } else {
            // Trường hợp 2: KHÔNG đặt làm mặc định
            docRef.set(addressData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, (addressDocId == null ? "Thêm mới" : "Cập nhật") + " địa chỉ thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi lưu địa chỉ: ", e);
                        Toast.makeText(this, "Lỗi lưu địa chỉ.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteAddress() {
        if (addressDocId == null) return;

        if (isInitiallyDefault) {
            Toast.makeText(this, "Không thể xóa địa chỉ mặc định. Vui lòng đặt địa chỉ khác làm mặc định trước.", Toast.LENGTH_LONG).show();
            return;
        }

        db.collection("users").document(userId).collection("addresses").document(addressDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Địa chỉ đã được xóa.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi xóa địa chỉ.", Toast.LENGTH_SHORT).show();
                });
    }
}