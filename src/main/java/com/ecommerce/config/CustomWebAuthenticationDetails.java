package com.ecommerce.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class CustomWebAuthenticationDetails extends WebAuthenticationDetails {

    private final String role;

    public CustomWebAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.role = request.getParameter("role");
    }

    public String getRole() {
        return role;
    }
}
