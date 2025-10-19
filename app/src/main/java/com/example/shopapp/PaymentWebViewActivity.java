package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URL;

public class PaymentWebViewActivity extends AppCompatActivity {

    private static final String TAG = "PaymentWebViewActivity";
    private WebView webView;
    private String paymentUrl;
    private String orderId; // ID đơn hàng được truyền vào

    // URL mà VNPay sẽ redirect về sau khi người dùng thanh toán xong (hoặc hủy)
    private static final String VNPAY_RETURN_URL_HOST = "localhost"; // Giả định ReturnUrl của bạn là http://localhost:8080/vnpay_return

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_webview); // Bạn cần tạo layout này

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        webView = findViewById(R.id.webViewPayment); // Giả định ID trong layout

        Intent intent = getIntent();
        paymentUrl = intent.getStringExtra("PAYMENT_URL");
        orderId = intent.getStringExtra("ORDER_ID");

        if (paymentUrl == null || orderId == null) {
            Toast.makeText(this, "Lỗi: Dữ liệu thanh toán không hợp lệ.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupWebView();
        webView.loadUrl(paymentUrl);
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {

            // Hàm này được gọi mỗi khi WebView cố gắng tải một URL mới
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                Log.d(TAG, "Đang tải URL: " + url);

                // *** BẮT TRẠNG THÁI THANH TOÁN TỪ VNPAY RETURN URL ***
                if (url.contains(VNPAY_RETURN_URL_HOST) || url.contains("vnp_ResponseCode")) {
                    handleVnPayResult(url);
                    return true; // Ngăn WebView tải tiếp URL này
                }

                return super.shouldOverrideUrlLoading(view, request);
            }
        });
    }

    private void handleVnPayResult(String returnUrl) {
        // TẠI ĐÂY: Logic phân tích chuỗi URL trả về từ VNPay

        boolean success = false;
        String message = "Giao dịch không xác định.";

        try {
            // Lấy tham số vnp_ResponseCode và vnp_TransactionStatus
            // Trong môi trường thật: bạn phải parse query parameters

            if (returnUrl.contains("vnp_ResponseCode=00") && returnUrl.contains("vnp_TransactionStatus=00")) {
                // Mã 00 thường là giao dịch thành công trong hệ thống VNPay
                success = true;
                message = "Thanh toán thành công! Đơn hàng " + orderId;
            } else if (returnUrl.contains("vnp_ResponseCode=24")) {
                message = "Thanh toán bị hủy bởi người dùng.";
            } else {
                // Các mã lỗi khác (ví dụ: 01, 02, 07, 99...)
                message = "Thanh toán thất bại. Vui lòng thử lại.";
            }

        } catch (Exception e) {
            Log.e(TAG, "Lỗi phân tích kết quả VNPay: " + e.getMessage());
            success = false;
            message = "Lỗi hệ thống khi xử lý kết quả.";
        }

        // Trả kết quả về CheckoutActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("PAYMENT_SUCCESS", success);
        resultIntent.putExtra("MESSAGE", message);
        setResult(RESULT_OK, resultIntent);

        finish(); // Đóng WebView
    }
}