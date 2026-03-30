package com.ecommerce.dto.admin;

import java.util.List;

public record PaginatedResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {
}
