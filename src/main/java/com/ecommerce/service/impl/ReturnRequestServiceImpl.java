package com.ecommerce.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.model.ReturnRequest;
import com.ecommerce.model.UserDtls;
import com.ecommerce.repository.ReturnRequestRepository;
import com.ecommerce.service.ReturnRequestService;

@Service
public class ReturnRequestServiceImpl implements ReturnRequestService {

	@Autowired
	private ReturnRequestRepository returnRequestRepository;

	@Override
	public ReturnRequest saveReturnRequest(ReturnRequest returnRequest) {
		returnRequest.setCreatedAt(LocalDateTime.now());
		returnRequest.setStatus("PENDING");
		return returnRequestRepository.save(returnRequest);
	}

	@Override
	public List<ReturnRequest> getAllReturnRequests() {
		return returnRequestRepository.findAll();
	}

	@Override
	public List<ReturnRequest> getReturnRequestsByUser(UserDtls user) {
		return returnRequestRepository.findByUser(user);
	}

	@Override
	public ReturnRequest updateReturnStatus(Integer id, String status, String adminComment) {
		ReturnRequest returnRequest = returnRequestRepository.findById(id).orElse(null);
		if (returnRequest != null) {
			returnRequest.setStatus(status);
			returnRequest.setAdminComment(adminComment);
			return returnRequestRepository.save(returnRequest);
		}
		return null;
	}

	@Override
	public ReturnRequest getReturnRequestById(Integer id) {
		return returnRequestRepository.findById(id).orElse(null);
	}

	@Override
	public List<ReturnRequest> getReturnRequestsBySeller(Integer sellerId) {
		return returnRequestRepository.findByProductSellerId(sellerId);
	}
}
