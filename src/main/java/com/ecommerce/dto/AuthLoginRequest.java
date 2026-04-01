package com.ecommerce.dto;

public record AuthLoginRequest(String email, String password, String role) {
}
