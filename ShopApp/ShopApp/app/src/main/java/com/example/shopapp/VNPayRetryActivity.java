package com.example.shopapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;

public class VNPayRetryActivity extends AppCompatActivity {

    private static final String TAG = "VNPayRetryActivity";
    private WebView webView;
    private ProgressBar progressBar;
    private String orderId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar_vnpay);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Thanh toán lại - VNPAY");

        webView = findViewById(R.id.webview_vnpay);
        progressBar = findViewById(R.id.progressbar_vnpay);

        String url = getIntent().getStringExtra("payment_url");
        orderId = getIntent().getStringExtra("order_id");

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(VNPayConfig.vnp_ReturnUrl)) {
                    Uri uri = Uri.parse(url);
                    String responseCode = uri.getQueryParameter("vnp_ResponseCode");

                    if ("00".equals(responseCode)) {
                        // Thanh toán thành công - Update order status
                        updateOrderStatus("PAID");
                    } else {
                        // Thanh toán thất bại
                        Toast.makeText(VNPayRetryActivity.this,
                                "Thanh toán thất bại. Vui lòng thử lại!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        webView.loadUrl(url);
    }

    private void updateOrderStatus(String newStatus) {
        if (orderId == null) {
            finish();
            return;
        }

        db.collection("orders").document(orderId)
                .update("orderStatus", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();

                    // Chuyển về OrderDetailActivity
                    Intent intent = new Intent(this, OrderDetailActivity.class);
                    intent.putExtra("ORDER_ID", orderId);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating order status", e);
                    Toast.makeText(this, "Lỗi cập nhật trạng thái đơn hàng", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}