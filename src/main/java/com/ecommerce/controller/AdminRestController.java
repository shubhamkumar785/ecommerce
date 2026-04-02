package com.ecommerce.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.DashboardStats;
import com.ecommerce.model.UserDtls;
import com.ecommerce.repository.ProductOrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.DashboardService;
import com.ecommerce.service.UserService;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

	@Autowired
	private DashboardService dashboardService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductOrderRepository orderRepository;

	@Autowired
	private UserService userService;

	@GetMapping("/stats")
	public ResponseEntity<Map<String, Object>> getStats() {
		DashboardStats stats = dashboardService.getGlobalStats();
		Map<String, Object> response = new HashMap<>();
		response.put("totalUsers", userRepository.countByRole("ROLE_USER"));
		response.put("totalSellers", userRepository.countByRole("ROLE_SELLER"));
		response.put("totalProducts", productRepository.count());
		response.put("totalOrders", stats.getTotalOrders());
		response.put("totalRevenue", stats.getTotalRevenue());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/users-by-state")
	public ResponseEntity<List<Map<String, Object>>> getUsersByState() {
		List<Object[]> results = userRepository.countUsersByState();
		List<Map<String, Object>> response = results.stream().map(row -> {
			Map<String, Object> map = new HashMap<>();
			map.put("state", row[0]);
			map.put("count", row[1]);
			return map;
		}).collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/monthly-revenue")
	public ResponseEntity<List<Map<String, Object>>> getMonthlyRevenue() {
		List<Object[]> results = orderRepository.sumMonthlyRevenue();
		return ResponseEntity.ok(mapChartData(results));
	}

	@GetMapping("/user-growth")
	public ResponseEntity<List<Map<String, Object>>> getUserGrowth() {
		List<Object[]> results = userRepository.countUserGrowth();
		return ResponseEntity.ok(mapChartData(results));
	}

	@GetMapping("/orders-growth")
	public ResponseEntity<List<Map<String, Object>>> getOrdersGrowth() {
		List<Object[]> results = orderRepository.countMonthlyOrders();
		return ResponseEntity.ok(mapChartData(results));
	}

	@GetMapping("/users")
	public ResponseEntity<Page<UserDtls>> getUsers(
			@RequestParam(required = false) String role,
			@RequestParam(defaultValue = "") String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(userRepository.searchByRole(role, search, pageable));
	}

	@PatchMapping("/users/{id}/status")
	public ResponseEntity<Void> toggleUserStatus(@PathVariable Integer id, @RequestParam boolean enabled) {
		UserDtls user = userRepository.findById(id).orElse(null);
		if (user != null) {
			user.setIsEnable(enabled);
			userRepository.save(user);
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}

	@DeleteMapping("/users/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
		if (userRepository.existsById(id)) {
			userRepository.deleteById(id);
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}

	private List<Map<String, Object>> mapChartData(List<Object[]> results) {
		return results.stream().map(row -> {
			Map<String, Object> map = new HashMap<>();
			map.put("label", row[0]);
			map.put("value", row[1]);
			return map;
		}).collect(Collectors.toList());
	}
}
