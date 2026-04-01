package com.ecommerce.dto;

public record OtpVerifyRequest(String channel, String purpose, String destination, String otp) {
}
