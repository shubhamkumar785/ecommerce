package com.ecommerce.service.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.model.UserDtls;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDtls saveUser(UserDtls user) {
		UserDtls saveUser = userRepository.save(user);
		return saveUser;
	}

}