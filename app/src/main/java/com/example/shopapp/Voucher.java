package com.example.shopapp;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;

public class Voucher {

    private String documentId;
    private String code;
    private String description;

    // Loại voucher: PERCENT (%), FIXED_AMOUNT (Số tiền cố định)
    private String discountType;
    private double discountValue; // Giá trị: 10.0 (nếu là %) hoặc 50000.0 (nếu là fixed amount)

    @PropertyName("type")
    private String voucherTypeString; // Lưu dưới dạng String: "public", "hidden", "user_specific"

    private double minOrderValue;
    private long maxUsageLimit;
    private long timesUsed;

    private Date startDate;
    private Date endDate;

    public Voucher() {}

    // GETTERS VÀ SETTERS

    @Exclude
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    // *** ĐÃ SỬA: SỬ DỤNG TÊN CHUẨN discountValue ***
    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    @PropertyName("type")
    public String getVoucherTypeString() { return voucherTypeString; }

    @PropertyName("type")
    public void setVoucherTypeString(String voucherTypeString) { this.voucherTypeString = voucherTypeString; }

    @Exclude
    public VoucherType getVoucherType() { return VoucherType.fromString(voucherTypeString); }

    public double getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }

    public long getMaxUsageLimit() { return maxUsageLimit; }
    public void setMaxUsageLimit(long maxUsageLimit) { this.maxUsageLimit = maxUsageLimit; }

    public long getTimesUsed() { return timesUsed; }
    public void setTimesUsed(long timesUsed) { this.timesUsed = timesUsed; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
}