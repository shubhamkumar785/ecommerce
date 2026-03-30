package com.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.model.ReturnRequest;
import com.ecommerce.model.UserDtls;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Integer> {

	List<ReturnRequest> findByUser(UserDtls user);

	List<ReturnRequest> findByOrderId(String orderId);

	List<ReturnRequest> findByStatus(String status);

	List<ReturnRequest> findByProductSellerId(Integer sellerId);
}
