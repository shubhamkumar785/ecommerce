package com.ecommerce.dto.admin;

public record AuthLoginResponse(String token, String role, String name, String redirectUrl) {
}
