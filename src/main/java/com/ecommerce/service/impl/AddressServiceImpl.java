package com.ecommerce.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ecommerce.model.Address;
import com.ecommerce.model.UserDtls;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.AddressService;

@Service
public class AddressServiceImpl implements AddressService {

	@Autowired
	private AddressRepository addressRepository;

	@Autowired
	private UserRepository userRepository;

	@Override
	public Address saveAddress(Address address, Integer userId) {
		UserDtls user = userRepository.findById(userId).orElse(null);
		if (user != null) {
			address.setUser(user);
			if (address.getIsDefault() == null) address.setIsDefault(false);
			return addressRepository.save(address);
		}
		return null;
	}

	@Override
	public List<Address> getAddressByUser(Integer userId) {
		return addressRepository.findByUserId(userId);
	}

	@Override
	public Address getAddressById(Integer id) {
		return addressRepository.findById(id).orElse(null);
	}

	@Override
	public Boolean deleteAddress(Integer id) {
		Address address = addressRepository.findById(id).orElse(null);
		if (address != null) {
			addressRepository.delete(address);
			return true;
		}
		return false;
	}
}
