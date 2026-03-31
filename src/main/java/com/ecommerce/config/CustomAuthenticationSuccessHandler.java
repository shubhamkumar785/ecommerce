package com.ecommerce.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.ecommerce.model.UserDtls;
import com.ecommerce.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String AUTH_COOKIE = "SHOPPING_CART_TOKEN";

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        UserDtls user = userService.getUserByEmail(authentication.getName());
        if (user != null && user.getId() != null) {
            userService.resetAttempt(user.getId());
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
            ResponseCookie authCookie = ResponseCookie.from(AUTH_COOKIE, token)
                    .httpOnly(true)
                    .secure(request.isSecure())
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(jwtUtil.getExpirationSeconds())
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
        }

        // Default: customer
        response.sendRedirect("/");
    }
}
