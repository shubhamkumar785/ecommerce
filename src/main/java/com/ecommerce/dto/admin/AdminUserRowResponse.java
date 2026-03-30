package com.ecommerce.dto.admin;

import java.time.LocalDateTime;

public record AdminUserRowResponse(
		Integer id,
		String name,
		String email,
		String role,
		String state,
		boolean enabled,
		LocalDateTime createdAt) {
}
