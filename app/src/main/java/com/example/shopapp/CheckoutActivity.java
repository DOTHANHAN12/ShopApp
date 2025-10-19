package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private static final String TAG = "CheckoutActivity";
    private Order orderData;
    private String selectedPaymentMethod = "COD"; // Mặc định là COD

    // UI Elements
    private TextView textOrderTotal, textOrderItemsCount, textShippingSummary;
    private TextView textFooterTotal;
    private Button btnPlaceOrder;
    private RelativeLayout layoutPaymentCod, layoutPaymentVnPay;
    private RadioButton radioCod, radioVnPay;

    // Consts cho Payment
    private static final String PAYMENT_COD = "COD";
    private static final String PAYMENT_VNPAY = "VNPAY";
    private static final int REQUEST_CODE_VNPAY_WEBVIEW = 2001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mapViews();
        loadOrderData(getIntent());
        setupListeners();
    }

    private void mapViews() {
        // Ánh xạ Headers và Buttons
        ImageView imgBack = findViewById(R.id.img_back_checkout);
        imgBack.setOnClickListener(v -> finish());

        // Ánh xạ Tóm tắt đơn hàng
        textOrderTotal = findViewById(R.id.text_order_total);
        textOrderItemsCount = findViewById(R.id.text_order_items_count);
        textShippingSummary = findViewById(R.id.text_shipping_summary);

        // Ánh xạ Options Thanh toán
        layoutPaymentCod = findViewById(R.id.layout_payment_cod);
        layoutPaymentVnPay = findViewById(R.id.layout_payment_vnpay);
        radioCod = findViewById(R.id.radio_cod);
        radioVnPay = findViewById(R.id.radio_vnpay);

        // Footer
        textFooterTotal = findViewById(R.id.text_footer_total);
        btnPlaceOrder = findViewById(R.id.btn_place_order);
    }

    private void loadOrderData(Intent intent) {
        String orderJson = intent.getStringExtra("ORDER_DATA_JSON");
        if (orderJson == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy dữ liệu đơn hàng.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            orderData = new Gson().fromJson(orderJson, Order.class);
            updateUI();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi parse Order JSON: " + e.getMessage());
            Toast.makeText(this, "Lỗi định dạng dữ liệu.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void updateUI() {
        if (orderData == null) return;

        // Cập nhật Tóm tắt đơn hàng
        String totalFormatted = String.format(Locale.getDefault(), "%,.0f VND", orderData.getTotalAmount());
        textOrderTotal.setText("Tổng tiền: " + totalFormatted);
        textFooterTotal.setText("TỔNG CỘNG: " + totalFormatted);

        // Cập nhật số lượng item
        textOrderItemsCount.setText("Số lượng sản phẩm: " + orderData.getItems().size());

        // Cập nhật Địa chỉ
        String summaryAddress = orderData.getShippingAddress().get("fullName") + " - " + orderData.getShippingAddress().get("streetAddress");
        textShippingSummary.setText("Giao đến: " + summaryAddress);

        // Thiết lập COD là mặc định
        updatePaymentSelection(PAYMENT_COD);
    }

    private void setupListeners() {
        layoutPaymentCod.setOnClickListener(v -> updatePaymentSelection(PAYMENT_COD));
        layoutPaymentVnPay.setOnClickListener(v -> updatePaymentSelection(PAYMENT_VNPAY));

        btnPlaceOrder.setOnClickListener(v -> handlePlaceOrder());
    }

    private void updatePaymentSelection(String method) {
        selectedPaymentMethod = method;
        radioCod.setChecked(method.equals(PAYMENT_COD));
        radioVnPay.setChecked(method.equals(PAYMENT_VNPAY));
    }

    // *** LOGIC XỬ LÝ ĐẶT HÀNG CUỐI CÙNG ***
    private void handlePlaceOrder() {
        if (orderData == null) return;

        orderData.setPaymentMethod(selectedPaymentMethod);

        if (selectedPaymentMethod.equals(PAYMENT_COD)) {
            // 1. Xử lý COD: Lưu đơn hàng vào Firebase
            saveOrderToFirebase(orderData);

        } else if (selectedPaymentMethod.equals(PAYMENT_VNPAY)) {
            // 2. Xử lý VNPAY: Gọi Backend để tạo giao dịch
            callBackendForVnPayUrl(orderData);

        } else {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán.", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm giả lập (MOCK) gọi API Backend để lấy URL VNPay
    private void callBackendForVnPayUrl(Order order) {
        // TẠI ĐÂY: Bạn cần gửi dữ liệu order lên Backend Server (Node.js/Python/Java)
        // Backend sẽ:
        // 1. Tạo orderId (hoặc transactionId)
        // 2. Tạo chữ ký bảo mật (checksum)
        // 3. Gọi API VNPay (thường là /payment/create)
        // 4. Trả về paymentUrl (URL cổng thanh toán VNPay)

        Toast.makeText(this, "Đang kết nối cổng VNPay...", Toast.LENGTH_SHORT).show();

        // *** GIẢ LẬP LẤY URL VNPAY ***
        // String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=10000000..."; // URL thật

        // Giả lập thành công: Chuyển sang WebView
        String mockPaymentUrl = "https://mock.vnpay.checkout.url?amount=" + order.getTotalAmount();

        // Khởi động WebView Activity
        Intent intent = new Intent(this, PaymentWebViewActivity.class);
        intent.putExtra("PAYMENT_URL", mockPaymentUrl);
        intent.putExtra("ORDER_ID", "MOCK_ORDER_123"); // Truyền Order ID
        startActivityForResult(intent, REQUEST_CODE_VNPAY_WEBVIEW);
    }

    // Hàm lưu Order cho COD (đơn giản hóa)
    private void saveOrderToFirebase(Order order) {
        // TẠI ĐÂY: Logic lưu order vào collection 'orders' của Firebase
        // Đơn giản hóa: Trừ tồn kho và cập nhật timesUsed của voucher phải được thực hiện trên Server hoặc Cloud Functions

        Toast.makeText(this, "Đặt hàng COD thành công! Đang xử lý...", Toast.LENGTH_LONG).show();
        // Sau khi thành công:
        // Chuyển sang màn hình xác nhận
        // finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_VNPAY_WEBVIEW) {
            // Xử lý kết quả trả về từ WebView VNPay
            if (resultCode == RESULT_OK && data != null) {
                // Nhận thông tin từ PaymentWebViewActivity
                boolean paymentSuccess = data.getBooleanExtra("PAYMENT_SUCCESS", false);
                String message = data.getStringExtra("MESSAGE");

                if (paymentSuccess) {
                    Toast.makeText(this, "Thanh toán VNPay thành công! Đơn hàng đang được xử lý.", Toast.LENGTH_LONG).show();
                    // Chuyển sang màn hình thành công
                } else {
                    Toast.makeText(this, "Thanh toán thất bại: " + message, Toast.LENGTH_LONG).show();
                    // Vẫn giữ ở màn hình checkout để người dùng thử lại
                }
            } else {
                Toast.makeText(this, "Giao dịch VNPay bị hủy bỏ.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}