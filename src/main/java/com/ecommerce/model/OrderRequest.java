package com.ecommerce.model;

public class OrderRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String mobileNo;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String paymentType;
    private String upiId;
    private String paymentStatus;
    private String paymentGatewayProvider;
    private String paymentGatewayOrderId;
    private String paymentGatewayPaymentId;
    private String paymentFailureCode;
    private String paymentFailureReason;

    // Default Constructor
    public OrderRequest() {
    }

    // Getter and Setter for firstName
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    // Getter and Setter for lastName
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    // Getter and Setter for email
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Getter and Setter for mobileNo
    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    // Getter and Setter for address
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    // Getter and Setter for city
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    // Getter and Setter for state
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    // Getter and Setter for pincode
    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    // Getter and Setter for paymentType
    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentGatewayProvider() {
        return paymentGatewayProvider;
    }

    public void setPaymentGatewayProvider(String paymentGatewayProvider) {
        this.paymentGatewayProvider = paymentGatewayProvider;
    }

    public String getPaymentGatewayOrderId() {
        return paymentGatewayOrderId;
    }

    public void setPaymentGatewayOrderId(String paymentGatewayOrderId) {
        this.paymentGatewayOrderId = paymentGatewayOrderId;
    }

    public String getPaymentGatewayPaymentId() {
        return paymentGatewayPaymentId;
    }

    public void setPaymentGatewayPaymentId(String paymentGatewayPaymentId) {
        this.paymentGatewayPaymentId = paymentGatewayPaymentId;
    }

    public String getPaymentFailureCode() {
        return paymentFailureCode;
    }

    public void setPaymentFailureCode(String paymentFailureCode) {
        this.paymentFailureCode = paymentFailureCode;
    }

    public String getPaymentFailureReason() {
        return paymentFailureReason;
    }

    public void setPaymentFailureReason(String paymentFailureReason) {
        this.paymentFailureReason = paymentFailureReason;
    }
}
