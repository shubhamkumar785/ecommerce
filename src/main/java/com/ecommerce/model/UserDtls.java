package com.ecommerce.model;

import java.util.Date;

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
public class UserDtls {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String name;

	private String mobileNumber;

	private String email;

	private String address;

	private String city;

	private String state;

	private String pincode;

	private String password;

	private String profileImage;

	private String role;

	private Boolean isEnable;

	private Boolean accountNonLocked;

	private Integer failedAttempt;

	private Date lockTime;
	
	private String resetToken;
	
	private String storeName;
	
	private String storeDescription;

	// Getter and Setter for id
	public Integer getId() {
	    return id;
	}

	public void setId(Integer id) {
	    this.id = id;
	}

	// Getter and Setter for storeName
	public String getStoreName() {
	    return storeName;
	}

	public void setStoreName(String storeName) {
	    this.storeName = storeName;
	}

	// Getter and Setter for storeDescription
	public String getStoreDescription() {
	    return storeDescription;
	}

	public void setStoreDescription(String storeDescription) {
	    this.storeDescription = storeDescription;
	}

	// Getter and Setter for name
	public String getName() {
	    return name;
	}

	public void setName(String name) {
	    this.name = name;
	}

	// Getter and Setter for mobileNumber
	public String getMobileNumber() {
	    return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
	    this.mobileNumber = mobileNumber;
	}

	// Getter and Setter for email
	public String getEmail() {
	    return email;
	}

	public void setEmail(String email) {
	    this.email = email;
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

	// Getter and Setter for password
	public String getPassword() {
	    return password;
	}

	public void setPassword(String password) {
	    this.password = password;
	}

	// Getter and Setter for profileImage
	public String getProfileImage() {
	    return profileImage;
	}

	public void setProfileImage(String profileImage) {
	    this.profileImage = profileImage;
	}

	// Getter and Setter for role
	public String getRole() {
	    return role;
	}

	public void setRole(String role) {
	    this.role = role;
	}

	// Getter and Setter for isEnable
	public Boolean getIsEnable() {
	    return isEnable;
	}

	public void setIsEnable(Boolean isEnable) {
	    this.isEnable = isEnable;
	}

	// Getter and Setter for accountNonLocked
	public Boolean getAccountNonLocked() {
	    return accountNonLocked;
	}

	public void setAccountNonLocked(Boolean accountNonLocked) {
	    this.accountNonLocked = accountNonLocked;
	}

	// Getter and Setter for failedAttempt
	public Integer getFailedAttempt() {
	    return failedAttempt;
	}

	public void setFailedAttempt(Integer failedAttempt) {
	    this.failedAttempt = failedAttempt;
	}

	// Getter and Setter for lockTime
	public Date getLockTime() {
	    return lockTime;
	}

	public void setLockTime(Date lockTime) {
	    this.lockTime = lockTime;
	}

	// Getter and Setter for resetToken
	public String getResetToken() {
	    return resetToken;
	}

	public void setResetToken(String resetToken) {
	    this.resetToken = resetToken;
	}
}