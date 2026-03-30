package com.ecommerce.service.impl;

import com.ecommerce.dto.admin.AdminStatsResponse;
import com.ecommerce.dto.admin.AdminUserRowResponse;
import com.ecommerce.dto.admin.ChartPointResponse;
import com.ecommerce.dto.admin.PaginatedResponse;
import com.ecommerce.dto.admin.StateCountResponse;
import com.ecommerce.model.UserDtls;
import com.ecommerce.repository.ProductOrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.AdminDashboardService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

	private static final String ROLE_USER = "ROLE_USER";
	private static final String ROLE_SELLER = "ROLE_SELLER";
	private static final String ROLE_ADMIN = "ROLE_ADMIN";
	private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
	private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductOrderRepository productOrderRepository;

	@Override
	public AdminStatsResponse getStats() {
		BigDecimal totalRevenue = productOrderRepository.calculateTotalRevenue();
		return new AdminStatsResponse(
				userRepository.countByRole(ROLE_USER),
				userRepository.countByRole(ROLE_SELLER),
				productRepository.count(),
				productOrderRepository.count(),
				totalRevenue == null ? 0.0 : totalRevenue.doubleValue());
	}

	@Override
	public List<StateCountResponse> getUsersByState() {
		return userRepository.countUsersByState().stream()
				.map(row -> new StateCountResponse(valueAsString(row[0], "Unknown"), valueAsLong(row[1])))
				.toList();
	}

	@Override
	public List<ChartPointResponse> getMonthlyRevenue() {
		return normalizeMonthlySeries(productOrderRepository.sumMonthlyRevenue(), 6);
	}

	@Override
	public List<ChartPointResponse> getUserGrowth() {
		return normalizeMonthlySeries(userRepository.countUserGrowth(), 6);
	}

	@Override
	public List<ChartPointResponse> getOrderGrowth() {
		return normalizeMonthlySeries(productOrderRepository.countMonthlyOrders(), 6);
	}

	@Override
	public PaginatedResponse<AdminUserRowResponse> getUsers(String role, String search, Integer page, Integer size) {
		int safePage = page == null || page < 0 ? 0 : page;
		int safeSize = size == null || size < 1 ? 10 : Math.min(size, 50);
		String normalizedRole = normalizeRole(role);
		String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;

		Page<UserDtls> resultPage = userRepository.searchByRole(normalizedRole, normalizedSearch,
				PageRequest.of(safePage, safeSize));

		List<AdminUserRowResponse> rows = resultPage.getContent().stream()
				.map(user -> new AdminUserRowResponse(
						user.getId(),
						valueAsString(user.getName(), "Unknown"),
						valueAsString(user.getEmail(), "-"),
						normalizeRole(user.getRole()),
						valueAsString(user.getState(), "Unknown"),
						!Boolean.FALSE.equals(user.getIsEnable()),
						user.getCreatedAt()))
				.toList();

		return new PaginatedResponse<>(
				rows,
				resultPage.getNumber(),
				resultPage.getSize(),
				resultPage.getTotalElements(),
				resultPage.getTotalPages(),
				resultPage.isFirst(),
				resultPage.isLast());
	}

	@Override
	public boolean updateUserStatus(Integer userId, boolean enabled, String currentAdminEmail) {
		UserDtls targetUser = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		if (StringUtils.hasText(currentAdminEmail)
				&& currentAdminEmail.equalsIgnoreCase(targetUser.getEmail())
				&& !enabled) {
			throw new IllegalArgumentException("You cannot block your own admin account");
		}

		targetUser.setIsEnable(enabled);
		userRepository.save(targetUser);
		return true;
	}

	@Override
	public void deleteUser(Integer userId, String currentAdminEmail) {
		UserDtls targetUser = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		if (ROLE_ADMIN.equals(normalizeRole(targetUser.getRole()))) {
			throw new IllegalArgumentException("Admin accounts cannot be deleted from this screen");
		}

		if (StringUtils.hasText(currentAdminEmail) && currentAdminEmail.equalsIgnoreCase(targetUser.getEmail())) {
			throw new IllegalArgumentException("You cannot delete your own account");
		}

		try {
			userRepository.delete(targetUser);
		} catch (DataIntegrityViolationException ex) {
			throw new IllegalStateException(
					"User cannot be deleted because related orders, products, or addresses still exist", ex);
		}
	}

	@Override
	public byte[] exportUsersCsv(String role, String search) {
		Page<UserDtls> resultPage = userRepository.searchByRole(normalizeRole(role),
				StringUtils.hasText(search) ? search.trim() : null,
				org.springframework.data.domain.Pageable.unpaged());

		StringBuilder csv = new StringBuilder();
		csv.append("Id,Name,Email,Role,State,Status,Created At").append('\n');

		for (UserDtls user : resultPage.getContent()) {
			csv.append(user.getId()).append(',')
					.append(escapeCsv(valueAsString(user.getName(), ""))).append(',')
					.append(escapeCsv(valueAsString(user.getEmail(), ""))).append(',')
					.append(escapeCsv(normalizeRole(user.getRole()))).append(',')
					.append(escapeCsv(valueAsString(user.getState(), "Unknown"))).append(',')
					.append(escapeCsv(!Boolean.FALSE.equals(user.getIsEnable()) ? "Active" : "Blocked")).append(',')
					.append(escapeCsv(user.getCreatedAt() == null ? "" : user.getCreatedAt().toString()))
					.append('\n');
		}

		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private List<ChartPointResponse> normalizeMonthlySeries(List<Object[]> rawRows, int monthCount) {
		Map<String, Double> dataByMonth = new LinkedHashMap<>();
		for (Object[] row : rawRows) {
			dataByMonth.put(valueAsString(row[0], ""), valueAsDouble(row[1]));
		}

		YearMonth currentMonth = YearMonth.now();
		List<ChartPointResponse> series = new ArrayList<>();
		for (int index = monthCount - 1; index >= 0; index--) {
			YearMonth month = currentMonth.minusMonths(index);
			String monthKey = month.format(MONTH_KEY);
			series.add(new ChartPointResponse(
					month.format(MONTH_LABEL),
					dataByMonth.getOrDefault(monthKey, 0.0)));
		}
		return series;
	}

	private String normalizeRole(String role) {
		if (!StringUtils.hasText(role)) {
			return null;
		}
		String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
		return normalizedRole.startsWith("ROLE_") ? normalizedRole : "ROLE_" + normalizedRole;
	}

	private String escapeCsv(String value) {
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	private String valueAsString(Object value, String fallback) {
		if (value == null) {
			return fallback;
		}
		String text = value.toString().trim();
		return text.isEmpty() ? fallback : text;
	}

	private long valueAsLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		return value == null ? 0 : Long.parseLong(value.toString());
	}

	private double valueAsDouble(Object value) {
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		return value == null ? 0.0 : Double.parseDouble(value.toString());
	}
}
