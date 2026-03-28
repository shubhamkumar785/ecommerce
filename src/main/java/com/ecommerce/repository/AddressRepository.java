package com.ecommerce.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ecommerce.model.Address;

public interface AddressRepository extends JpaRepository<Address, Integer> {
	List<Address> findByUserId(Integer userId);
}
