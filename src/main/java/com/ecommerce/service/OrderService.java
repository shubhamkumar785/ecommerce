package com.ecommerce.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ecommerce.model.OrderRequest;
import com.ecommerce.model.Product;
import com.ecommerce.model.ProductOrder;

public interface OrderService {

	public void saveOrder(Integer userid, OrderRequest orderRequest) throws Exception;

	public ProductOrder saveSingleProductOrder(Integer userId, Product product, Integer quantity, OrderRequest orderRequest)
			throws Exception;

	public List<ProductOrder> getOrdersByUser(Integer userId);

	public ProductOrder updateOrderStatus(Integer id, String status);

	public ProductOrder getOrderById(Integer id);

	public List<ProductOrder> getAllOrders();

	public ProductOrder getOrdersByOrderId(String orderId);
	
	public Page<ProductOrder> getAllOrdersPagination(Integer pageNo,Integer pageSize);

	public Page<ProductOrder> getOrdersBySeller(Integer sellerId, Integer pageNo, Integer pageSize);
}
