package com.example.shopapp;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ShippingAddress {

    // Document ID trong Firestore (để dễ dàng thao tác CRUD)
    @Exclude
    private String documentId;

    // Thông tin người nhận
    private String fullName;
    private String phoneNumber;

    // Chi tiết địa chỉ
    private String cityProvince; // Tỉnh/Thành phố
    private String district;     // Quận/Huyện
    private String ward;         // Phường/Xã
    private String streetAddress; // Tên đường, Số nhà

    // Trạng thái
    private boolean isDefault;    // Địa chỉ mặc định
    private String addressType;   // Loại địa chỉ: "Nhà Riêng" hoặc "Văn Phòng"

    // Constructor rỗng (BẮT BUỘC cho Firestore)
    public ShippingAddress() {}

    // Constructor đầy đủ (Tùy chọn)
    public ShippingAddress(String fullName, String phoneNumber, String cityProvince, String district, String ward, String streetAddress, boolean isDefault, String addressType) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.cityProvince = cityProvince;
        this.district = district;
        this.ward = ward;
        this.streetAddress = streetAddress;
        this.isDefault = isDefault;
        this.addressType = addressType;
    }

    // =======================================================
    // GETTERS VÀ SETTERS
    // =======================================================

    @Exclude
    public String getDocumentId() { return documentId; }
    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getCityProvince() { return cityProvince; }
    public void setCityProvince(String cityProvince) { this.cityProvince = cityProvince; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }

    // *** ĐÃ SỬA: Đổi tên getter/setter để khớp với tên trường 'isDefault' ***
    public boolean getIsDefault() { return isDefault; }
    public void setIsDefault(boolean aDefault) { isDefault = aDefault; }

    public String getAddressType() { return addressType; }
    public void setAddressType(String addressType) { this.addressType = addressType; }

    // Hàm tiện ích để hiển thị địa chỉ ngắn gọn
    public String getFullLocation() {
        return String.format("%s, %s, %s", getWard(), getDistrict(), getCityProvince());
    }
}