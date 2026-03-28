package com.ecommerce.service;

import java.util.List;
import com.ecommerce.model.Address;

public interface AddressService {
	public Address saveAddress(Address address, Integer userId);
	public List<Address> getAddressByUser(Integer userId);
	public Address getAddressById(Integer id);
	public Boolean deleteAddress(Integer id);
}
