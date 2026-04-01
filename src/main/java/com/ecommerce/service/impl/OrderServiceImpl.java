package com.ecommerce.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ecommerce.model.Cart;
import com.ecommerce.model.OrderAddress;
import com.ecommerce.model.OrderRequest;
import com.ecommerce.model.Product;
import com.ecommerce.model.ProductOrder;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductOrderRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.util.CommonUtil;
import com.ecommerce.util.OrderStatus;

@Service
public class OrderServiceImpl implements OrderService {
	private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

	@Autowired
	private ProductOrderRepository orderRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CommonUtil commonUtil;

	@Override
	public void saveOrder(Integer userid, OrderRequest orderRequest) throws Exception {

		List<Cart> carts = cartRepository.findByUserId(userid);

		for (Cart cart : carts) {
			ProductOrder saveOrder = buildAndSaveOrder(cart.getUser().getId(), cart.getProduct(), cart.getQuantity(), orderRequest);
			sendOrderMailSafely(saveOrder);
		}
	}

	@Override
	public ProductOrder saveSingleProductOrder(Integer userId, Product product, Integer quantity, OrderRequest orderRequest)
			throws Exception {
		ProductOrder saveOrder = buildAndSaveOrder(userId, product, quantity, orderRequest);
		sendOrderMailSafely(saveOrder);
		return saveOrder;
	}

	@Override
	public List<ProductOrder> getOrdersByUser(Integer userId) {
		List<ProductOrder> orders = orderRepository.findByUserId(userId);
		return orders;
	}

	@Override
	public ProductOrder updateOrderStatus(Integer id, String status) {
		Optional<ProductOrder> findById = orderRepository.findById(id);
		if (findById.isPresent()) {
			ProductOrder productOrder = findById.get();
			productOrder.setStatus(status);
			ProductOrder updateOrder = orderRepository.save(productOrder);
			return updateOrder;
		}
		return null;
	}

	@Override
	public ProductOrder getOrderById(Integer id) {
		return orderRepository.findById(id).orElse(null);
	}

	@Override
	public List<ProductOrder> getAllOrders() {
		return orderRepository.findAll();
	}

	@Override
	public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return orderRepository.findAll(pageable);

	}

	@Override
	public ProductOrder getOrdersByOrderId(String orderId) {
		return orderRepository.findByOrderId(orderId);
	}

	@Override
	public Page<ProductOrder> getOrdersBySeller(Integer sellerId, Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return orderRepository.findByProductSellerId(sellerId, pageable);
	}

	private ProductOrder buildAndSaveOrder(Integer userId, Product product, Integer quantity, OrderRequest orderRequest) {
		ProductOrder order = new ProductOrder();

		order.setOrderId(UUID.randomUUID().toString());
		order.setOrderDate(LocalDate.now());
		order.setProduct(product);
		order.setPrice(product.getDiscountPrice());
		order.setCostPrice(product.getCostPrice());
		order.setQuantity(quantity == null || quantity < 1 ? 1 : quantity);
		order.setUser(userRepository.findById(userId).orElse(null));
		order.setStatus(OrderStatus.IN_PROGRESS.getName());
		order.setPaymentType(orderRequest.getPaymentType());
		order.setOrderAddress(buildOrderAddress(orderRequest));

		return orderRepository.save(order);
	}

	private OrderAddress buildOrderAddress(OrderRequest orderRequest) {
		OrderAddress address = new OrderAddress();
		address.setFirstName(orderRequest.getFirstName());
		address.setLastName(orderRequest.getLastName());
		address.setEmail(orderRequest.getEmail());
		address.setMobileNo(orderRequest.getMobileNo());
		address.setAddress(orderRequest.getAddress());
		address.setCity(orderRequest.getCity());
		address.setState(orderRequest.getState());
		address.setPincode(orderRequest.getPincode());
		return address;
	}

	private void sendOrderMailSafely(ProductOrder order) {
		try {
			commonUtil.sendMailForProductOrder(order, "success");
		} catch (Exception ex) {
			log.warn("Order {} was placed but confirmation email could not be sent: {}", order.getOrderId(), ex.getMessage());
		}
	}

}
