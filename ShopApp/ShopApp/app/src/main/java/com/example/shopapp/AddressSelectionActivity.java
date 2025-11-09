package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class AddressSelectionActivity extends AppCompatActivity implements AddressAdapter.OnAddressActionListener {

    private static final String TAG = "AddressSelectionActivity";
    public static final String MODE_SELECT = "mode_select";
    public static final String SELECTED_ADDRESS_JSON = "selected_address_json";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private AddressAdapter addressAdapter;
    private final List<ShippingAddress> addressList = new ArrayList<>();

    private String userId;
    private boolean isSelectionMode = false;
    private String initialSelectedAddressId = null; // ID địa chỉ đang được chọn từ Cart

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_selection);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        } else {
            Toast.makeText(this, "Bạn cần đăng nhập.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ✅ THÊM LOG ĐỂ DEBUG
        isSelectionMode = getIntent().getBooleanExtra(MODE_SELECT, false);
        initialSelectedAddressId = getIntent().getStringExtra("CURRENT_ADDRESS_ID");

        Log.d(TAG, "onCreate - isSelectionMode: " + isSelectionMode);
        Log.d(TAG, "onCreate - initialSelectedAddressId: " + initialSelectedAddressId);

        mapViews();
        setupRecyclerView();
        setupNavigation();
    }

    private void setupNavigation() {
        NavigationHelper navigationHelper = new NavigationHelper(this);
        
        // ✅ SỬA: Ẩn header và footer khi ở chế độ chọn địa chỉ
        if (isSelectionMode) {
            navigationHelper.hideHeaderAndFooter();
        } else {
            // Chế độ quản lý thông thường - setup navigation
            navigationHelper.setupNavigation();
        }

        // Nút back luôn hoạt động với logic đặc biệt
        ImageView backButton = findViewById(R.id.img_back_arrow);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked - isSelectionMode: " + isSelectionMode);
                // ✅ SỬA: Nếu đang ở chế độ chọn, trả về RESULT_CANCELED
                if (isSelectionMode) {
                    setResult(RESULT_CANCELED);
                }
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        loadAddresses();
    }

    private void mapViews() {
        recyclerView = findViewById(R.id.recycler_addresses);
        // ✅ SỬA: Đúng kiểu CardView như trong XML
        androidx.cardview.widget.CardView btnAddNewAddress = findViewById(R.id.btn_add_new_address);

        if (btnAddNewAddress != null) {
            btnAddNewAddress.setOnClickListener(v -> {
                Log.d(TAG, "Add new address button clicked");
                Intent intent = new Intent(AddressSelectionActivity.this, EditAddressActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupRecyclerView() {
        addressAdapter = new AddressAdapter(this, addressList, this, initialSelectedAddressId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(addressAdapter);
    }

    private void loadAddresses() {
        if (userId == null) return;

        Log.d(TAG, "Loading addresses for user: " + userId);

        db.collection("users").document(userId).collection("addresses")
                .orderBy("isDefault", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    addressList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ShippingAddress address = doc.toObject(ShippingAddress.class);
                        if (address != null) {
                            address.setDocumentId(doc.getId());
                            addressList.add(address);
                        }
                    }
                    Log.d(TAG, "Loaded " + addressList.size() + " addresses");
                    addressAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải địa chỉ: " + e.getMessage(), e);
                    Toast.makeText(this, "Lỗi tải danh sách địa chỉ", Toast.LENGTH_SHORT).show();
                });
    }

    // --------------------------------------------------------------------------------
    // TRIỂN KHAI INTERFACE TỪ ADAPTER
    // --------------------------------------------------------------------------------

    @Override
    public void onAddressSelected(ShippingAddress selectedAddress) {
        Log.d(TAG, "onAddressSelected called - isSelectionMode: " + isSelectionMode);
        Log.d(TAG, "Selected address: " + (selectedAddress != null ? selectedAddress.getFullName() : "null"));

        if (isSelectionMode) {
            // CHẾ ĐỘ 1: CHỌN VÀ TRẢ VỀ CARTACTIVITY
            Gson gson = new Gson();
            Intent resultIntent = new Intent();

            String addressJson = gson.toJson(selectedAddress);
            Log.d(TAG, "Returning address JSON: " + addressJson);

            resultIntent.putExtra(SELECTED_ADDRESS_JSON, addressJson);
            setResult(RESULT_OK, resultIntent);

            Log.d(TAG, "Finishing activity with RESULT_OK");
            finish();

        } else {
            // CHẾ ĐỘ 2: QUẢN LÝ TÀI KHOẢN (ĐẶT MẶC ĐỊNH LÂU DÀI)
            Log.d(TAG, "Management mode - setting default address");

            if (Boolean.TRUE.equals(selectedAddress.getIsDefault())) {
                Toast.makeText(this, "Địa chỉ này đã là mặc định.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Nếu chọn một địa chỉ KHÔNG phải là mặc định, thực hiện thay đổi
            setNewDefaultAddress(selectedAddress);
        }
    }

    @Override
    public void onEditClicked(ShippingAddress address) {
        Log.d(TAG, "onEditClicked - Address: " + address.getDocumentId());
        Intent intent = new Intent(this, EditAddressActivity.class);
        intent.putExtra("ADDRESS_DOC_ID", address.getDocumentId());
        startActivity(intent);
    }

    /**
     * Logic sử dụng WriteBatch để đảm bảo chỉ có 1 địa chỉ mặc định (Chế độ Quản lý).
     */
    private void setNewDefaultAddress(ShippingAddress addressToSetDefault) {
        if (userId == null || addressToSetDefault.getDocumentId() == null) {
            Log.w(TAG, "Cannot set default - userId or documentId is null");
            return;
        }

        Log.d(TAG, "Setting new default address: " + addressToSetDefault.getDocumentId());

        db.collection("users").document(userId).collection("addresses")
                .whereEqualTo("isDefault", true)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {

                        DocumentSnapshot oldDefaultDoc = null;
                        if (!task.getResult().isEmpty()) {
                            oldDefaultDoc = task.getResult().getDocuments().get(0);
                            Log.d(TAG, "Found old default address: " + oldDefaultDoc.getId());
                        }

                        WriteBatch batch = db.batch();

                        // A. Vô hiệu hóa địa chỉ mặc định cũ
                        if (oldDefaultDoc != null && !oldDefaultDoc.getId().equals(addressToSetDefault.getDocumentId())) {
                            batch.update(oldDefaultDoc.getReference(), "isDefault", false);
                            Log.d(TAG, "Disabling old default: " + oldDefaultDoc.getId());
                        }

                        // B. Kích hoạt địa chỉ mới được chọn
                        batch.update(
                                db.collection("users").document(userId).collection("addresses").document(addressToSetDefault.getDocumentId()),
                                "isDefault", true
                        );
                        Log.d(TAG, "Enabling new default: " + addressToSetDefault.getDocumentId());

                        // 3. Thực hiện Batch Write
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Default address updated successfully");
                                    Toast.makeText(this, "Đã đặt địa chỉ mặc định thành công!", Toast.LENGTH_SHORT).show();
                                    loadAddresses(); // Tải lại để cập nhật UI
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi cập nhật batch mặc định: " + e.getMessage(), e);
                                    Toast.makeText(this, "Lỗi cập nhật địa chỉ mặc định.", Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        Log.e(TAG, "Lỗi tìm kiếm địa chỉ mặc định cũ", task.getException());
                    }
                });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed called - isSelectionMode: " + isSelectionMode);
        if (isSelectionMode) {
            setResult(RESULT_CANCELED);
        }
        super.onBackPressed();
    }
}