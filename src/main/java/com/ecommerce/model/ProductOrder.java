package com.ecommerce.model;

import java.time.LocalDate;
import java.util.Date;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class ProductOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String orderId;

	private LocalDate orderDate;

	@ManyToOne
	private Product product;

	private Double price;

	private Integer quantity;

	@ManyToOne
	private UserDtls user;

	private String status;

	private String paymentType;

	private Double costPrice;

	@OneToOne(cascade = CascadeType.ALL)
	private OrderAddress orderAddress;
	
	// Getter and Setter for id
	public Integer getId() {
	    return id;
	}

	public void setId(Integer id) {
	    this.id = id;
	}

	// Getter and Setter for orderId
	public String getOrderId() {
	    return orderId;
	}

	public void setOrderId(String orderId) {
	    this.orderId = orderId;
	}

	// Getter and Setter for orderDate
	public LocalDate getOrderDate() {
	    return orderDate;
	}

	public void setOrderDate(LocalDate orderDate) {
	    this.orderDate = orderDate;
	}

	// Getter and Setter for product
	public Product getProduct() {
	    return product;
	}

	public void setProduct(Product product) {
	    this.product = product;
	}

	// Getter and Setter for price
	public Double getPrice() {
	    return price;
	}

	public void setPrice(Double price) {
	    this.price = price;
	}

	// Getter and Setter for quantity
	public Integer getQuantity() {
	    return quantity;
	}

	public void setQuantity(Integer quantity) {
	    this.quantity = quantity;
	}

	// Getter and Setter for user
	public UserDtls getUser() {
	    return user;
	}

	public void setUser(UserDtls user) {
	    this.user = user;
	}

	// Getter and Setter for status
	public String getStatus() {
	    return status;
	}

	public void setStatus(String status) {
	    this.status = status;
	}

	// Getter and Setter for paymentType
	public String getPaymentType() {
	    return paymentType;
	}

	public void setPaymentType(String paymentType) {
	    this.paymentType = paymentType;
	}

	// Getter and Setter for orderAddress
	public OrderAddress getOrderAddress() {
	    return orderAddress;
	}

	public void setOrderAddress(OrderAddress orderAddress) {
	    this.orderAddress = orderAddress;
	}

	public Double getCostPrice() {
		return costPrice;
	}

	public void setCostPrice(Double costPrice) {
		this.costPrice = costPrice;
	}

}