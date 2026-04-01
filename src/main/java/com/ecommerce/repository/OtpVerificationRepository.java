package com.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.model.OtpChannel;
import com.ecommerce.model.OtpPurpose;
import com.ecommerce.model.OtpStatus;
import com.ecommerce.model.OtpVerification;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

	Optional<OtpVerification> findTopByPurposeAndChannelAndDestinationOrderByRequestedAtDesc(OtpPurpose purpose,
			OtpChannel channel, String destination);

	Optional<OtpVerification> findTopByPurposeAndChannelAndDestinationAndStatusOrderByRequestedAtDesc(
			OtpPurpose purpose, OtpChannel channel, String destination, OtpStatus status);
}
