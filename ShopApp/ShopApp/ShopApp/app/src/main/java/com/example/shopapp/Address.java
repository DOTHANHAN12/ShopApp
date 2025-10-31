package com.example.shopapp;

// Model cho chi tiết địa chỉ
public class Address {
    public String addressId;
    public String street;
    public String city;
    public String zipCode;
    public boolean isDefault;

    public Address() {}

    public Address(String addressId, String street, String city, String zipCode, boolean isDefault) {
        this.addressId = addressId;
        this.street = street;
        this.city = city;
        this.zipCode = zipCode;
        this.isDefault = isDefault;
    }

    // GETTERS VÀ SETTERS
    public String getAddressId() { return addressId; }
    public void setAddressId(String addressId) { this.addressId = addressId; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
}