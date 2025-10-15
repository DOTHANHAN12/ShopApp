package com.example.shopapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartActionListener {

    private static final String TAG = "CartActivity";
    private static final int REQUEST_CODE_SELECT_ADDRESS = 1001;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private final List<CartItem> cartItemList = new ArrayList<>();

    private TextView textCheckoutTotal;
    private String userId;

    // UI và Data cho Địa chỉ
    private TextView textShippingNamePhone;
    private TextView textShippingAddressLine;
    private TextView btnChangeAddress;
    private ShippingAddress selectedShippingAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }

        mapViews();
        setupRecyclerView();
        loadCartItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Chỉ tải địa chỉ mặc định nếu chưa có địa chỉ nào được chọn cho đơn hàng này
        if (selectedShippingAddress == null) {
            loadDefaultAddress();
        }
    }

    private void mapViews() {
        recyclerView = findViewById(R.id.recycler_cart_items);
        textCheckoutTotal = findViewById(R.id.text_checkout_total);
        ImageView imgBack = findViewById(R.id.img_back_cart);

        // Ánh xạ các thành phần Địa chỉ
        textShippingNamePhone = findViewById(R.id.text_shipping_name_phone);
        textShippingAddressLine = findViewById(R.id.text_shipping_address_line);
        btnChangeAddress = findViewById(R.id.btn_change_address);

        // Listener gọi startActivityForResult
        btnChangeAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressSelectionActivity.class);
            intent.putExtra(AddressSelectionActivity.MODE_SELECT, true);

            // TRUYỀN ID CỦA ĐỊA CHỈ ĐANG ĐƯỢC CHỌN HIỆN TẠI VÀO INTENT
            if (selectedShippingAddress != null && selectedShippingAddress.getDocumentId() != null) {
                intent.putExtra("CURRENT_ADDRESS_ID", selectedShippingAddress.getDocumentId());
            }

            startActivityForResult(intent, REQUEST_CODE_SELECT_ADDRESS);
        });

        imgBack.setOnClickListener(v -> finish());
    }

    // *** PHƯƠNG THỨC: Xử lý kết quả trả về từ AddressSelectionActivity ***
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_ADDRESS && resultCode == RESULT_OK && data != null) {
            String addressJson = data.getStringExtra(AddressSelectionActivity.SELECTED_ADDRESS_JSON);
            if (addressJson != null) {
                Gson gson = new Gson();
                ShippingAddress newSelectedAddress = gson.fromJson(addressJson, ShippingAddress.class);

                // Gán địa chỉ mới được chọn cho đơn hàng hiện tại
                this.selectedShippingAddress = newSelectedAddress;

                // Cập nhật giao diện giỏ hàng
                updateAddressUI(newSelectedAddress);

                Toast.makeText(this, "Địa chỉ giao hàng đã được cập nhật.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // **********************************************************************************

    private void setupRecyclerView() {
        adapter = new CartAdapter(this, cartItemList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadDefaultAddress() {
        if (userId == null) return;

        db.collection("users").document(userId).collection("addresses")
                .whereEqualTo("isDefault", true)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        ShippingAddress defaultAddress = document.toObject(ShippingAddress.class);
                        defaultAddress.setDocumentId(document.getId());

                        this.selectedShippingAddress = defaultAddress;
                        updateAddressUI(defaultAddress);
                    } else {
                        updateAddressUI(null);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi tải địa chỉ mặc định: " + e.getMessage()));
    }

    private void updateAddressUI(ShippingAddress address) {
        if (address != null) {
            String namePhone = String.format("%s | (+84) %s", address.getFullName(), address.getPhoneNumber());
            String addressLine = String.format("%s, %s", address.getStreetAddress(), address.getFullLocation());

            textShippingNamePhone.setText(namePhone);
            textShippingAddressLine.setText(addressLine);
        } else {
            textShippingNamePhone.setText("Vui lòng thêm địa chỉ giao hàng");
            textShippingAddressLine.setText("Chưa có địa chỉ mặc định.");
        }
    }

    private void loadCartItems() {
        if (userId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để xem giỏ hàng.", Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedShippingAddress == null) {
            loadDefaultAddress();
        }

        db.collection("users").document(userId).collection("cart")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cartItemList.clear();
                        List<Task<Void>> detailTasks = new ArrayList<>();

                        for (QueryDocumentSnapshot cartDocument : task.getResult()) {
                            CartItem item = cartDocument.toObject(CartItem.class);

                            item.setProductId(cartDocument.getString("productId"));
                            item.setVariantId(cartDocument.getString("variantId"));

                            if (item.getProductId() != null && item.getVariantId() != null) {
                                cartItemList.add(item);

                                Task<Void> detailTask = loadProductDetailForCartItem(item);
                                detailTasks.add(detailTask);
                            }
                        }

                        Tasks.whenAllComplete(detailTasks)
                                .addOnCompleteListener(allTasks -> {
                                    adapter.notifyDataSetChanged();
                                    calculateCartTotal();
                                    Log.d(TAG, "Tải giỏ hàng hoàn thành với chi tiết sản phẩm.");
                                });

                    } else {
                        Log.e(TAG, "Lỗi tải giỏ hàng: ", task.getException());
                        Toast.makeText(this, "Lỗi tải giỏ hàng.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Task<Void> loadProductDetailForCartItem(CartItem item) {
        // 1. Tải chi tiết Product
        Task<DocumentSnapshot> productTask = db.collection("products").document(item.getProductId()).get();

        return productTask.continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Product product = task.getResult().toObject(Product.class);
                item.setProductDetails(product);

                if (product != null && product.getVariants() != null && item.getVariantId() != null) {
                    // 2. Tìm chi tiết Variant từ danh sách Variants của Product
                    for (ProductVariant variant : product.getVariants()) {
                        if (variant.getVariantId() != null && variant.getVariantId().equals(item.getVariantId())) {
                            item.setVariantDetails(variant);
                            break;
                        }
                    }
                }
            } else {
                Log.w(TAG, "Không tìm thấy chi tiết Product cho ID: " + item.getProductId());
            }
            return Tasks.forResult(null);
        });
    }

    private void calculateCartTotal() {
        double total = 0;
        for (CartItem item : cartItemList) {
            total += item.getPriceAtTimeOfAdd() * item.getQuantity();
        }
        updateCartTotal(total);
    }

    private void updateCartTotal(double total) {
        textCheckoutTotal.setText(String.format(Locale.getDefault(), "%,.0f VND", total));
    }

    @Override
    public void onCartUpdated() {
        calculateCartTotal();
    }

    @Override
    public void onItemDeleted(CartItem item) {
        if (userId == null) return;

        String documentId = item.getProductId() + "_" + item.getVariantId();

        db.collection("users").document(userId).collection("cart").document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa sản phẩm khỏi giỏ hàng.", Toast.LENGTH_SHORT).show();
                    cartItemList.remove(item);
                    adapter.notifyDataSetChanged();
                    calculateCartTotal();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xóa item.", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        if (userId == null) return;

        String documentId = item.getProductId() + "_" + item.getVariantId();

        db.collection("users").document(userId).collection("cart").document(documentId)
                .update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cập nhật số lượng thành công.");
                    Toast.makeText(this, "Cập nhật số lượng thành công.", Toast.LENGTH_SHORT).show();

                    item.setQuantity(newQuantity);
                    adapter.notifyDataSetChanged();
                    calculateCartTotal();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật số lượng.", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                });
    }
}