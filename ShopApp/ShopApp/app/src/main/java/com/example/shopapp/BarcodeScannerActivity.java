package com.example.shopapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeScannerActivity extends AppCompatActivity {

    private static final String TAG = "BarcodeScanner";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private PreviewView previewView;
    private TextView textInstruction;
    private ImageView btnBack;
    private ImageView btnFlashlight;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ExecutorService cameraExecutor;
    private FirebaseFirestore db;
    private boolean isScanning = true;
    private boolean isFlashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);

        db = FirebaseFirestore.getInstance();
        cameraExecutor = Executors.newSingleThreadExecutor();

        initViews();
        setupListeners();

        // Check camera permission
        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.preview_view);
        textInstruction = findViewById(R.id.text_instruction);
        btnBack = findViewById(R.id.btn_back_scanner);
        btnFlashlight = findViewById(R.id.btn_flashlight);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnFlashlight.setOnClickListener(v -> toggleFlashlight());
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Cần quyền camera để quét barcode", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Image Analysis for barcode scanning
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (!isScanning) {
                        imageProxy.close();
                        return;
                    }

                    InputImage image = InputImage.fromMediaImage(
                            imageProxy.getImage(),
                            imageProxy.getImageInfo().getRotationDegrees()
                    );

                    // Scan barcode
                    BarcodeScanning.getClient()
                            .process(image)
                            .addOnSuccessListener(barcodes -> {
                                if (!isScanning) return;

                                for (Barcode barcode : barcodes) {
                                    String barcodeValue = barcode.getRawValue();
                                    if (barcodeValue != null && !barcodeValue.isEmpty()) {
                                        // Found barcode, stop scanning and search product
                                        isScanning = false;
                                        searchProductByBarcode(barcodeValue);
                                        break;
                                    }
                                }
                                imageProxy.close();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error scanning barcode", e);
                                imageProxy.close();
                            });
                });

                // Camera selector
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Lỗi khởi động camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void toggleFlashlight() {
        if (camera != null) {
            if (isFlashOn) {
                camera.getCameraControl().enableTorch(false);
                isFlashOn = false;
                btnFlashlight.setAlpha(0.5f);
            } else {
                camera.getCameraControl().enableTorch(true);
                isFlashOn = true;
                btnFlashlight.setAlpha(1.0f);
            }
        }
    }

    private void searchProductByBarcode(String barcodeValue) {
        textInstruction.setText("Đang tìm sản phẩm...");
        
        // Search in Firestore
        db.collection("products")
                .whereEqualTo("barcode", barcodeValue)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Found product
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        Product product = document.toObject(Product.class);
                        
                        if (product != null) {
                            // Navigate to product detail
                            Intent intent = new Intent(BarcodeScannerActivity.this, ProductDetailActivity.class);
                            intent.putExtra("PRODUCT_ID", product.productId);
                            startActivity(intent);
                            finish();
                        } else {
                            showProductNotFound();
                        }
                    } else {
                        showProductNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching product", e);
                    Toast.makeText(this, "Lỗi tìm kiếm sản phẩm", Toast.LENGTH_SHORT).show();
                    isScanning = true;
                    textInstruction.setText("Đưa camera vào barcode để quét");
                });
    }

    private void showProductNotFound() {
        textInstruction.setText("Không tìm thấy sản phẩm với barcode này");
        Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
        
        // Resume scanning after 2 seconds
        previewView.postDelayed(() -> {
            isScanning = true;
            textInstruction.setText("Đưa camera vào barcode để quét");
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}

