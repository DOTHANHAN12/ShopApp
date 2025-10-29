package com.example.shopapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VoucherSelectionActivity extends AppCompatActivity implements VoucherAdapter.OnVoucherClickListener {

    private static final String TAG = "VoucherSelectActivity";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Các RecyclerView
    private RecyclerView recyclerShippingVouchers;
    private RecyclerView recyclerAvailableVouchers;
    private RecyclerView recyclerUnavailableVouchers;

    // Các Adapter
    private VoucherAdapter shippingVoucherAdapter;
    private VoucherAdapter availableVoucherAdapter;
    private VoucherAdapter unavailableVoucherAdapter;

    // Danh sách Voucher
    private final List<Voucher> shippingVoucherList = new ArrayList<>();
    private final List<Voucher> availableVoucherList = new ArrayList<>();
    private final List<Voucher> unavailableVoucherList = new ArrayList<>();

    // UI elements
    private EditText editVoucherCodeInput;
    private Button btnApplyManualVoucher;
    private TextView textVoucherSelectedSummary;
    private TextView textVoucherActionMessage;
    private Button btnConfirmVoucherSelection;

    // Header Textviews
    private TextView textShippingVoucherHeader;
    private TextView textAvailableVoucherHeader;
    private TextView textUnavailableVoucherHeader;

    private Voucher selectedVoucher = null;
    private double currentCartSubtotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_voucher_selection);

            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }

            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();

            currentCartSubtotal = getIntent().getDoubleExtra("CURRENT_SUBTOTAL", 0.0);
            Log.d(TAG, "Subtotal nhận được: " + currentCartSubtotal);

            mapViews();
            setupRecyclerViews();
            loadVouchers();
            updateFooterUI();
            Log.d(TAG, "Activity khởi tạo thành công.");

        } catch (Exception e) {
            // Ghi lỗi chi tiết ra màn hình bằng Toast
            String errorMessage = "LỖI CRASH! Nguyên nhân: " + (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định");
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

            // Vẫn ghi log chi tiết cho Logcat
            Log.e(TAG, "LỖI CRASH KHỞI TẠO VOUCHER ACTIVITY: " + errorMessage, e);

            // Ném lỗi để đảm bảo Logcat hiển thị Stack Trace đầy đủ
            throw new RuntimeException("Voucher Activity failed to start: " + e.getMessage());
        }
    }

    private void mapViews() {
        try {
            // Header
            ImageView imgBack = findViewById(R.id.img_back_voucher);
            imgBack.setOnClickListener(v -> finish());

            // Input Voucher Manual
            editVoucherCodeInput = findViewById(R.id.edit_voucher_code_input);
            btnApplyManualVoucher = findViewById(R.id.btn_apply_manual_voucher);
            btnApplyManualVoucher.setOnClickListener(v -> handleApplyManualVoucher());

            // Headers cho các phần RecyclerView
            textShippingVoucherHeader = findViewById(R.id.text_shipping_voucher_header);
            textAvailableVoucherHeader = findViewById(R.id.text_available_voucher_header);
            textUnavailableVoucherHeader = findViewById(R.id.text_unavailable_voucher_header);

            // RecyclerViews
            recyclerShippingVouchers = findViewById(R.id.recycler_shipping_vouchers);
            recyclerAvailableVouchers = findViewById(R.id.recycler_available_vouchers);
            recyclerUnavailableVouchers = findViewById(R.id.recycler_unavailable_vouchers);

            // Footer
            textVoucherSelectedSummary = findViewById(R.id.text_voucher_selected_summary);
            textVoucherActionMessage = findViewById(R.id.text_voucher_action_message);
            btnConfirmVoucherSelection = findViewById(R.id.btn_confirm_voucher_selection);
            btnConfirmVoucherSelection.setOnClickListener(v -> returnSelectedVoucher());

        } catch (Exception e) {
            Log.e(TAG, "LỖI FINDVIEWBYID: Thiếu ID trong activity_voucher_selection.xml", e);
            // Quan trọng: Bắt lỗi và Ném lại để hiển thị Stack Trace chi tiết nhất
            throw new RuntimeException("UI Initialization Failed: " + e.getMessage());
        }
    }

    private void setupRecyclerViews() {
        Log.d(TAG, "Thiết lập RecyclerViews...");
        shippingVoucherAdapter = new VoucherAdapter(this, shippingVoucherList, this, currentCartSubtotal);
        recyclerShippingVouchers.setLayoutManager(new LinearLayoutManager(this));
        recyclerShippingVouchers.setAdapter(shippingVoucherAdapter);

        availableVoucherAdapter = new VoucherAdapter(this, availableVoucherList, this, currentCartSubtotal);
        recyclerAvailableVouchers.setLayoutManager(new LinearLayoutManager(this));
        recyclerAvailableVouchers.setAdapter(availableVoucherAdapter);

        unavailableVoucherAdapter = new VoucherAdapter(this, unavailableVoucherList, this, currentCartSubtotal);
        recyclerUnavailableVouchers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUnavailableVouchers.setAdapter(unavailableVoucherAdapter);
        Log.d(TAG, "Thiết lập RecyclerViews HOÀN THÀNH.");
    }

    private void loadVouchers() {
        Log.d(TAG, "Bắt đầu tải vouchers từ Firestore.");
        // Logic tải vouchers giữ nguyên
        db.collection("vouchers")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // ... (Logic phân loại voucher)
                    shippingVoucherList.clear();
                    availableVoucherList.clear();
                    unavailableVoucherList.clear();

                    Date now = new Date();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Voucher voucher = document.toObject(Voucher.class);
                        voucher.setDocumentId(document.getId());

                        if (VoucherType.PUBLIC.getValue().equals(voucher.getVoucherTypeString())) {

                            boolean isValidTime = voucher.getStartDate() != null && voucher.getEndDate() != null &&
                                    !voucher.getStartDate().after(now) && !voucher.getEndDate().before(now);
                            boolean isNotUsedUp = voucher.getTimesUsed() < voucher.getMaxUsageLimit();

                            if (isValidTime && isNotUsedUp) {
                                availableVoucherList.add(voucher);
                            } else {
                                unavailableVoucherList.add(voucher);
                            }
                        }
                    }

                    availableVoucherAdapter.notifyDataSetChanged();
                    unavailableVoucherAdapter.notifyDataSetChanged();

                    textShippingVoucherHeader.setVisibility(shippingVoucherList.isEmpty() ? View.GONE : View.VISIBLE);
                    textAvailableVoucherHeader.setVisibility(availableVoucherList.isEmpty() ? View.GONE : View.VISIBLE);
                    textUnavailableVoucherHeader.setVisibility(unavailableVoucherList.isEmpty() ? View.GONE : View.VISIBLE);
                    Log.d(TAG, "Tải vouchers thành công: " + availableVoucherList.size() + " khả dụng.");

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "LỖI FIRESTORE: Không thể tải danh sách voucher.", e);
                    Toast.makeText(this, "Lỗi kết nối khi tải voucher.", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleApplyManualVoucher() {
        final String inputCode = editVoucherCodeInput.getText().toString().trim();
        Log.d(TAG, "Mã thủ công nhập vào: " + inputCode);
        if (inputCode.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã voucher.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("vouchers")
                .whereEqualTo("code", inputCode)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        Voucher voucher = querySnapshot.getDocuments().get(0).toObject(Voucher.class);

                        if (voucher.getVoucherType().equals(VoucherType.HIDDEN) || voucher.getVoucherType().equals(VoucherType.PUBLIC)) {
                            // Giả định logic kiểm tra thời gian/lượt dùng đã được thực hiện
                            // NẾU HỢP LỆ:
                            selectedVoucher = voucher;
                            updateFooterUI();
                            Toast.makeText(this, "Đã áp dụng mã " + inputCode, Toast.LENGTH_SHORT).show();
                            editVoucherCodeInput.setText("");
                        } else {
                            Toast.makeText(this, "Mã voucher không phải loại nhập tay hợp lệ.", Toast.LENGTH_LONG).show();
                            selectedVoucher = null;
                        }
                    } else {
                        Toast.makeText(this, "Mã voucher không tồn tại.", Toast.LENGTH_LONG).show();
                        selectedVoucher = null;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "LỖI FIRESTORE khi nhập mã thủ công.", e);
                    Toast.makeText(this, "Lỗi kết nối khi áp dụng mã.", Toast.LENGTH_SHORT).show();
                });
    }

    private void returnSelectedVoucher() {
        String code = (selectedVoucher != null) ? selectedVoucher.getCode() : "";
        Log.i(TAG, "Trả về voucher đã chọn: " + (code.isEmpty() ? "NULL" : code));
        Intent resultIntent = new Intent();
        resultIntent.putExtra("SELECTED_VOUCHER_CODE", code);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onVoucherClicked(Voucher voucher) {
        if (voucher == selectedVoucher) {
            selectedVoucher = null;
        } else {
            if (currentCartSubtotal >= voucher.getMinOrderValue()) {
                selectedVoucher = voucher;
            } else {
                Toast.makeText(this, "Đơn hàng chưa đạt giá trị tối thiểu " + String.format(Locale.getDefault(), "%,.0f₫", voucher.getMinOrderValue()), Toast.LENGTH_LONG).show();
                selectedVoucher = null;
            }
        }

        availableVoucherAdapter.setSelectedVoucher(selectedVoucher);
        shippingVoucherAdapter.setSelectedVoucher(selectedVoucher);

        updateFooterUI();
    }

    private void updateFooterUI() {
        if (selectedVoucher != null) {
            textVoucherSelectedSummary.setText("1 Voucher đã được chọn.");
            textVoucherActionMessage.setText("Đã áp dụng mã: " + selectedVoucher.getCode());
            btnConfirmVoucherSelection.setEnabled(true);
            btnConfirmVoucherSelection.setBackgroundResource(R.drawable.bg_rounded_primary);
        } else {
            textVoucherSelectedSummary.setText("Chưa có voucher nào được chọn.");
            textVoucherActionMessage.setText("");
            btnConfirmVoucherSelection.setEnabled(false);
            btnConfirmVoucherSelection.setBackgroundResource(R.drawable.bg_rounded_grey);
        }
    }
}