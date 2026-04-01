package com.ecommerce.dto;

public record OtpSendRequest(String channel, String purpose, String destination, String name) {
}
