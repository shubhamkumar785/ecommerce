package com.ecommerce.config;

import java.util.Locale;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomAuthenticationProvider extends DaoAuthenticationProvider {

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        // Run standard checks (password validation)
        super.additionalAuthenticationChecks(userDetails, authentication);
        
        // After password is confirmed correct, fetch the requested role
        Object details = authentication.getDetails();
        
        if (details instanceof CustomWebAuthenticationDetails) {
            String requestedRole = normalizeRole(((CustomWebAuthenticationDetails) details).getRole());
            
            if (requestedRole != null && !requestedRole.isEmpty()) {
                // Determine the true role of the loaded user
                String actualRole = userDetails.getAuthorities().isEmpty() 
                                    ? "" 
                                    : normalizeRole(userDetails.getAuthorities().iterator().next().getAuthority());
                
                // If it does not match, throw exception to reject login
                if (!requestedRole.equals(actualRole)) {
                    throw new BadCredentialsException("Invalid role selected for this account!");
                }
            }
        }
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (normalizedRole.isEmpty()) {
            return normalizedRole;
        }

        return normalizedRole.startsWith("ROLE_") ? normalizedRole : "ROLE_" + normalizedRole;
    }
}
