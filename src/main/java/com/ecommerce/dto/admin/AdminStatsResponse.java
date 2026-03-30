package com.ecommerce.dto.admin;

public record AdminStatsResponse(
		long totalUsers,
		long totalSellers,
		long totalProducts,
		long totalOrders,
		double totalRevenue) {
}
