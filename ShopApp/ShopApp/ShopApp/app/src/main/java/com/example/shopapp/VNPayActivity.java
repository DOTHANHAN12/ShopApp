package com.example.shopapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class VNPayActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private Order order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay);

        Toolbar toolbar = findViewById(R.id.toolbar_vnpay);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Thanh toán VNPAY");

        webView = findViewById(R.id.webview_vnpay);
        progressBar = findViewById(R.id.progressbar_vnpay);

        String url = getIntent().getStringExtra("payment_url");
        order = (Order) getIntent().getSerializableExtra("order");

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

                    Intent resultIntent = new Intent();
                    if ("00".equals(responseCode)) {
                        resultIntent.putExtra("status", "success");
                    } else {
                        resultIntent.putExtra("status", "failure");
                    }
                    resultIntent.putExtra("order", order);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        webView.loadUrl(url);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
