package com.ecommerce.dto;

public record AuthLoginResponse(String token, String role, String name, String redirectUrl) {
}
