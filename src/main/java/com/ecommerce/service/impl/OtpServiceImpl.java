package com.ecommerce.service.impl;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecommerce.model.OtpChannel;
import com.ecommerce.model.OtpPurpose;
import com.ecommerce.model.OtpStatus;
import com.ecommerce.model.OtpVerification;
import com.ecommerce.repository.OtpVerificationRepository;
import com.ecommerce.service.OtpService;
import com.ecommerce.service.UserService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@Transactional
public class OtpServiceImpl implements OtpService {

	private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int MAX_ATTEMPTS = 5;

	@Autowired
	private OtpVerificationRepository otpVerificationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private UserService userService;

	@Value("${app.otp.length:6}")
	private int otpLength;

	@Value("${app.otp.expiry-minutes:10}")
	private long otpExpiryMinutes;

	@Value("${app.otp.resend-cooldown-seconds:30}")
	private long resendCooldownSeconds;

	@Value("${app.otp.verified-window-minutes:15}")
	private long verifiedWindowMinutes;

	@Value("${app.otp.email.from:${spring.mail.username:}}")
	private String otpFromEmail;

	@Value("${app.otp.email.from-name:Ecom Support}")
	private String otpFromName;

	@Value("${app.otp.sms.provider:twilio}")
	private String smsProvider;

	@Value("${app.otp.sms.twilio.account-sid:}")
	private String twilioAccountSid;

	@Value("${app.otp.sms.twilio.auth-token:}")
	private String twilioAuthToken;

	@Value("${app.otp.sms.twilio.from-number:}")
	private String twilioFromNumber;

	@Value("${app.otp.preview-enabled:true}")
	private boolean otpPreviewEnabled;

	@Value("${app.otp.sms.mock-enabled:true}")
	private boolean smsMockEnabled;

	@Value("${app.otp.email.mock-enabled:true}")
	private boolean emailMockEnabled;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Override
	public OtpDispatchResult sendOtp(OtpPurpose purpose, OtpChannel channel, String destination, String recipientName) {
		String normalizedDestination = normalizeDestination(channel, destination);
		validateAvailability(purpose, channel, normalizedDestination);

		LocalDateTime now = LocalDateTime.now();
		otpVerificationRepository.findTopByPurposeAndChannelAndDestinationOrderByRequestedAtDesc(purpose, channel,
				normalizedDestination).ifPresent(existing -> {
			if (existing.getRequestedAt() != null
					&& existing.getRequestedAt().plusSeconds(resendCooldownSeconds).isAfter(now)) {
				long remaining = now.until(existing.getRequestedAt().plusSeconds(resendCooldownSeconds),
						java.time.temporal.ChronoUnit.SECONDS);
				throw new IllegalStateException("Please wait " + Math.max(1, remaining) + " seconds before requesting another OTP.");
			}
		});

		String otp = generateOtp();
		OtpVerification otpVerification = new OtpVerification();
		otpVerification.setPurpose(purpose);
		otpVerification.setChannel(channel);
		otpVerification.setStatus(OtpStatus.PENDING);
		otpVerification.setDestination(normalizedDestination);
		otpVerification.setOtpHash(passwordEncoder.encode(otp));
		otpVerification.setAttemptCount(0);
		otpVerification.setRequestedAt(now);
		otpVerification.setExpiresAt(now.plusMinutes(otpExpiryMinutes));
		otpVerificationRepository.save(otpVerification);

		DeliveryResult deliveryResult = dispatchOtp(channel, normalizedDestination, recipientName, otp);
		return new OtpDispatchResult(buildDispatchMessage(channel, deliveryResult.deliveryMode()),
				maskDestination(channel, normalizedDestination),
				resendCooldownSeconds, otpExpiryMinutes, deliveryResult.deliveryMode(), deliveryResult.previewOtp());
	}

	@Override
	public void verifyOtp(OtpPurpose purpose, OtpChannel channel, String destination, String otp) {
		String normalizedDestination = normalizeDestination(channel, destination);
		if (!StringUtils.hasText(otp)) {
			throw new IllegalStateException("Please enter the OTP.");
		}

		OtpVerification otpVerification = otpVerificationRepository
				.findTopByPurposeAndChannelAndDestinationOrderByRequestedAtDesc(purpose, channel, normalizedDestination)
				.orElseThrow(() -> new IllegalStateException("OTP has not been requested yet."));

		if (otpVerification.getStatus() == OtpStatus.VERIFIED && otpVerification.getVerifiedAt() != null
				&& otpVerification.getVerifiedAt().plusMinutes(verifiedWindowMinutes).isAfter(LocalDateTime.now())) {
			return;
		}

		if (otpVerification.getStatus() == OtpStatus.CONSUMED) {
			throw new IllegalStateException("OTP has already been used. Please request a new one.");
		}

		if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
			otpVerification.setStatus(OtpStatus.EXPIRED);
			otpVerificationRepository.save(otpVerification);
			throw new IllegalStateException("OTP expired. Please request a new one.");
		}

		if (!passwordEncoder.matches(otp.trim(), otpVerification.getOtpHash())) {
			int attempts = otpVerification.getAttemptCount() + 1;
			otpVerification.setAttemptCount(attempts);
			if (attempts >= MAX_ATTEMPTS) {
				otpVerification.setStatus(OtpStatus.EXPIRED);
			}
			otpVerificationRepository.save(otpVerification);
			throw new IllegalStateException(attempts >= MAX_ATTEMPTS
					? "Too many invalid attempts. Please request a new OTP."
					: "Invalid OTP. Please try again.");
		}

		otpVerification.setStatus(OtpStatus.VERIFIED);
		otpVerification.setVerifiedAt(LocalDateTime.now());
		otpVerificationRepository.save(otpVerification);
	}

	@Override
	@Transactional(readOnly = true)
	public void assertVerified(OtpPurpose purpose, OtpChannel channel, String destination) {
		String normalizedDestination = normalizeDestination(channel, destination);
		OtpVerification otpVerification = otpVerificationRepository
				.findTopByPurposeAndChannelAndDestinationAndStatusOrderByRequestedAtDesc(purpose, channel,
						normalizedDestination, OtpStatus.VERIFIED)
				.orElseThrow(() -> new IllegalStateException(channel == OtpChannel.EMAIL
						? "Please verify your email OTP before continuing."
						: "Please verify your mobile OTP before continuing."));

		if (otpVerification.getVerifiedAt() == null
				|| otpVerification.getVerifiedAt().plusMinutes(verifiedWindowMinutes).isBefore(LocalDateTime.now())) {
			throw new IllegalStateException(channel == OtpChannel.EMAIL
					? "Email OTP verification expired. Please verify again."
					: "Mobile OTP verification expired. Please verify again.");
		}
	}

	@Override
	public void consumeVerified(OtpPurpose purpose, OtpChannel channel, String destination) {
		String normalizedDestination = normalizeDestination(channel, destination);
		otpVerificationRepository.findTopByPurposeAndChannelAndDestinationAndStatusOrderByRequestedAtDesc(purpose,
				channel, normalizedDestination, OtpStatus.VERIFIED).ifPresent(otpVerification -> {
			otpVerification.setStatus(OtpStatus.CONSUMED);
			otpVerificationRepository.save(otpVerification);
		});
	}

	private void validateAvailability(OtpPurpose purpose, OtpChannel channel, String destination) {
		if (purpose == OtpPurpose.SELLER_SIGNUP && channel == OtpChannel.EMAIL && userService.existsEmail(destination)) {
			throw new IllegalStateException("This email is already registered.");
		}
	}

	private DeliveryResult dispatchOtp(OtpChannel channel, String destination, String recipientName, String otp) {
		if (channel == OtpChannel.EMAIL) {
			return sendEmailOtp(destination, recipientName, otp);
		}

		return sendSmsOtp(destination, otp);
	}

	private DeliveryResult sendEmailOtp(String recipientEmail, String recipientName, String otp) {
		if (shouldUseMockEmail()) {
			return buildMockDelivery("mock-email", recipientEmail, otp);
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message);
			String safeName = StringUtils.hasText(recipientName) ? recipientName.trim() : "Seller";

			helper.setFrom(otpFromEmail, otpFromName);
			helper.setTo(recipientEmail);
			helper.setSubject("Your Ecom verification OTP");
			helper.setText(buildEmailContent(safeName, otp), true);
			mailSender.send(message);
			return new DeliveryResult("email", null);
		} catch (MessagingException | UnsupportedEncodingException ex) {
			if (emailMockEnabled) {
				log.warn("Email OTP send failed. Falling back to mock delivery: {}", ex.getMessage());
				return buildMockDelivery("mock-email", recipientEmail, otp);
			}
			throw new IllegalStateException("Unable to send email OTP right now. Please check your mail configuration.", ex);
		}
	}

	private String buildEmailContent(String recipientName, String otp) {
		return "<div style='font-family:Arial,sans-serif;color:#1f2937;line-height:1.6;'>"
				+ "<h2 style='margin-bottom:8px;'>Verify your seller account</h2>"
				+ "<p>Hello " + escapeHtml(recipientName) + ",</p>"
				+ "<p>Use the OTP below to continue your seller registration:</p>"
				+ "<div style='font-size:28px;font-weight:700;letter-spacing:8px;background:#eff6ff;padding:16px 20px;border-radius:12px;display:inline-block;'>"
				+ otp + "</div>"
				+ "<p style='margin-top:16px;'>This OTP expires in " + otpExpiryMinutes + " minutes.</p>"
				+ "<p>If you did not request this code, you can ignore this email.</p>" + "</div>";
	}

	private DeliveryResult sendSmsOtp(String mobileNumber, String otp) {
		if ("mock".equalsIgnoreCase(smsProvider)) {
			return buildMockDelivery("mock-sms", mobileNumber, otp);
		}

		if (!StringUtils.hasText(twilioAccountSid) || !StringUtils.hasText(twilioAuthToken)
				|| !StringUtils.hasText(twilioFromNumber)) {
			if (smsMockEnabled) {
				return buildMockDelivery("mock-sms", mobileNumber, otp);
			}
			throw new IllegalStateException("Twilio credentials are missing. Please configure account SID, auth token, and from number.");
		}

		if (!"twilio".equalsIgnoreCase(smsProvider)) {
			if (smsMockEnabled) {
				return buildMockDelivery("mock-sms", mobileNumber, otp);
			}
			throw new IllegalStateException(
					"SMS provider is not configured. Set app.otp.sms.provider=twilio and configure Twilio credentials.");
		}

		Map<String, String> formData = Map.of("To", mobileNumber, "From", twilioFromNumber,
				"Body", "Your Ecom OTP is " + otp + ". It is valid for " + otpExpiryMinutes + " minutes.");
		String form = formData.entrySet().stream()
				.map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
				.reduce((left, right) -> left + "&" + right).orElse("");
		String credentials = Base64.getEncoder()
				.encodeToString((twilioAccountSid + ":" + twilioAuthToken).getBytes(StandardCharsets.UTF_8));

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json"))
				.header("Authorization", "Basic " + credentials)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(form)).build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IllegalStateException("SMS provider rejected the OTP request. Response code: " + response.statusCode());
			}
			return new DeliveryResult("sms", null);
		} catch (Exception ex) {
			if (smsMockEnabled) {
				log.warn("SMS OTP send failed. Falling back to mock delivery: {}", ex.getMessage());
				return buildMockDelivery("mock-sms", mobileNumber, otp);
			}
			throw new IllegalStateException("Unable to send SMS OTP right now. Please check your SMS configuration.", ex);
		}
	}

	private boolean shouldUseMockEmail() {
		String normalizedFromEmail = otpFromEmail == null ? "" : otpFromEmail.trim().toLowerCase();
		return !StringUtils.hasText(normalizedFromEmail) || normalizedFromEmail.contains("yourmail@gmail.com")
				|| normalizedFromEmail.contains("example.com");
	}

	private DeliveryResult buildMockDelivery(String deliveryMode, String destination, String otp) {
		log.info("Development OTP for {} [{}]: {}", deliveryMode, destination, otp);
		return new DeliveryResult(deliveryMode, otpPreviewEnabled ? otp : null);
	}

	private String buildDispatchMessage(OtpChannel channel, String deliveryMode) {
		if (deliveryMode != null && deliveryMode.startsWith("mock-")) {
			return channel == OtpChannel.EMAIL
					? "Email OTP generated in development mode. Check the server log."
					: "Mobile OTP generated in development mode. Check the server log.";
		}

		return channel == OtpChannel.EMAIL ? "OTP sent to your email address." : "OTP sent to your mobile number.";
	}

	private String normalizeDestination(OtpChannel channel, String destination) {
		if (!StringUtils.hasText(destination)) {
			throw new IllegalStateException(channel == OtpChannel.EMAIL ? "Email address is required." : "Mobile number is required.");
		}

		if (channel == OtpChannel.EMAIL) {
			String normalizedEmail = destination.trim().toLowerCase();
			if (!normalizedEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
				throw new IllegalStateException("Please enter a valid email address.");
			}
			return normalizedEmail;
		}

		String digits = destination.replaceAll("[^0-9]", "");
		if (digits.length() == 10) {
			return "+91" + digits;
		}
		if (digits.length() == 12 && digits.startsWith("91")) {
			return "+" + digits;
		}
		if (digits.length() >= 11 && destination.trim().startsWith("+")) {
			return "+" + digits;
		}
		throw new IllegalStateException("Please enter a valid mobile number.");
	}

	private String generateOtp() {
		int bound = (int) Math.pow(10, otpLength);
		int start = (int) Math.pow(10, otpLength - 1);
		return String.valueOf(start + RANDOM.nextInt(bound - start));
	}

	private String maskDestination(OtpChannel channel, String destination) {
		if (channel == OtpChannel.EMAIL) {
			String[] parts = destination.split("@");
			if (parts.length != 2) {
				return destination;
			}
			String local = parts[0];
			String maskedLocal = local.length() <= 2 ? local.charAt(0) + "*" : local.substring(0, 2) + "***";
			return maskedLocal + "@" + parts[1];
		}

		if (destination.length() < 4) {
			return destination;
		}
		return "*".repeat(Math.max(0, destination.length() - 4)) + destination.substring(destination.length() - 4);
	}

	private String urlEncode(String value) {
		return URLEncoder.encode(Objects.toString(value, ""), StandardCharsets.UTF_8);
	}

	private String escapeHtml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private record DeliveryResult(String deliveryMode, String previewOtp) {
	}
}
