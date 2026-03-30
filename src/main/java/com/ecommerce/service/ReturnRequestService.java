package com.ecommerce.service;

import java.util.List;

import com.ecommerce.model.ReturnRequest;
import com.ecommerce.model.UserDtls;

public interface ReturnRequestService {

	ReturnRequest saveReturnRequest(ReturnRequest returnRequest);

	List<ReturnRequest> getAllReturnRequests();

	List<ReturnRequest> getReturnRequestsByUser(UserDtls user);

	ReturnRequest updateReturnStatus(Integer id, String status, String adminComment);

	ReturnRequest getReturnRequestById(Integer id);

	List<ReturnRequest> getReturnRequestsBySeller(Integer sellerId);
}
