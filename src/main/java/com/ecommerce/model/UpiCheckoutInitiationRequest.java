package com.ecommerce.model;

public class UpiCheckoutInitiationRequest extends OrderRequest {

	private String checkoutType;
	private Integer pid;
	private Integer quantity;

	public String getCheckoutType() {
		return checkoutType;
	}

	public void setCheckoutType(String checkoutType) {
		this.checkoutType = checkoutType;
	}

	public Integer getPid() {
		return pid;
	}

	public void setPid(Integer pid) {
		this.pid = pid;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
}
