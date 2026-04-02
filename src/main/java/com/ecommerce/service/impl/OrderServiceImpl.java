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
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.model.Cart;
import com.ecommerce.model.OrderAddress;
import com.ecommerce.model.OrderRequest;
import com.ecommerce.model.Product;
import com.ecommerce.model.ProductOrder;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
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
	private ProductRepository productRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CommonUtil commonUtil;

	@Override
	@Transactional
	public void saveOrder(Integer userid, OrderRequest orderRequest) throws Exception {

		List<Cart> carts = cartRepository.findByUserId(userid);

		for (Cart cart : carts) {
			ProductOrder saveOrder = buildAndSaveOrder(cart.getUser().getId(), cart.getProduct(), cart.getQuantity(), orderRequest);
			sendOrderMailSafely(saveOrder);
		}
	}

	@Override
	@Transactional
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
	@Transactional
	public ProductOrder updateOrderStatus(Integer id, String status) {
		Optional<ProductOrder> findById = orderRepository.findById(id);
		if (findById.isPresent()) {
			ProductOrder productOrder = findById.get();
			String oldStatus = productOrder.getStatus();
			productOrder.setStatus(status);
			ProductOrder updateOrder = orderRepository.save(productOrder);

			// If the order is cancelled, return the stock to the product
			if (OrderStatus.CANCEL.getName().equalsIgnoreCase(status) && !OrderStatus.CANCEL.getName().equalsIgnoreCase(oldStatus)) {
				returnStock(updateOrder.getProduct(), updateOrder.getQuantity());
			}

			return updateOrder;
		}
		return null;
	}

	private void returnStock(Product product, int quantity) {
		if (product == null || product.getId() == null) {
			return;
		}
		Product latestProduct = productRepository.findById(product.getId()).orElse(null);
		if (latestProduct != null) {
			latestProduct.setStock(latestProduct.getStock() + quantity);
			productRepository.save(latestProduct);
		}
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

	@Override
	public List<ProductOrder> getOrdersBySeller(Integer sellerId) {
		return orderRepository.findByProductSellerId(sellerId);
	}

	private ProductOrder buildAndSaveOrder(Integer userId, Product product, Integer quantity, OrderRequest orderRequest) {
		int orderQuantity = quantity == null || quantity < 1 ? 1 : quantity;
		Product reservedProduct = reserveStock(product, orderQuantity);
		ProductOrder order = new ProductOrder();

		order.setOrderId(UUID.randomUUID().toString());
		order.setOrderDate(LocalDate.now());
		order.setProduct(reservedProduct);
		order.setPrice(reservedProduct.getDiscountPrice());
		order.setCostPrice(reservedProduct.getCostPrice());
		order.setQuantity(orderQuantity);
		order.setUser(userRepository.findById(userId).orElse(null));
		order.setStatus(OrderStatus.IN_PROGRESS.getName());
		order.setPaymentType(orderRequest.getPaymentType());
		order.setPaymentStatus(resolvePaymentStatus(orderRequest));
		order.setPaymentGatewayProvider(orderRequest.getPaymentGatewayProvider());
		order.setPaymentGatewayOrderId(orderRequest.getPaymentGatewayOrderId());
		order.setPaymentGatewayPaymentId(orderRequest.getPaymentGatewayPaymentId());
		order.setPaymentFailureCode(orderRequest.getPaymentFailureCode());
		order.setPaymentFailureReason(orderRequest.getPaymentFailureReason());
		order.setPayerUpiId(orderRequest.getUpiId());
		order.setOrderAddress(buildOrderAddress(orderRequest));

		return orderRepository.save(order);
	}

	private Product reserveStock(Product product, int quantity) {
		if (product == null || product.getId() == null) {
			throw new IllegalStateException("This product is currently unavailable");
		}

		Product latestProduct = productRepository.findById(product.getId()).orElse(null);
		if (latestProduct == null || !Boolean.TRUE.equals(latestProduct.getIsActive())) {
			throw new IllegalStateException("This product is currently unavailable");
		}

		if (latestProduct.getStock() <= 0) {
			throw new IllegalStateException("Out of stock. We will notify you when available.");
		}

		if (latestProduct.getStock() < quantity) {
			throw new IllegalStateException("Only " + latestProduct.getStock() + " item(s) are available right now.");
		}

		latestProduct.setStock(latestProduct.getStock() - quantity);
		return productRepository.save(latestProduct);
	}

	private String resolvePaymentStatus(OrderRequest orderRequest) {
		if (orderRequest == null) {
			return "SUCCESS";
		}
		if (orderRequest.getPaymentStatus() != null && !orderRequest.getPaymentStatus().isBlank()) {
			return orderRequest.getPaymentStatus();
		}
		return "COD".equalsIgnoreCase(orderRequest.getPaymentType()) ? "PENDING" : "SUCCESS";
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
			commonUtil.sendOrderConfirmationEmail(order);
		} catch (Exception ex) {
			log.warn("Order {} was placed but confirmation email could not be sent: {}", order.getOrderId(), ex.getMessage());
		}
	}

}
