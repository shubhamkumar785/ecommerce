package com.ecommerce.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class Address {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String fullName;

	private String mobileNumber;

	private String addressLine1;

	private String city;

	private String state;

	private String pincode;

	private String landmark;

	private String country;

	private Boolean isDefault;

	@ManyToOne
	private UserDtls user;

	// Getter and Setter for id
	public Integer getId() {
	    return id;
	}

	public void setId(Integer id) {
	    this.id = id;
	}

	// Getter and Setter for fullName
	public String getFullName() {
	    return fullName;
	}

	public void setFullName(String fullName) {
	    this.fullName = fullName;
	}

	// Getter and Setter for mobileNumber
	public String getMobileNumber() {
	    return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
	    this.mobileNumber = mobileNumber;
	}

	// Getter and Setter for addressLine1
	public String getAddressLine1() {
	    return addressLine1;
	}

	public void setAddressLine1(String addressLine1) {
	    this.addressLine1 = addressLine1;
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

	// Getter and Setter for landmark
	public String getLandmark() {
	    return landmark;
	}

	public void setLandmark(String landmark) {
	    this.landmark = landmark;
	}

	// Getter and Setter for country
	public String getCountry() {
	    return country;
	}

	public void setCountry(String country) {
	    this.country = country;
	}

	// Getter and Setter for isDefault
	public Boolean getIsDefault() {
	    return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
	    this.isDefault = isDefault;
	}

	// Getter and Setter for user
	public UserDtls getUser() {
	    return user;
	}

	public void setUser(UserDtls user) {
	    this.user = user;
	}
}
