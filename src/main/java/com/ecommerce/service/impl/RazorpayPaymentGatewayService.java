package com.ecommerce.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ecommerce.service.PaymentGatewayService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RazorpayPaymentGatewayService implements PaymentGatewayService {

	private static final String RAZORPAY_API_BASE = "https://api.razorpay.com/v1";

	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	@Value("${app.payment.razorpay.key-id:}")
	private String keyId;

	@Value("${app.payment.razorpay.key-secret:}")
	private String keySecret;

	RazorpayPaymentGatewayService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newHttpClient();
	}

	@Override
	public boolean isConfigured() {
		return StringUtils.hasText(keyId) && StringUtils.hasText(keySecret);
	}

	@Override
	public String getPublicKey() {
		return keyId;
	}

	@Override
	public PaymentOrder createOrder(PaymentOrderRequest request) throws IOException, InterruptedException {
		ensureConfigured();

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("amount", request.amountInPaise());
		payload.put("currency", StringUtils.hasText(request.currency()) ? request.currency() : "INR");
		payload.put("receipt", request.receipt());
		payload.put("notes", request.notes() == null ? Map.of() : request.notes());

		HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(RAZORPAY_API_BASE + "/orders"))
				.header("Authorization", buildBasicAuthHeader())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
				.build();

		HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException(extractGatewayMessage(response.body(), "Unable to create Razorpay order"));
		}

		Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {
		});
		return new PaymentOrder(stringValue(body.get("id")), intValue(body.get("amount")),
				stringValue(body.get("currency")), stringValue(body.get("status")));
	}

	@Override
	public PaymentVerificationResult verifyPayment(String gatewayOrderId, String paymentId, String signature)
			throws IOException, InterruptedException {
		ensureConfigured();

		boolean signatureValid = verifySignature(gatewayOrderId, paymentId, signature);
		if (!signatureValid) {
			return new PaymentVerificationResult(false, gatewayOrderId, paymentId, "signature_failed", "RAZORPAY", null,
					"signature_verification_failed", "Payment signature verification failed");
		}

		HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(RAZORPAY_API_BASE + "/payments/" + paymentId))
				.header("Authorization", buildBasicAuthHeader())
				.GET()
				.build();

		HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException(extractGatewayMessage(response.body(), "Unable to fetch Razorpay payment details"));
		}

		Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {
		});
		String paymentStatus = stringValue(body.get("status"));
		String returnedOrderId = stringValue(body.get("order_id"));
		String failureCode = stringValue(body.get("error_code"));
		String failureReason = stringValue(body.get("error_description"));
		String payerUpiId = stringValue(body.get("vpa"));

		boolean orderMatches = gatewayOrderId.equals(returnedOrderId);
		boolean successfulStatus = "captured".equalsIgnoreCase(paymentStatus)
				|| "authorized".equalsIgnoreCase(paymentStatus);

		return new PaymentVerificationResult(signatureValid && orderMatches && successfulStatus, returnedOrderId,
				stringValue(body.get("id")), paymentStatus, "RAZORPAY", payerUpiId,
				StringUtils.hasText(failureCode) ? failureCode : (orderMatches ? null : "order_mismatch"),
				StringUtils.hasText(failureReason) ? failureReason
						: (orderMatches ? "Payment could not be verified as successful"
								: "Gateway payment does not belong to the initiated order"));
	}

	private boolean verifySignature(String gatewayOrderId, String paymentId, String signature) {
		if (!StringUtils.hasText(gatewayOrderId) || !StringUtils.hasText(paymentId) || !StringUtils.hasText(signature)) {
			return false;
		}

		try {
			String payload = gatewayOrderId + "|" + paymentId;
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString().equals(signature);
		} catch (Exception ex) {
			return false;
		}
	}

	private String buildBasicAuthHeader() {
		String token = Base64.getEncoder()
				.encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
		return "Basic " + token;
	}

	private void ensureConfigured() {
		if (!isConfigured()) {
			throw new IllegalStateException("Razorpay test credentials are not configured");
		}
	}

	private String extractGatewayMessage(String responseBody, String defaultMessage) {
		if (!StringUtils.hasText(responseBody)) {
			return defaultMessage;
		}

		try {
			Map<String, Object> body = objectMapper.readValue(responseBody, new TypeReference<>() {
			});
			Object errorNode = body.get("error");
			if (errorNode instanceof Map<?, ?> errorMap) {
				Object description = errorMap.get("description");
				if (description != null) {
					return description.toString();
				}
			}
		} catch (Exception ex) {
			return defaultMessage;
		}

		return defaultMessage;
	}

	private String stringValue(Object value) {
		return value == null ? "" : value.toString();
	}

	private int intValue(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		return 0;
	}
}
