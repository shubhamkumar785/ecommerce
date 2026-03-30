package com.ecommerce.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.config.CustomUser;
import com.ecommerce.config.JwtUtil;
import com.ecommerce.dto.admin.AuthLoginRequest;
import com.ecommerce.dto.admin.AuthLoginResponse;
import com.ecommerce.model.UserDtls;
import com.ecommerce.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.ClassPathResource;

@RestController
public class AuthController {

    private static final String AUTH_COOKIE = "SHOPPING_CART_TOKEN";

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody AuthLoginRequest request, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDtls user = userService.getUserByEmail(request.email());
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
            }

            String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
            servletResponse.addHeader(HttpHeaders.SET_COOKIE, buildCookie(token, servletRequest.isSecure()).toString());
            return ResponseEntity.ok(new AuthLoginResponse(token, user.getRole(), user.getName(), redirectFor(user)));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }
    }

    @PostMapping("/api/auth/signup")
    public ResponseEntity<?> signup(@ModelAttribute UserDtls user, @RequestParam(value = "img", required = false) MultipartFile file, HttpServletRequest request) {
        if (userService.existsEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
        }

        // Prevent setting Admin role from frontend
        if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            user.setRole("ROLE_USER");
        }
        if (ObjectUtils.isEmpty(user.getRole())) {
            user.setRole("ROLE_USER");
        }

        String imageName = (file == null || file.isEmpty()) ? "default.png" : file.getOriginalFilename();
        user.setProfileImage(imageName);

        UserDtls savedUser = userService.saveUser(user);

        if (!ObjectUtils.isEmpty(savedUser) && file != null && !file.isEmpty()) {
            try {
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ResponseEntity.ok(Map.of("message", "Registered successfully", "success", true));
    }

    @GetMapping("/api/user/profile")
    public ResponseEntity<?> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }
        String email = auth.getName();
        UserDtls user = userService.getUserByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }
        // Scrub password before returning
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/auth/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> data, HttpServletRequest request,
            HttpServletResponse response) {
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
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(jwtUtil.generateToken(user.getEmail(), user.getRole()), request.isSecure()).toString());

        return ResponseEntity.ok(Map.of("message", "Login successful", "success", true));
    }

    private ResponseCookie buildCookie(String token, boolean secureRequest) {
        return ResponseCookie.from(AUTH_COOKIE, token)
                .httpOnly(true)
                .secure(secureRequest)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtUtil.getExpirationSeconds())
                .build();
    }

    private String redirectFor(UserDtls user) {
        if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            return "/admin/dashboard";
        }
        if ("ROLE_SELLER".equalsIgnoreCase(user.getRole())) {
            return "/seller/dashboard";
        }
        return "/user/dashboard";
    }
}
