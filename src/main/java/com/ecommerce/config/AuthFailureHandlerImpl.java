package com.ecommerce.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.ecommerce.model.UserDtls;
import com.ecommerce.service.UserService;
import com.ecommerce.util.AppConstant;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthFailureHandlerImpl extends SimpleUrlAuthenticationFailureHandler {

	@Autowired
	private UserService userService;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		String selectedRole = request.getParameter("role");

		String email = request.getParameter("username");

		UserDtls userDtls = userService.getUserByEmail(email);

		if ("Invalid role selected for this account!".equals(exception.getMessage())) {
			super.setDefaultFailureUrl(buildFailureUrl(selectedRole));
			super.onAuthenticationFailure(request, response, exception);
			return;
		}

		if (userDtls != null) {
			boolean isEnabled = !Boolean.FALSE.equals(userDtls.getIsEnable());
			boolean isAccountNonLocked = !Boolean.FALSE.equals(userDtls.getAccountNonLocked());
			int failedAttempt = userDtls.getFailedAttempt() == null ? 0 : userDtls.getFailedAttempt();

			if (isEnabled) {

				if (isAccountNonLocked) {

					if (failedAttempt < AppConstant.ATTEMPT_TIME) {
						userService.increaseFailedAttempt(userDtls);
					} else {
						userService.userAccountLock(userDtls);
						exception = new LockedException("Your account is locked !! failed attempt 3");
					}
				} else {

					if (userService.unlockAccountTimeExpired(userDtls)) {
						exception = new LockedException("Your account is unlocked !! Please try to login");
					} else {
						exception = new LockedException("your account is Locked !! Please try after sometimes");
					}
				}

			} else {
				exception = new LockedException("your account is inactive");
			}
		} else {
			exception = new LockedException("Email & password invalid");
		}

		super.setDefaultFailureUrl(buildFailureUrl(selectedRole));
		super.onAuthenticationFailure(request, response, exception);
	}

	private String buildFailureUrl(String selectedRole) {
		String failureUrl = "/signin?error";
		if (selectedRole != null && !selectedRole.isBlank()) {
			failureUrl += "&role=" + URLEncoder.encode(selectedRole, StandardCharsets.UTF_8);
		}
		return failureUrl;
	}

}
