package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

// Gợi ý: Các lớp Momo SDK cần thiết cho App-to-App Payment
// import vn.momo.momoapp.MoMoPay;
// import vn.momo.momoapp.listener.MoMoPaymentListener;

import java.util.HashMap;
import java.util.Map;

public class MomoPaymentActivity extends AppCompatActivity {

    private static final String TAG = "MomoPaymentActivity";
    private Order pendingOrder;
    private static final int REQUEST_CODE_MOMO_PAYMENT = 3001;

    // Khóa Momo TEST/Sandbox (Cần được lấy từ Backend trong thực tế)
    private static final String PARTNER_CODE = "MOMONOSEND";
    private static final String MERCHANT_NAME = "ShopApp Mobile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Có thể dùng một layout đơn giản để hiển thị trạng thái "Đang chờ Momo"
        // setContentView(R.layout.activity_momo_payment);

        loadOrderData(getIntent());

        if (pendingOrder != null) {
            // Tự động bắt đầu thanh toán khi Activity khởi tạo
            requestMomoPayment(pendingOrder);
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy dữ liệu đơn hàng.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadOrderData(Intent intent) {
        String orderJson = intent.getStringExtra("ORDER_DATA_JSON");
        if (orderJson != null) {
            try {
                pendingOrder = new Gson().fromJson(orderJson, Order.class);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi parse Order JSON: " + e.getMessage());
                pendingOrder = null;
            }
        }
    }

    private void requestMomoPayment(Order order) {
        try {
            long amountInVND = (long) Math.round(order.getTotalAmount());
            String orderId = "ORDER_" + System.currentTimeMillis();

            // *** TẠI ĐÂY: GỌI FIREBASE FUNCTION TẠO HASH/GIAO DỊCH ***
            // Trong luồng App-to-App chuẩn, bạn gửi yêu cầu lên backend trước để lấy Hash/OrderID

            // Bước 1: Gọi Firebase Function (Tạm thời bỏ qua, giả sử đã có Hash)

            // Bước 2: Tạo tham số Momo SDK
            Map<String, Object> momoParams = new HashMap<>();
            momoParams.put("partnerCode", PARTNER_CODE);
            momoParams.put("partnerRefId", orderId);
            momoParams.put("customerNumber", order.getShippingAddress().get("phoneNumber"));
            momoParams.put("appData", order.getUserId());
            momoParams.put("amount", amountInVND);
            momoParams.put("description", "Thanh toán đơn hàng: " + orderId);

            // *** PHƯƠNG THỨC GỌI MOMO THẬT ***
            // MoMoPay.getInstance().sendPaymentData(this, momoParams, REQUEST_CODE_MOMO_PAYMENT);

            // MÔ PHỎNG: Hiển thị hướng dẫn và chuyển sang màn hình chờ kết quả
            Toast.makeText(this, "Đang chờ thanh toán qua Momo...", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Momo Request Sent. OrderID: " + orderId);

            // Sau khi gọi Momo SDK, Activity sẽ chờ kết quả trong onActivityResult

        } catch (Exception e) {
            Log.e(TAG, "LỖI GỌI MOMO: " + e.getMessage(), e);
            Toast.makeText(this, "Không thể kết nối Momo. Vui lòng kiểm tra cài đặt App Momo.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_MOMO_PAYMENT) {

            // TẠI ĐÂY: Xử lý kết quả trả về từ Momo App
            if (data != null) {
                // Giả lập kết quả trả về từ Momo (ví dụ: MoMo status, message)
                String momoStatus = data.getStringExtra("status");

                if ("0".equals(momoStatus)) { // Giả sử status '0' là thành công
                    // BẮT BUỘC: GỌI BACKEND XÁC THỰC LẠI GIAO DỊCH (QUERY MOMO API)
                    Log.i(TAG, "Thanh toán thành công. Cần xác thực Backend.");
                    Toast.makeText(this, "Thanh toán Momo thành công! Đang xác thực đơn hàng...", Toast.LENGTH_LONG).show();

                    // Trả kết quả thành công về CartActivity
                    setResult(RESULT_OK);
                    finish();

                } else if ("-1".equals(momoStatus) || "2".equals(momoStatus)) {
                    // -1: Lỗi, 2: Bị hủy
                    Toast.makeText(this, "Thanh toán Momo bị hủy hoặc thất bại.", Toast.LENGTH_LONG).show();
                    setResult(RESULT_CANCELED);
                    finish();
                } else {
                    Toast.makeText(this, "Giao dịch không thành công.", Toast.LENGTH_LONG).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            } else {
                Toast.makeText(this, "Giao dịch Momo bị hủy.", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }
}