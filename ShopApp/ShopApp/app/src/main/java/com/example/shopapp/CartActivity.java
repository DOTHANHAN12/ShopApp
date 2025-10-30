package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartActionListener {

    private static final String TAG = "CartActivity";
    private static final int REQUEST_CODE_SELECT_ADDRESS = 1001;
    private static final int REQUEST_CODE_SELECT_VOUCHER = 1002;
    private static final int REQUEST_CODE_VNPAY = 1003;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private final List<CartItem> cartItemList = new ArrayList<>();

    // UI Elements
    private TextView textCheckoutTotal, textSubtotal, textVoucherDiscount, textVoucherAppliedInfo;
    private LinearLayout layoutVoucherSelector;
    private Button btnCheckout;
    private TextView textShippingNamePhone, textShippingAddressLine, btnChangeAddress;
    private RadioGroup radioGroupPayment;
    private LinearLayout layoutShippingAddress; // ✅ THÊM BIẾN NÀY

    // State
    private String appliedVoucherCode = null;
    private double voucherDiscountValue = 0.0;
    private String userId;
    private ShippingAddress selectedShippingAddress;
    private String currentTxnRef; // Store the current transaction reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

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
        textSubtotal = findViewById(R.id.text_subtotal);
        textVoucherDiscount = findViewById(R.id.text_voucher_discount);
        textVoucherAppliedInfo = findViewById(R.id.text_voucher_info_line);
        layoutVoucherSelector = findViewById(R.id.layout_voucher_selector);
        btnCheckout = findViewById(R.id.btn_checkout);
        textShippingNamePhone = findViewById(R.id.text_shipping_name_phone);
        textShippingAddressLine = findViewById(R.id.text_shipping_address_line);
        btnChangeAddress = findViewById(R.id.btn_change_address);
        radioGroupPayment = findViewById(R.id.radio_group_payment);
        layoutShippingAddress = findViewById(R.id.layout_shipping_address); // ✅ THÊM DÒNG NÀY

        btnCheckout.setOnClickListener(v -> handleCheckout());

        // ✅ THAY ĐỔI: Set listener cho cả LinearLayout thay vì chỉ TextView
        layoutShippingAddress.setOnClickListener(v -> openAddressSelection());

        // ✅ GIỮ NGUYÊN listener cho TextView (để đảm bảo tương thích)
        btnChangeAddress.setOnClickListener(v -> openAddressSelection());

        layoutVoucherSelector.setOnClickListener(v -> openVoucherSelection());
        imgBack.setOnClickListener(v -> finish());
    }

    // ✅ THÊM METHOD MỚI để tránh lặp code
    private void openAddressSelection() {
        Intent intent = new Intent(this, AddressSelectionActivity.class);
        intent.putExtra(AddressSelectionActivity.MODE_SELECT, true);
        if (selectedShippingAddress != null && selectedShippingAddress.getDocumentId() != null) {
            intent.putExtra("CURRENT_ADDRESS_ID", selectedShippingAddress.getDocumentId());
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_ADDRESS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_ADDRESS && resultCode == RESULT_OK && data != null) {
            String addressJson = data.getStringExtra(AddressSelectionActivity.SELECTED_ADDRESS_JSON);
            if (addressJson != null) {
                selectedShippingAddress = new Gson().fromJson(addressJson, ShippingAddress.class);
                updateAddressUI(selectedShippingAddress);
                Toast.makeText(this, "Địa chỉ giao hàng đã được cập nhật.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_CODE_SELECT_VOUCHER && resultCode == RESULT_OK && data != null) {
            appliedVoucherCode = data.getStringExtra("SELECTED_VOUCHER_CODE");
            if (appliedVoucherCode == null || appliedVoucherCode.isEmpty()) {
                voucherDiscountValue = 0.0;
            }
            calculateCartTotal();
        }
        if (requestCode == REQUEST_CODE_VNPAY && data != null) {
            String status = data.getStringExtra("status");

            // Lấy đối tượng Order được gửi cùng Intent
            Order order = (Order) data.getSerializableExtra("order");

            if (order != null) {
                if ("success".equals(status) && resultCode == RESULT_OK) {
                    // ✅ FIXED: VNPAY thành công -> Đặt trạng thái PAID và lưu
                    order.setOrderStatus("PAID");
                    saveOrderToFirebase(order, true);
                } else {
                    // VNPAY thất bại
                    order.setOrderStatus("FAILED_PAYMENT");
                    saveOrderToFirebase(order, false); // Không xóa giỏ hàng nếu thanh toán thất bại
                    Toast.makeText(this, "Thanh toán VNPAY thất bại. Đơn hàng được lưu nháp.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Thanh toán VNPAY không hoàn thành hoặc dữ liệu đơn hàng bị mất.", Toast.LENGTH_LONG).show();
            }
        }
    }

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
                        if (defaultAddress != null) {
                            defaultAddress.setDocumentId(document.getId());
                            this.selectedShippingAddress = defaultAddress;
                            updateAddressUI(defaultAddress);
                        }
                    } else {
                        updateAddressUI(null);
                    }
                }).addOnFailureListener(e -> Log.e(TAG, "Lỗi tải địa chỉ mặc định: " + e.getMessage()));
    }

    private void updateAddressUI(ShippingAddress address) {
        if (address != null) {
            String namePhone = String.format("%s | (+84) %s", address.getFullName(), address.getPhoneNumber());
            // Sử dụng method getFullLocation() để lấy địa chỉ đầy đủ
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
                            if (item.getProductId() != null && item.getVariantId() != null) {
                                cartItemList.add(item);
                                detailTasks.add(loadProductDetailForCartItem(item));
                            }
                        }
                        Tasks.whenAllComplete(detailTasks).addOnCompleteListener(allTasks -> {
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

    private void calculateCartTotal() {
        double subtotal = cartItemList.stream().mapToDouble(item -> item.getPriceAtTimeOfAdd() * item.getQuantity()).sum();
        fetchAndApplyVoucher(subtotal);
    }

    private void fetchAndApplyVoucher(double subtotal) {
        if (appliedVoucherCode == null || subtotal <= 0) {
            voucherDiscountValue = 0.0;
            updateCartTotalUI(subtotal, voucherDiscountValue, subtotal);
            return;
        }

        db.collection("vouchers").whereEqualTo("code", appliedVoucherCode).limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    voucherDiscountValue = 0.0;
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        Voucher voucher = querySnapshot.getDocuments().get(0).toObject(Voucher.class);
                        Date now = new Date();
                        boolean isValid = voucher.getStartDate() != null && voucher.getEndDate() != null && !voucher.getStartDate().after(now) && !voucher.getEndDate().before(now) && voucher.getTimesUsed() < voucher.getMaxUsageLimit() && subtotal >= voucher.getMinOrderValue();
                        if (isValid) {
                            double discount = "PERCENT".equals(voucher.getDiscountType()) ? subtotal * (voucher.getDiscountValue() / 100.0) : voucher.getDiscountValue();
                            voucherDiscountValue = Math.min(discount, subtotal);
                            Toast.makeText(CartActivity.this, "Đã áp dụng mã " + appliedVoucherCode, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Mã " + appliedVoucherCode + " không hợp lệ/đủ điều kiện.", Toast.LENGTH_LONG).show();
                            appliedVoucherCode = null;
                        }
                    } else {
                        Toast.makeText(this, "Mã voucher không tồn tại.", Toast.LENGTH_LONG).show();
                        appliedVoucherCode = null;
                    }
                    double finalTotal = Math.max(0, subtotal - voucherDiscountValue);
                    updateCartTotalUI(subtotal, voucherDiscountValue, finalTotal);
                }).addOnFailureListener(e -> {
                    voucherDiscountValue = 0.0;
                    double finalTotal = subtotal - voucherDiscountValue;
                    updateCartTotalUI(subtotal, voucherDiscountValue, finalTotal);
                    Toast.makeText(this, "Lỗi kết nối khi kiểm tra voucher.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateCartTotalUI(double subtotal, double discount, double total) {
        textSubtotal.setText(String.format(Locale.getDefault(), "%,.0f VND", subtotal));
        textVoucherDiscount.setText(String.format(Locale.getDefault(), "- %,.0f VND", discount));
        textCheckoutTotal.setText(String.format(Locale.getDefault(), "%,.0f VND", total));

        if (appliedVoucherCode != null && discount > 0) {
            String infoText = (appliedVoucherCode.contains("SHIP") || appliedVoucherCode.contains("FREE")) ? "Miễn Phí Vận Chuyển" : "Đã áp dụng mã";
            textVoucherAppliedInfo.setText(infoText);
        } else {
            textVoucherAppliedInfo.setText("Chọn Voucher >");
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
        db.collection("users").document(userId).collection("cart").document(documentId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa sản phẩm khỏi giỏ hàng.", Toast.LENGTH_SHORT).show();
                    cartItemList.remove(item);
                    adapter.notifyDataSetChanged();
                    calculateCartTotal();
                }).addOnFailureListener(e -> Toast.makeText(this, "Lỗi xóa item.", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        if (userId == null) return;
        String documentId = item.getProductId() + "_" + item.getVariantId();
        db.collection("users").document(userId).collection("cart").document(documentId).update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> {
                    item.setQuantity(newQuantity);
                    adapter.notifyDataSetChanged();
                    calculateCartTotal();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật số lượng.", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                });
    }

    private void handleCheckout() {
        if (cartItemList.isEmpty() || selectedShippingAddress == null) {
            Toast.makeText(this, "Vui lòng điền đủ thông tin giỏ hàng và địa chỉ.", Toast.LENGTH_SHORT).show();
            return;
        }

        double subtotal = cartItemList.stream().mapToDouble(item -> item.getPriceAtTimeOfAdd() * item.getQuantity()).sum();
        double total = Math.max(0, subtotal - voucherDiscountValue);

        Map<String, String> addressMap = new HashMap<>();
        addressMap.put("fullName", selectedShippingAddress.getFullName());
        addressMap.put("phoneNumber", selectedShippingAddress.getPhoneNumber());
        addressMap.put("fullLocation", selectedShippingAddress.getFullLocation());
        addressMap.put("streetAddress", selectedShippingAddress.getStreetAddress());

        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (CartItem item : cartItemList) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productId", item.getProductId());
            itemMap.put("variantId", item.getVariantId());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("price", item.getPriceAtTimeOfAdd());
            itemMap.put("name", item.getProductDetails() != null ? item.getProductDetails().getName() : "Unknown");
            itemMap.put("variant", item.getVariantDetails() != null ? item.getVariantDetails().getColor() + "/" + item.getVariantDetails().getSize() : "N/A");
            itemsList.add(itemMap);
        }

        // Cần xác định trạng thái và phương thức thanh toán trước khi tạo Order

        int selectedPaymentId = radioGroupPayment.getCheckedRadioButtonId();
        String paymentMethod;
        String initialStatus;
        Order finalOrder;

        if (selectedPaymentId == R.id.radio_vnpay) {
            paymentMethod = "VNPAY";
            initialStatus = "PENDING"; // Chờ thanh toán từ VNPAY
            finalOrder = new Order(userId, total, subtotal, voucherDiscountValue, appliedVoucherCode, addressMap, itemsList, paymentMethod, initialStatus);
            startVNPayPayment(finalOrder);
        } else { // Default to COD
            paymentMethod = "COD";
            initialStatus = "PENDING"; // Chờ xác nhận từ Admin/Kho hàng
            finalOrder = new Order(userId, total, subtotal, voucherDiscountValue, appliedVoucherCode, addressMap, itemsList, paymentMethod, initialStatus);
            saveOrderToFirebase(finalOrder, true);
        }
    }

    private void startVNPayPayment(Order order) {
        currentTxnRef = "ORDER_" + System.currentTimeMillis();
        long amount = (long) order.getTotalAmount() * 100;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VNPayConfig.vnp_Version);
        vnp_Params.put("vnp_Command", VNPayConfig.vnp_Command);
        vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", currentTxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + currentTxnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", "127.0.0.1"); // Thêm địa chỉ IP

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        String paymentUrl = VNPayConfig.getPaymentUrl(vnp_Params);

        Intent intent = new Intent(this, VNPayActivity.class);
        intent.putExtra("payment_url", paymentUrl);
        intent.putExtra("order", order);
        startActivityForResult(intent, REQUEST_CODE_VNPAY);
    }

    private void saveOrderToFirebase(Order order, boolean clearCart) {
        if (userId == null) return;
        db.collection("orders").add(order).addOnSuccessListener(documentReference -> {
            String newOrderId = documentReference.getId();
            Log.i(TAG, "Order saved successfully. ID: " + newOrderId);
            if (clearCart) {
                deleteUserCartAndGoToSuccess(newOrderId);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "ERROR saving order: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi hệ thống khi đặt hàng.", Toast.LENGTH_LONG).show();
        });
    }

    private void deleteUserCartAndGoToSuccess(String orderId) {
        if (userId == null) return;
        db.collection("users").document(userId).collection("cart").get().addOnSuccessListener(querySnapshot -> {
            List<Task<Void>> deleteTasks = new ArrayList<>();
            for (QueryDocumentSnapshot doc : querySnapshot) {
                deleteTasks.add(doc.getReference().delete());
            }
            Tasks.whenAllComplete(deleteTasks).addOnCompleteListener(task -> {
                Intent successIntent = new Intent(this, OrderSuccessActivity.class);
                successIntent.putExtra("ORDER_ID", orderId);
                startActivity(successIntent);
                finish();
            });
        });
    }

    private void openVoucherSelection() {
        Intent intent = new Intent(this, VoucherSelectionActivity.class);

        double subtotal = 0;
        for (CartItem item : cartItemList) {
            subtotal += item.getPriceAtTimeOfAdd() * item.getQuantity();
        }
        intent.putExtra("CURRENT_SUBTOTAL", subtotal);

        startActivityForResult(intent, REQUEST_CODE_SELECT_VOUCHER);
    }
}