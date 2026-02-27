package com.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.model.UserDtls;

public interface UserRepository extends JpaRepository<UserDtls, Integer> {

}