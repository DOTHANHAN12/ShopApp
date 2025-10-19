package com.example.shopapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartActionListener {

    private static final String TAG = "CartActivity";
    private static final int REQUEST_CODE_SELECT_ADDRESS = 1001;
    private static final int REQUEST_CODE_SELECT_VOUCHER = 1002;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private final List<CartItem> cartItemList = new ArrayList<>();

    // UI và Data cho Tổng tiền & Voucher
    private TextView textCheckoutTotal;
    private TextView textSubtotal;
    private TextView textVoucherDiscount;
    private TextView textVoucherAppliedInfo; // Dòng hiển thị "Miễn Phí Vận Chuyển"
    private LinearLayout layoutVoucherSelector; // Layout cho vùng click chọn voucher

    // Trạng thái Voucher hiện tại
    private String appliedVoucherCode = null;
    private double voucherDiscountValue = 0.0;

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
        if (selectedShippingAddress == null) {
            loadDefaultAddress();
        }
    }

    private void mapViews() {
        recyclerView = findViewById(R.id.recycler_cart_items);
        textCheckoutTotal = findViewById(R.id.text_checkout_total);
        ImageView imgBack = findViewById(R.id.img_back_cart);

        // ÁNH XẠ VOUCHER VÀ SUBTOTAL
        textSubtotal = findViewById(R.id.text_subtotal);
        textVoucherDiscount = findViewById(R.id.text_voucher_discount);

        // Ánh xạ các View MỚI theo mẫu Shopee
        textVoucherAppliedInfo = findViewById(R.id.text_voucher_info_line);
        layoutVoucherSelector = findViewById(R.id.layout_voucher_selector);

        // Ánh xạ các thành phần Địa chỉ
        textShippingNamePhone = findViewById(R.id.text_shipping_name_phone);
        textShippingAddressLine = findViewById(R.id.text_shipping_address_line);
        btnChangeAddress = findViewById(R.id.btn_change_address);

        // Listener cho Địa chỉ
        btnChangeAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressSelectionActivity.class);
            intent.putExtra(AddressSelectionActivity.MODE_SELECT, true);

            if (selectedShippingAddress != null && selectedShippingAddress.getDocumentId() != null) {
                intent.putExtra("CURRENT_ADDRESS_ID", selectedShippingAddress.getDocumentId());
            }

            startActivityForResult(intent, REQUEST_CODE_SELECT_ADDRESS);
        });

        // Listener cho vùng chọn Voucher (click vào layout_voucher_selector)
        layoutVoucherSelector.setOnClickListener(v -> openVoucherSelection());

        imgBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 1. Xử lý kết quả từ Chọn Địa chỉ
        if (requestCode == REQUEST_CODE_SELECT_ADDRESS && resultCode == RESULT_OK && data != null) {
            String addressJson = data.getStringExtra(AddressSelectionActivity.SELECTED_ADDRESS_JSON);
            if (addressJson != null) {
                Gson gson = new Gson();
                ShippingAddress newSelectedAddress = gson.fromJson(addressJson, ShippingAddress.class);

                this.selectedShippingAddress = newSelectedAddress;
                updateAddressUI(newSelectedAddress);

                Toast.makeText(this, "Địa chỉ giao hàng đã được cập nhật.", Toast.LENGTH_SHORT).show();
            }
        }

        // 2. XỬ LÝ KẾT QUẢ TỪ CHỌN VOUCHER
        if (requestCode == REQUEST_CODE_SELECT_VOUCHER && resultCode == RESULT_OK && data != null) {
            String selectedCode = data.getStringExtra("SELECTED_VOUCHER_CODE");

            if (selectedCode != null && !selectedCode.isEmpty()) {
                appliedVoucherCode = selectedCode;
            } else {
                appliedVoucherCode = null;
                voucherDiscountValue = 0.0;
            }

            calculateCartTotal();
            // Toast sẽ được xử lý trong fetchAndApplyVoucher
        }
    }

    // *** KHẮC PHỤC LỖI CANNOT RESOLVE METHOD 'setupRecyclerView' ***
    private void setupRecyclerView() {
        adapter = new CartAdapter(this, cartItemList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadDefaultAddress() {
        if (userId == null) return;
        // Logic tải địa chỉ giữ nguyên
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
                                detailTasks.add(loadProductDetailForCartItem(item));
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

    // *** GIẢ ĐỊNH CÓ PRODUCT VÀ VARIANTS CLASS ***
    private Task<Void> loadProductDetailForCartItem(CartItem item) {
        // 1. Tải chi tiết Product
        Task<DocumentSnapshot> productTask = db.collection("products").document(item.getProductId()).get();

        return productTask.continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Product product = task.getResult().toObject(Product.class);
                item.setProductDetails(product);

                if (product != null && product.getVariants() != null && item.getVariantId() != null) {
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
    // ********************************************

    private void calculateCartTotal() {
        // 1. TÍNH TỔNG PHỤ (SUBTOTAL)
        double subtotal = 0;
        for (CartItem item : cartItemList) {
            subtotal += item.getPriceAtTimeOfAdd() * item.getQuantity();
        }

        // 2. GỌI HÀM BẤT ĐỒNG BỘ ĐỂ TẢI VOUCHER VÀ TÍNH TỔNG CUỐI CÙNG
        fetchAndApplyVoucher(subtotal);
    }

    private void fetchAndApplyVoucher(double subtotal) {
        if (appliedVoucherCode == null || subtotal <= 0) {
            voucherDiscountValue = 0.0;
            updateCartTotalUI(subtotal, voucherDiscountValue, subtotal);
            return;
        }

        db.collection("vouchers")
                .whereEqualTo("code", appliedVoucherCode)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    voucherDiscountValue = 0.0;

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        Voucher voucher = querySnapshot.getDocuments().get(0).toObject(Voucher.class);

                        // LOGIC KIỂM TRA ĐIỀU KIỆN
                        Date now = new Date();
                        boolean isValid = true;

                        if (voucher.getStartDate() == null || voucher.getEndDate() == null || voucher.getStartDate().after(now) || voucher.getEndDate().before(now)) {
                            isValid = false;
                        } else if (voucher.getTimesUsed() >= voucher.getMaxUsageLimit()) {
                            isValid = false;
                        } else if (subtotal < voucher.getMinOrderValue()) {
                            isValid = false;
                        }

                        if (isValid) {
                            // TÍNH TOÁN GIẢM GIÁ
                            double discount = 0;
                            if ("PERCENT".equals(voucher.getDiscountType())) {
                                discount = subtotal * (voucher.getDiscountValue() / 100.0);
                            } else if ("FIXED_AMOUNT".equals(voucher.getDiscountType())) {
                                discount = voucher.getDiscountValue();
                            }

                            voucherDiscountValue = Math.min(discount, subtotal);
                            Toast.makeText(CartActivity.this, "Đã áp dụng mã " + appliedVoucherCode, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Voucher không hợp lệ sau khi kiểm tra lại.");
                            Toast.makeText(this, "Mã " + appliedVoucherCode + " không hợp lệ/đủ điều kiện.", Toast.LENGTH_LONG).show();
                            appliedVoucherCode = null; // Hủy áp dụng
                        }
                    } else {
                        Log.d(TAG, "Không tìm thấy mã voucher trong DB.");
                        Toast.makeText(this, "Mã voucher không tồn tại.", Toast.LENGTH_LONG).show();
                        appliedVoucherCode = null;
                    }

                    // CẬP NHẬT GIAO DIỆN SAU KHI TÍNH TOÁN
                    double finalTotal = subtotal - voucherDiscountValue;
                    if (finalTotal < 0) finalTotal = 0;
                    updateCartTotalUI(subtotal, voucherDiscountValue, finalTotal);

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tải voucher: " + e.getMessage());
                    voucherDiscountValue = 0.0;
                    double finalTotal = subtotal - voucherDiscountValue;
                    updateCartTotalUI(subtotal, voucherDiscountValue, finalTotal);
                    Toast.makeText(this, "Lỗi kết nối khi kiểm tra voucher.", Toast.LENGTH_SHORT).show();
                });
    }

    // *** CẬP NHẬT UI ĐỂ PHẢN ÁNH TÔNG MÀU ĐỎ VÀ THÔNG TIN VOUCHER ***
    private void updateCartTotalUI(double subtotal, double discount, double total) {
        textSubtotal.setText(String.format(Locale.getDefault(), "%,.0f VND", subtotal));

        // 1. Dòng Giảm giá Voucher (Màu Đỏ)
        if (discount > 0) {
            textVoucherDiscount.setText(String.format(Locale.getDefault(), "- %,.0f VND", discount));
            textVoucherDiscount.setTextColor(getResources().getColor(R.color.colorPrimary));
        } else {
            textVoucherDiscount.setText("0 VND");
            textVoucherDiscount.setTextColor(getResources().getColor(R.color.grey_dark));
        }

        // 2. Tổng Thanh toán (Màu Đỏ)
        textCheckoutTotal.setText(String.format(Locale.getDefault(), "%,.0f VND", total));
        textCheckoutTotal.setTextColor(getResources().getColor(R.color.colorPrimary));

        // 3. Dòng thông tin Voucher (Theo mẫu ảnh - Màu Xanh Shopee)
        if (appliedVoucherCode != null && discount > 0) {
            String infoText = (appliedVoucherCode.contains("SHIP") || appliedVoucherCode.contains("FREE")) ? "Miễn Phí Vận Chuyển" : "Đã áp dụng mã";
            textVoucherAppliedInfo.setText(infoText);
            textVoucherAppliedInfo.setTextColor(getResources().getColor(R.color.shopee_green));
            textVoucherAppliedInfo.setBackgroundResource(R.drawable.bg_rounded_shopee_green_border);
        } else {
            textVoucherAppliedInfo.setText("Chọn Voucher >");
            textVoucherAppliedInfo.setTextColor(getResources().getColor(R.color.grey_dark));
            textVoucherAppliedInfo.setBackground(null); // Xóa background nếu không áp dụng
        }
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

    // PHƯƠNG THỨC XỬ LÝ VOUCHER
    private void openVoucherSelection() {
        Intent intent = new Intent(this, VoucherSelectionActivity.class);

        // Truyền subtotal để Activity VoucherSelection có thể kiểm tra điều kiện ngay lập tức
        double subtotal = 0;
        for (CartItem item : cartItemList) {
            subtotal += item.getPriceAtTimeOfAdd() * item.getQuantity();
        }
        intent.putExtra("CURRENT_SUBTOTAL", subtotal);

        startActivityForResult(intent, REQUEST_CODE_SELECT_VOUCHER);
    }
}