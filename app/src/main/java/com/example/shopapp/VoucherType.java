package com.example.shopapp;

public enum VoucherType {
    // PUBLIC: Voucher hiển thị công khai trong danh sách chọn (VoucherSelectionActivity)
    PUBLIC("public"),
    // HIDDEN: Voucher cần nhập tay (hoặc chỉ gửi riêng cho người dùng)
    HIDDEN("hidden"),
    // USER_SPECIFIC: Voucher chỉ dùng 1 lần cho 1 người dùng (cần quản lý trong collection UserVoucher)
    USER_SPECIFIC("user_specific");

    private final String value;

    VoucherType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // Phương thức utility để chuyển đổi từ String sang Enum
    public static VoucherType fromString(String text) {
        for (VoucherType type : VoucherType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return null; // Hoặc ném ngoại lệ tùy logic
    }
}