package com.ecommerce.service;

import java.io.IOException;
import java.util.Map;

public interface PaymentGatewayService {

	boolean isConfigured();

	String getPublicKey();

	PaymentOrder createOrder(PaymentOrderRequest request) throws IOException, InterruptedException;

	PaymentVerificationResult verifyPayment(String gatewayOrderId, String paymentId, String signature)
			throws IOException, InterruptedException;

	record PaymentOrderRequest(int amountInPaise, String currency, String receipt, Map<String, String> notes) {
	}

	record PaymentOrder(String gatewayOrderId, int amountInPaise, String currency, String status) {
	}

	record PaymentVerificationResult(boolean success, String gatewayOrderId, String gatewayPaymentId,
			String paymentStatus, String gatewayProvider, String payerUpiId, String failureCode, String failureReason) {
	}
}
