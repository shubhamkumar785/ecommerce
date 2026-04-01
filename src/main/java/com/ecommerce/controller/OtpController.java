package com.ecommerce.controller;

import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.OtpSendRequest;
import com.ecommerce.dto.OtpVerifyRequest;
import com.ecommerce.model.OtpChannel;
import com.ecommerce.model.OtpPurpose;
import com.ecommerce.service.OtpService;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

	@Autowired
	private OtpService otpService;

	@PostMapping("/send")
	public ResponseEntity<?> sendOtp(@RequestBody OtpSendRequest request) {
		try {
			OtpService.OtpDispatchResult response = otpService.sendOtp(parsePurpose(request.purpose()),
					parseChannel(request.channel()), request.destination(), request.name());
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("message", response.message());
			body.put("maskedDestination", response.maskedDestination());
			body.put("resendAfterSeconds", response.resendAfterSeconds());
			body.put("expiresInMinutes", response.expiresInMinutes());
			body.put("deliveryMode", response.deliveryMode());
			body.put("previewOtp", response.previewOtp());
			body.put("success", true);
			return ResponseEntity.ok(body);
		} catch (IllegalStateException ex) {
			return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage(), "success", false));
		}
	}

	@PostMapping("/verify")
	public ResponseEntity<?> verifyOtp(@RequestBody OtpVerifyRequest request) {
		try {
			otpService.verifyOtp(parsePurpose(request.purpose()), parseChannel(request.channel()),
					request.destination(), request.otp());
			return ResponseEntity.ok(Map.of("message", "OTP verified successfully.", "success", true));
		} catch (IllegalStateException ex) {
			return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage(), "success", false));
		}
	}

	private OtpChannel parseChannel(String channel) {
		try {
			return OtpChannel.valueOf(channel.trim().toUpperCase(Locale.ROOT));
		} catch (Exception ex) {
			throw new IllegalStateException("Unsupported OTP channel.");
		}
	}

	private OtpPurpose parsePurpose(String purpose) {
		try {
			return OtpPurpose.valueOf(purpose.trim().toUpperCase(Locale.ROOT));
		} catch (Exception ex) {
			throw new IllegalStateException("Unsupported OTP purpose.");
		}
	}
}
