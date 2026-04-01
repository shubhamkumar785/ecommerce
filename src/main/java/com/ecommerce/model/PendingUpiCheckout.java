package com.ecommerce.model;

public class PendingUpiCheckout {

	private String checkoutReference;
	private String checkoutType;
	private Integer userId;
	private Integer productId;
	private Integer quantity;
	private Integer amountInPaise;
	private String currency;
	private String gatewayOrderId;
	private String upiId;
	private OrderRequest orderRequest;

	public String getCheckoutReference() {
		return checkoutReference;
	}

	public void setCheckoutReference(String checkoutReference) {
		this.checkoutReference = checkoutReference;
	}

	public String getCheckoutType() {
		return checkoutType;
	}

	public void setCheckoutType(String checkoutType) {
		this.checkoutType = checkoutType;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getProductId() {
		return productId;
	}

	public void setProductId(Integer productId) {
		this.productId = productId;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Integer getAmountInPaise() {
		return amountInPaise;
	}

	public void setAmountInPaise(Integer amountInPaise) {
		this.amountInPaise = amountInPaise;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getGatewayOrderId() {
		return gatewayOrderId;
	}

	public void setGatewayOrderId(String gatewayOrderId) {
		this.gatewayOrderId = gatewayOrderId;
	}

	public String getUpiId() {
		return upiId;
	}

	public void setUpiId(String upiId) {
		this.upiId = upiId;
	}

	public OrderRequest getOrderRequest() {
		return orderRequest;
	}

	public void setOrderRequest(OrderRequest orderRequest) {
		this.orderRequest = orderRequest;
	}
}
