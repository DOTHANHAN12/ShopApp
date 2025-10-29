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

        if (getIntent().getBooleanExtra(MODE_SELECT, false)) {
            isSelectionMode = true;
            initialSelectedAddressId = getIntent().getStringExtra("CURRENT_ADDRESS_ID");
        }

        mapViews();
        setupRecyclerView();
        setupNavigation();
    }

    private void setupNavigation() {
        ImageView cartButton = findViewById(R.id.ic_cart);
        if (cartButton != null) {
            cartButton.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }

        ImageView homeButton = findViewById(R.id.nav_home_cs);
        if (homeButton != null) {
            homeButton.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        }

        ImageView userButton = findViewById(R.id.nav_user_cs);
        if (userButton != null) {
            userButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        ImageView backButton = findViewById(R.id.img_back_arrow);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddresses();
    }

    private void mapViews() {
        recyclerView = findViewById(R.id.recycler_addresses);
        TextView btnAddNewAddress = findViewById(R.id.btn_add_new_address);

        btnAddNewAddress.setOnClickListener(v -> {
            Intent intent = new Intent(AddressSelectionActivity.this, EditAddressActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        addressAdapter = new AddressAdapter(this, addressList, this, initialSelectedAddressId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(addressAdapter);
    }

    private void loadAddresses() {
        if (userId == null) return;

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
                    addressAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi tải địa chỉ: " + e.getMessage()));
    }

    // --------------------------------------------------------------------------------
    // TRIỂN KHAI INTERFACE TỪ ADAPTER
    // --------------------------------------------------------------------------------

    @Override
    public void onAddressSelected(ShippingAddress selectedAddress) {
        if (isSelectionMode) {
            // CHẾ ĐỘ 1: CHỌN VÀ TRẢ VỀ CARTACTIVITY
            Gson gson = new Gson();
            Intent resultIntent = new Intent();

            resultIntent.putExtra(SELECTED_ADDRESS_JSON, gson.toJson(selectedAddress));
            setResult(RESULT_OK, resultIntent);
            finish();

        } else {
            // CHẾ ĐỘ 2: QUẢN LÝ TÀI KHOẢN (ĐẶT MẶC ĐỊNH LÂU DÀI)

            // *** ĐÃ SỬA: Gọi getIsDefault() ***
            if (selectedAddress.getIsDefault()) {
                Toast.makeText(this, "Vui lòng chọn một địa chỉ khác làm mặc định để hủy địa chỉ này.", Toast.LENGTH_LONG).show();
                return;
            }

            // Nếu chọn một địa chỉ KHÔNG phải là mặc định, thực hiện thay đổi
            setNewDefaultAddress(selectedAddress);
        }
    }

    @Override
    public void onEditClicked(ShippingAddress address) {
        Intent intent = new Intent(this, EditAddressActivity.class);
        intent.putExtra("ADDRESS_DOC_ID", address.getDocumentId());
        startActivity(intent);
    }

    /**
     * Logic sử dụng WriteBatch để đảm bảo chỉ có 1 địa chỉ mặc định (Chế độ Quản lý).
     */
    private void setNewDefaultAddress(ShippingAddress addressToSetDefault) {
        if (userId == null || addressToSetDefault.getDocumentId() == null) return;

        db.collection("users").document(userId).collection("addresses")
                .whereEqualTo("isDefault", true)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {

                        DocumentSnapshot oldDefaultDoc = null;
                        if (!task.getResult().isEmpty()) {
                            oldDefaultDoc = task.getResult().getDocuments().get(0);
                        }

                        WriteBatch batch = db.batch();

                        // A. Vô hiệu hóa địa chỉ mặc định cũ
                        if (oldDefaultDoc != null && !oldDefaultDoc.getId().equals(addressToSetDefault.getDocumentId())) {
                            batch.update(oldDefaultDoc.getReference(), "isDefault", false);
                        }

                        // B. Kích hoạt địa chỉ mới được chọn
                        batch.update(
                                db.collection("users").document(userId).collection("addresses").document(addressToSetDefault.getDocumentId()),
                                "isDefault", true
                        );

                        // 3. Thực hiện Batch Write
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đã đặt địa chỉ mặc định thành công!", Toast.LENGTH_SHORT).show();
                                    loadAddresses(); // Tải lại để cập nhật UI
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi cập nhật batch mặc định: " + e.getMessage());
                                    Toast.makeText(this, "Lỗi cập nhật địa chỉ mặc định.", Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        Log.e(TAG, "Lỗi tìm kiếm địa chỉ mặc định cũ: " + task.getException());
                    }
                });
    }
}
