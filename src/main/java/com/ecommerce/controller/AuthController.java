package com.ecommerce.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.config.CustomUser;
import com.ecommerce.model.UserDtls;
import com.ecommerce.service.UserService;

@RestController
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/auth/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> data) {
        String email = data.get("email");
        String name = data.get("name");
        String profileImage = data.get("profileImage");

        UserDtls user = userService.getUserByEmail(email);

        if (user == null) {
            // Register new user
            user = new UserDtls();
            user.setName(name);
            user.setEmail(email);
            user.setProfileImage(profileImage);
            user.setRole("ROLE_USER");
            user.setIsEnable(true);
            user.setAccountNonLocked(true);
            user.setFailedAttempt(0);
            // Default password for Google users (won't be used for login)
            user.setPassword("GOOGLE_AUTH_USER"); 
            user = userService.saveUser(user);
        }

        // Manually Authenticate in Spring Security Context
        UserDetails userDetails = new CustomUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return ResponseEntity.ok(Map.of("message", "Login successful", "success", true));
    }
}
