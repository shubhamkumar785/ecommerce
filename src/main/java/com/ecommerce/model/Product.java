package com.ecommerce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 500)
	private String title;

	@Column(length = 5000)
	private String description;

	private String category;

	private Double price;

	private int stock;

	private String image;

	private int discount;
	
	private Double discountPrice;
	
	private String color;
	
	private String size;
	
	private Double rating;
	
	private Boolean isActive;
	
	private Boolean isReturnable;
	
	private Integer returnWindow;
	
	private Double costPrice;

	@jakarta.persistence.ManyToOne
	private UserDtls seller;
	
	public Integer getId() {
	    return id;
	}

	public void setId(Integer id) {
	    this.id = id;
	}

	// Getter and Setter for title
	public String getTitle() {
	    return title;
	}

	public void setTitle(String title) {
	    this.title = title;
	}

	// Getter and Setter for description
	public String getDescription() {
	    return description;
	}

	public void setDescription(String description) {
	    this.description = description;
	}

	// Getter and Setter for category
	public String getCategory() {
	    return category;
	}

	public void setCategory(String category) {
	    this.category = category;
	}

	// Getter and Setter for price
	public Double getPrice() {
	    return price;
	}

	public void setPrice(Double price) {
	    this.price = price;
	}

	// Getter and Setter for stock
	public int getStock() {
	    return stock;
	}

	public void setStock(int stock) {
	    this.stock = stock;
	}

	// Getter and Setter for image
	public String getImage() {
	    return image;
	}

	public void setImage(String image) {
	    this.image = image;
	}

	// Getter and Setter for discount
	public int getDiscount() {
	    return discount;
	}

	public void setDiscount(int discount) {
	    this.discount = discount;
	}

	// Getter and Setter for discountPrice
	public Double getDiscountPrice() {
	    return discountPrice;
	}

	public void setDiscountPrice(Double discountPrice) {
	    this.discountPrice = discountPrice;
	}

	// Getter and Setter for isActive
	public Boolean getIsActive() {
	    return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public UserDtls getSeller() {
		return seller;
	}

	public void setSeller(UserDtls seller) {
		this.seller = seller;
	}

	// Getter and Setter for color
	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	// Getter and Setter for size
	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	// Getter and Setter for rating
	public Double getRating() {
		return rating;
	}

	public void setRating(Double rating) {
		this.rating = rating;
	}

	public Boolean getIsReturnable() {
		return isReturnable;
	}

	public void setIsReturnable(Boolean isReturnable) {
		this.isReturnable = isReturnable;
	}

	public Integer getReturnWindow() {
		return returnWindow;
	}

	public void setReturnWindow(Integer returnWindow) {
		this.returnWindow = returnWindow;
	}

	public Double getCostPrice() {
		return costPrice;
	}

	public void setCostPrice(Double costPrice) {
		this.costPrice = costPrice;
	}
}