package com.ecommerce.service;

import com.ecommerce.dto.admin.AdminStatsResponse;
import com.ecommerce.dto.admin.AdminUserRowResponse;
import com.ecommerce.dto.admin.ChartPointResponse;
import com.ecommerce.dto.admin.PaginatedResponse;
import com.ecommerce.dto.admin.StateCountResponse;

import java.util.List;

public interface AdminDashboardService {

	AdminStatsResponse getStats();

	List<StateCountResponse> getUsersByState();

	List<ChartPointResponse> getMonthlyRevenue();

	List<ChartPointResponse> getUserGrowth();

	List<ChartPointResponse> getOrderGrowth();

	PaginatedResponse<AdminUserRowResponse> getUsers(String role, String search, Integer page, Integer size);

	boolean updateUserStatus(Integer userId, boolean enabled, String currentAdminEmail);

	void deleteUser(Integer userId, String currentAdminEmail);

	byte[] exportUsersCsv(String role, String search);
}
