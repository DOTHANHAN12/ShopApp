package com.example.shopapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class EditAddressActivity extends AppCompatActivity {

    private static final String TAG = "EditAddressActivity";

    // UI Elements
    private AutoCompleteTextView editCityDistrict;
    private AutoCompleteTextView editDistrict;
    private TextInputEditText editWard;
    private TextInputEditText editStreetAddress;
    private TextInputEditText editFullName;
    private TextInputEditText editPhoneNumber;
    private Switch switchSetDefault;
    private TextInputLayout layoutDistrict;
    private View labelAddressTypeHome;
    private View labelAddressTypeOffice;

    // Data
    private String selectedProvince = null;
    private String selectedDistrict = null;
    private String selectedAddressType = "Nhà Riêng"; // Mặc định
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private String addressDocId = null; // Nếu đang edit địa chỉ có sẵn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_address);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        // Kiểm tra có edit địa chỉ cũ không
        addressDocId = getIntent().getStringExtra("ADDRESS_DOC_ID");

        mapViews();
        setupProvinceDropdown();
        setupListeners();

        // ✅ THÊM: Update UI ban đầu
        updateAddressTypeUI();

        // Nếu đang edit, load dữ liệu cũ
        if (addressDocId != null) {
            loadAddressData(addressDocId);
        }
    }

    private void mapViews() {
        editCityDistrict = findViewById(R.id.edit_city_district);
        editDistrict = findViewById(R.id.edit_district);
        editWard = findViewById(R.id.edit_ward);
        editStreetAddress = findViewById(R.id.edit_street_address);
        editFullName = findViewById(R.id.edit_full_name);
        editPhoneNumber = findViewById(R.id.edit_phone_number);
        switchSetDefault = findViewById(R.id.switch_set_default);
        layoutDistrict = findViewById(R.id.layout_district);
        labelAddressTypeHome = findViewById(R.id.label_address_type_home);
        labelAddressTypeOffice = findViewById(R.id.label_address_type_office);

        ImageView imgBack = findViewById(R.id.img_back_arrow);
        imgBack.setOnClickListener(v -> finish());

        findViewById(R.id.btn_save_address).setOnClickListener(v -> saveAddress());
        findViewById(R.id.btn_delete_address).setOnClickListener(v -> deleteAddress());
    }

    private void setupProvinceDropdown() {
        // Lấy danh sách tỉnh/thành phố
        List<String> provinces = VietnamLocationData.getProvinces();

        ArrayAdapter<String> provinceAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                provinces
        );

        editCityDistrict.setAdapter(provinceAdapter);

        // Khi chọn tỉnh/thành phố
        editCityDistrict.setOnItemClickListener((parent, view, position, id) -> {
            selectedProvince = provinces.get(position);
            setupDistrictDropdown(selectedProvince);
        });
    }

    private void setupDistrictDropdown(String province) {
        // Kiểm tra tỉnh có dữ liệu quận/huyện không
        if (VietnamLocationData.hasDistrictData(province)) {
            List<String> districts = VietnamLocationData.getDistricts(province);

            ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    districts
            );

            editDistrict.setAdapter(districtAdapter);
            editDistrict.setText(""); // Clear old value
            layoutDistrict.setVisibility(View.VISIBLE);

            // Khi chọn quận/huyện
            editDistrict.setOnItemClickListener((parent, view, position, id) -> {
                selectedDistrict = districts.get(position);
            });
        } else {
            // Tỉnh chưa có dữ liệu quận/huyện, ẩn dropdown
            layoutDistrict.setVisibility(View.GONE);
            selectedDistrict = null;
        }
    }

    private void setupListeners() {
        // Address Type Selection - Nhà Riêng
        labelAddressTypeHome.setOnClickListener(v -> {
            selectedAddressType = "Nhà Riêng";
            updateAddressTypeUI();
        });

        // Address Type Selection - Văn Phòng
        labelAddressTypeOffice.setOnClickListener(v -> {
            selectedAddressType = "Văn Phòng";
            updateAddressTypeUI();
        });

        // Switch Default Address - Kiểm tra logic thông minh
        switchSetDefault.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked && addressDocId != null) {
                // Kiểm tra nếu đây là địa chỉ mặc định duy nhất
                checkIfCanDisableDefault();
            }
        });
    }

    private void updateAddressTypeUI() {
        if ("Nhà Riêng".equals(selectedAddressType)) {
            labelAddressTypeHome.setBackgroundResource(R.drawable.rounded_red_border);
            labelAddressTypeOffice.setBackgroundResource(R.drawable.rounded_gray_border);
        } else {
            labelAddressTypeHome.setBackgroundResource(R.drawable.rounded_gray_border);
            labelAddressTypeOffice.setBackgroundResource(R.drawable.rounded_red_border);
        }
    }

    private void checkIfCanDisableDefault() {
        if (userId == null) return;

        // Đếm số địa chỉ hiện có
        db.collection("users").document(userId).collection("addresses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalAddresses = querySnapshot.size();

                    if (totalAddresses <= 1) {
                        // Chỉ có 1 địa chỉ duy nhất, không cho tắt mặc định
                        switchSetDefault.setChecked(true);
                        Toast.makeText(this,
                                "Bạn cần thêm địa chỉ khác trước khi bỏ địa chỉ mặc định này!",
                                Toast.LENGTH_LONG).show();
                    } else {
                        // Có nhiều địa chỉ, kiểm tra xem có địa chỉ mặc định nào khác không
                        checkOtherDefaultAddress();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi kiểm tra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    switchSetDefault.setChecked(true); // Fallback: giữ mặc định
                });
    }

    private void checkOtherDefaultAddress() {
        if (userId == null || addressDocId == null) return;

        db.collection("users").document(userId).collection("addresses")
                .whereEqualTo("isDefault", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasOtherDefault = false;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (!doc.getId().equals(addressDocId)) {
                            hasOtherDefault = true;
                            break;
                        }
                    }

                    if (!hasOtherDefault && querySnapshot.size() == 1) {
                        // Đây là địa chỉ mặc định duy nhất
                        switchSetDefault.setChecked(true);
                        Toast.makeText(this,
                                "Vui lòng chọn địa chỉ khác làm mặc định trước!",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    switchSetDefault.setChecked(true); // Fallback
                });
    }

    private void saveAddress() {
        // Validate input
        String fullName = editFullName.getText().toString().trim();
        String phoneNumber = editPhoneNumber.getText().toString().trim();
        String ward = editWard.getText().toString().trim();
        String streetAddress = editStreetAddress.getText().toString().trim();
        boolean isDefault = switchSetDefault.isChecked();

        if (fullName.isEmpty() || phoneNumber.isEmpty() || selectedProvince == null ||
                ward.isEmpty() || streetAddress.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo ShippingAddress object với đúng các field
        ShippingAddress address = new ShippingAddress();
        address.setFullName(fullName);
        address.setPhoneNumber(phoneNumber);
        address.setCityProvince(selectedProvince);
        address.setDistrict(selectedDistrict != null ? selectedDistrict : "");
        address.setWard(ward);
        address.setStreetAddress(streetAddress);
        address.setIsDefault(isDefault);
        address.setAddressType(selectedAddressType); // Sử dụng loại địa chỉ đã chọn

        // Nếu đặt làm mặc định, cần vô hiệu hóa các địa chỉ mặc định khác
        if (isDefault) {
            saveAddressWithDefaultHandling(address);
        } else {
            saveAddressDirectly(address);
        }
    }

    private void saveAddressWithDefaultHandling(ShippingAddress address) {
        if (userId == null) return;

        // Tìm địa chỉ mặc định cũ
        db.collection("users").document(userId).collection("addresses")
                .whereEqualTo("isDefault", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();

                    // Vô hiệu hóa tất cả địa chỉ mặc định cũ
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (addressDocId == null || !doc.getId().equals(addressDocId)) {
                            batch.update(doc.getReference(), "isDefault", false);
                        }
                    }

                    // Lưu địa chỉ mới/cập nhật
                    if (addressDocId != null) {
                        batch.set(db.collection("users").document(userId)
                                .collection("addresses").document(addressDocId), address);
                    } else {
                        com.google.firebase.firestore.DocumentReference newDocRef =
                                db.collection("users").document(userId).collection("addresses").document();
                        batch.set(newDocRef, address);
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Lưu địa chỉ thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveAddressDirectly(ShippingAddress address) {
        if (userId == null) return;

        if (addressDocId != null) {
            // Update existing address
            db.collection("users").document(userId).collection("addresses")
                    .document(addressDocId)
                    .set(address)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật địa chỉ thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add new address
            db.collection("users").document(userId).collection("addresses")
                    .add(address)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Thêm địa chỉ thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteAddress() {
        if (addressDocId == null) {
            Toast.makeText(this, "Không thể xóa địa chỉ mới!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Confirm dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa địa chỉ")
                .setMessage("Bạn có chắc chắn muốn xóa địa chỉ này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("users").document(userId).collection("addresses")
                            .document(addressDocId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã xóa địa chỉ!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadAddressData(String docId) {
        db.collection("users").document(userId).collection("addresses")
                .document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ShippingAddress address = documentSnapshot.toObject(ShippingAddress.class);
                        if (address != null) {
                            editFullName.setText(address.getFullName());
                            editPhoneNumber.setText(address.getPhoneNumber());
                            editStreetAddress.setText(address.getStreetAddress());
                            switchSetDefault.setChecked(address.getIsDefault());

                            // Fill tỉnh/thành phố
                            if (address.getCityProvince() != null && !address.getCityProvince().isEmpty()) {
                                String province = address.getCityProvince();
                                editCityDistrict.setText(province, false);
                                selectedProvince = province;
                                setupDistrictDropdown(province);
                            }

                            // Fill quận/huyện
                            if (address.getDistrict() != null && !address.getDistrict().isEmpty()) {
                                String district = address.getDistrict();
                                editDistrict.setText(district, false);
                                selectedDistrict = district;
                            }

                            // Fill phường/xã
                            if (address.getWard() != null && !address.getWard().isEmpty()) {
                                editWard.setText(address.getWard());
                            }

                            // Fill loại địa chỉ
                            if (address.getAddressType() != null && !address.getAddressType().isEmpty()) {
                                selectedAddressType = address.getAddressType();
                            } else {
                                selectedAddressType = "Nhà Riêng"; // Default
                            }
                            updateAddressTypeUI();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}