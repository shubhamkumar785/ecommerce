package com.ecommerce.model;

public class UpiPaymentVerificationRequest {

	private String checkoutReference;
	private String razorpayOrderId;
	private String razorpayPaymentId;
	private String razorpaySignature;

	public String getCheckoutReference() {
		return checkoutReference;
	}

	public void setCheckoutReference(String checkoutReference) {
		this.checkoutReference = checkoutReference;
	}

	public String getRazorpayOrderId() {
		return razorpayOrderId;
	}

	public void setRazorpayOrderId(String razorpayOrderId) {
		this.razorpayOrderId = razorpayOrderId;
	}

	public String getRazorpayPaymentId() {
		return razorpayPaymentId;
	}

	public void setRazorpayPaymentId(String razorpayPaymentId) {
		this.razorpayPaymentId = razorpayPaymentId;
	}

	public String getRazorpaySignature() {
		return razorpaySignature;
	}

	public void setRazorpaySignature(String razorpaySignature) {
		this.razorpaySignature = razorpaySignature;
	}
}
