package com.ecommerce.service;

import com.ecommerce.model.OtpChannel;
import com.ecommerce.model.OtpPurpose;

public interface OtpService {

	OtpDispatchResult sendOtp(OtpPurpose purpose, OtpChannel channel, String destination, String recipientName);

	void verifyOtp(OtpPurpose purpose, OtpChannel channel, String destination, String otp);

	void assertVerified(OtpPurpose purpose, OtpChannel channel, String destination);

	void consumeVerified(OtpPurpose purpose, OtpChannel channel, String destination);

	record OtpDispatchResult(String message, String maskedDestination, long resendAfterSeconds, long expiresInMinutes,
			String deliveryMode, String previewOtp) {
	}
}
