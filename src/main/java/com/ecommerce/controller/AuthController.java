package com.ecommerce.controller;

import java.util.Map;
import java.util.Locale;

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
import com.ecommerce.dto.AuthLoginRequest;
import com.ecommerce.dto.AuthLoginResponse;
import com.ecommerce.model.OtpChannel;
import com.ecommerce.model.OtpPurpose;
import com.ecommerce.model.UserDtls;
import com.ecommerce.service.OtpService;
import com.ecommerce.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
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

    @Autowired
    private OtpService otpService;

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody AuthLoginRequest request, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        try {
            UserDtls user = userService.getUserByEmailOrMobile(request.email());
            String loginEmail = user != null ? user.getEmail() : request.email();

            if (user != null && StringUtils.hasText(request.role())) {
                String requestedRole = normalizeRole(request.role());
                String actualRole = normalizeRole(user.getRole());
                if (!requestedRole.equals(actualRole)) {
                    String message = "ROLE_SELLER".equals(requestedRole)
                            ? "Please log in with a seller account."
                            : "Please log in with a customer account.";
                    return ResponseEntity.status(403).body(Map.of("message", message));
                }
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginEmail, request.password()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            user = userService.getUserByEmail(loginEmail);
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
    public ResponseEntity<?> signup(@ModelAttribute UserDtls user,
            @RequestParam(value = "img", required = false) MultipartFile file,
            @RequestParam(value = "mobileOtp", required = false) String mobileOtp,
            @RequestParam(value = "emailOtp", required = false) String emailOtp,
            HttpServletRequest request, HttpServletResponse response) {
        if (userService.existsEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
        }

        // Default role is ROLE_USER if none provided
        if (ObjectUtils.isEmpty(user.getRole())) {
            user.setRole("ROLE_USER");
        }

        if ("ROLE_SELLER".equals(user.getRole())) {
            try {
                otpService.verifyOtp(OtpPurpose.SELLER_SIGNUP, OtpChannel.SMS, user.getMobileNumber(), mobileOtp);
                otpService.verifyOtp(OtpPurpose.SELLER_SIGNUP, OtpChannel.EMAIL, user.getEmail(), emailOtp);
                otpService.assertVerified(OtpPurpose.SELLER_SIGNUP, OtpChannel.SMS, user.getMobileNumber());
                otpService.assertVerified(OtpPurpose.SELLER_SIGNUP, OtpChannel.EMAIL, user.getEmail());
            } catch (IllegalStateException ex) {
                return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
            }
        }

        String imageName = (file == null || file.isEmpty()) ? "default.png" : file.getOriginalFilename();
        user.setProfileImage(imageName);

        UserDtls savedUser = userService.saveUser(user);

        if (!ObjectUtils.isEmpty(savedUser)) {
            // Save image if present
            if (file != null && !file.isEmpty()) {
                try {
                    File saveFile = new ClassPathResource("static/img").getFile();
                    Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
                            + file.getOriginalFilename());
                    if (!Files.exists(path.getParent()))
                        Files.createDirectories(path.getParent());
                    Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Generate Token and Login Automatically
            String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole());
            response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(token, request.isSecure()).toString());

            if ("ROLE_SELLER".equals(savedUser.getRole())) {
                otpService.consumeVerified(OtpPurpose.SELLER_SIGNUP, OtpChannel.SMS, savedUser.getMobileNumber());
                otpService.consumeVerified(OtpPurpose.SELLER_SIGNUP, OtpChannel.EMAIL, savedUser.getEmail());
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Registered successfully",
                    "success", true,
                    "token", token,
                    "role", savedUser.getRole(),
                    "name", savedUser.getName()));
        }

        return ResponseEntity.status(500).body(Map.of("message", "Something went wrong during registration"));
    }

    @PostMapping("/api/user/profile")
    public ResponseEntity<?> updateProfile(@ModelAttribute UserDtls user,
            @RequestParam(value = "img", required = false) MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        String email = auth.getName();
        UserDtls dbUser = userService.getUserByEmail(email);
        if (dbUser == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        // Update only provided fields
        if (StringUtils.hasText(user.getName()))
            dbUser.setName(user.getName());
        if (StringUtils.hasText(user.getMobileNumber()))
            dbUser.setMobileNumber(user.getMobileNumber());
        if (StringUtils.hasText(user.getAddress()))
            dbUser.setAddress(user.getAddress());
        if (StringUtils.hasText(user.getCity()))
            dbUser.setCity(user.getCity());
        if (StringUtils.hasText(user.getState()))
            dbUser.setState(user.getState());
        if (StringUtils.hasText(user.getPincode()))
            dbUser.setPincode(user.getPincode());
        if (StringUtils.hasText(user.getStoreName()))
            dbUser.setStoreName(user.getStoreName());
        if (StringUtils.hasText(user.getStoreDescription()))
            dbUser.setStoreDescription(user.getStoreDescription());

        UserDtls updated = userService.updateUserProfile(dbUser, file);
        updated.setPassword(null); // Safety

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully", "user", updated));
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
        if (user != null && "ROLE_SELLER".equals(user.getRole())) {
            return "/seller/dashboard";
        }
        if (user != null && "ROLE_ADMIN".equals(user.getRole())) {
            return "/admin/";
        }
        if (user != null && "ROLE_USER".equals(user.getRole())) {
            return "/";
        }
        return "/";
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "";
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return normalizedRole.startsWith("ROLE_") ? normalizedRole : "ROLE_" + normalizedRole;
    }
}
