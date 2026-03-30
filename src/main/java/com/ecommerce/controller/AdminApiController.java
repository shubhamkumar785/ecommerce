package com.ecommerce.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.admin.AdminStatsResponse;
import com.ecommerce.dto.admin.AdminUserRowResponse;
import com.ecommerce.dto.admin.ChartPointResponse;
import com.ecommerce.dto.admin.PaginatedResponse;
import com.ecommerce.dto.admin.StateCountResponse;
import com.ecommerce.service.AdminDashboardService;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

	@Autowired
	private AdminDashboardService adminDashboardService;

	@GetMapping("/stats")
	public AdminStatsResponse getStats() {
		return adminDashboardService.getStats();
	}

	@GetMapping("/users-by-state")
	public List<StateCountResponse> getUsersByState() {
		return adminDashboardService.getUsersByState();
	}

	@GetMapping("/monthly-revenue")
	public List<ChartPointResponse> getMonthlyRevenue() {
		return adminDashboardService.getMonthlyRevenue();
	}

	@GetMapping("/user-growth")
	public List<ChartPointResponse> getUserGrowth() {
		return adminDashboardService.getUserGrowth();
	}

	@GetMapping("/orders-growth")
	public List<ChartPointResponse> getOrdersGrowth() {
		return adminDashboardService.getOrderGrowth();
	}

	@GetMapping("/users")
	public PaginatedResponse<AdminUserRowResponse> getUsers(
			@RequestParam(defaultValue = "ROLE_USER") String role,
			@RequestParam(defaultValue = "") String search,
			@RequestParam(defaultValue = "0") Integer page,
			@RequestParam(defaultValue = "10") Integer size) {
		return adminDashboardService.getUsers(role, search, page, size);
	}

	@PatchMapping("/users/{userId}/status")
	public ResponseEntity<?> updateUserStatus(@PathVariable Integer userId, @RequestParam boolean enabled,
			Principal principal) {
		adminDashboardService.updateUserStatus(userId, enabled, principal == null ? null : principal.getName());
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/users/{userId}")
	public ResponseEntity<?> deleteUser(@PathVariable Integer userId, Principal principal) {
		adminDashboardService.deleteUser(userId, principal == null ? null : principal.getName());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/export/users.csv")
	public ResponseEntity<byte[]> exportUsers(
			@RequestParam(defaultValue = "ROLE_USER") String role,
			@RequestParam(defaultValue = "") String search) {
		byte[] payload = adminDashboardService.exportUsersCsv(role, search);
		String fileName = ("ROLE_SELLER".equalsIgnoreCase(role) ? "sellers" : "users") + "-export.csv";

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(fileName).build().toString())
				.contentType(MediaType.parseMediaType("text/csv"))
				.body(payload);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException exception) {
		return ResponseEntity.status(409).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleGenericError(Exception exception) {
		return ResponseEntity.internalServerError().body(Map.of("message", "Admin operation failed"));
	}
}
