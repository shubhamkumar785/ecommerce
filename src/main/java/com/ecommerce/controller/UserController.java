package com.ecommerce.controller;

import java.security.Principal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.ecommerce.config.CustomUser;
import com.ecommerce.config.JwtUtil;
import com.ecommerce.model.Address;
import com.ecommerce.model.Cart;
import com.ecommerce.model.Category;
import com.ecommerce.model.OrderRequest;
import com.ecommerce.model.ProductOrder;
import com.ecommerce.model.UserDtls;
import com.ecommerce.model.Wishlist;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.AddressService;
import com.ecommerce.service.CartService;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ReviewService;
import com.ecommerce.service.UserService;
import com.ecommerce.service.WishlistService;
import com.ecommerce.util.CommonUtil;
import com.ecommerce.util.OrderStatus;

import com.ecommerce.service.ReturnRequestService;
import com.ecommerce.model.ReturnRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.ClassPathResource;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	private static final String AUTH_COOKIE = "SHOPPING_CART_TOKEN";
	private static final String EMAIL_OTP_CODE = "profile.email.otp.code";
	private static final String EMAIL_OTP_VALUE = "profile.email.otp.value";
	private static final String EMAIL_OTP_EXPIRY = "profile.email.otp.expiry";
	private static final String MOBILE_OTP_CODE = "profile.mobile.otp.code";
	private static final String MOBILE_OTP_VALUE = "profile.mobile.otp.value";
	private static final String MOBILE_OTP_EXPIRY = "profile.mobile.otp.expiry";
	private static final long OTP_VALIDITY_MS = 10 * 60 * 1000L;

	@Autowired
	private UserService userService;
	@Autowired
	private CategoryService categoryService;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AddressService addressService;

	@Autowired
	private WishlistService wishlistService;

	@Autowired
	private ReturnRequestService returnRequestService;

	@Autowired
	private ReviewService reviewService;

	@Autowired
	private JwtUtil jwtUtil;

	@GetMapping("/")
	public String home() {
		return "redirect:/user/dashboard";
	}

	@GetMapping("/dashboard")
	public String loadDashboard(Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		List<ProductOrder> orders = orderService.getOrdersByUser(user.getId());
		List<Wishlist> wishlist = wishlistService.getWishlistByUser(user.getId());
		List<Address> addresses = addressService.getAddressByUser(user.getId());

		m.addAttribute("orders", orders);
		m.addAttribute("wishlist", wishlist);
		m.addAttribute("addresses", addresses);

		return "/user/dashboard";
	}

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
		}

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);
	}

	@GetMapping("/addCart")
	public String addToCart(@RequestParam Integer pid, @RequestParam(required = false) Integer uid, 
			Principal p, jakarta.servlet.http.HttpServletRequest request, HttpSession session) {
		
		Integer userId = uid;
		if (userId == null || userId == 0) {
			UserDtls user = getLoggedInUserDetails(p);
			userId = user.getId();
		}
		
		Cart saveCart = cartService.saveCart(pid, userId);

		if (ObjectUtils.isEmpty(saveCart)) {
			session.setAttribute("errorMsg", "Product add to cart failed");
		} else {
			session.setAttribute("succMsg", "Product added to cart");
		}
		
		String referer = request.getHeader("Referer");
		if (referer != null && !referer.isEmpty()) {
			return "redirect:" + referer;
		}
		
		return "redirect:/product/" + pid;
	}

	@GetMapping("/cart")
	public String loadCartPage(Principal p, Model m) {

		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		
		if (carts.size() > 0) {
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			m.addAttribute("totalOrderPrice", totalOrderPrice);
            
            // Calculate full price breakdown
            Double totalMrp = 0.0;
            for (Cart c : carts) {
                totalMrp += (c.getProduct().getPrice() * c.getQuantity());
            }
            m.addAttribute("totalMrp", totalMrp);
            m.addAttribute("totalDiscount", totalMrp - totalOrderPrice);
            m.addAttribute("itemsCount", carts.size());
		}
        
        m.addAttribute("userAddress", user.getAddress() + ", " + user.getCity() + " - " + user.getPincode());
        
		return "/user/cart";
	}

	@GetMapping("/cartQuantityUpdate")
	public String updateCartQuantity(@RequestParam String sy, @RequestParam Integer cid) {
		cartService.updateQuantity(sy, cid);
		return "redirect:/user/cart";
	}

	private UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

	@GetMapping("/orders")
	public String orderPage(Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		m.addAttribute("carts", carts);
		if (carts.size() > 0) {
			Double orderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
			Double totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice() + 250 + 100;
			m.addAttribute("orderPrice", orderPrice);
			m.addAttribute("totalOrderPrice", totalOrderPrice);
		}
		return "/user/order";
	}

	@PostMapping("/save-order")
	public String saveOrder(@ModelAttribute OrderRequest request, Principal p) throws Exception {
		// System.out.println(request);
		UserDtls user = getLoggedInUserDetails(p);
		orderService.saveOrder(user.getId(), request);

		return "redirect:/user/success";
	}

	@PostMapping("/submit-review")
	public String submitReview(@RequestParam Integer productId, @RequestParam Integer rating,
			@RequestParam(required = false) String comment, Principal p, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		try {
			reviewService.saveOrUpdateReview(user.getId(), productId, rating, comment);
			session.setAttribute("succMsg", "Your review has been saved successfully");
		} catch (IllegalArgumentException e) {
			session.setAttribute("errorMsg", e.getMessage());
		}
		return "redirect:/product/" + productId + "#reviews";
	}

	@GetMapping("/success")
	public String loadSuccess() {
		return "/user/success";
	}

	@GetMapping("/user-orders")
	public String myOrder(Model m, Principal p) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		List<ProductOrder> orders = orderService.getOrdersByUser(loginUser.getId());
		m.addAttribute("orders", orders);
		return "/user/my_orders";
	}

	@GetMapping("/update-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) {

		OrderStatus[] values = OrderStatus.values();
		String status = null;

		for (OrderStatus orderSt : values) {
			if (orderSt.getId().equals(st)) {
				status = orderSt.getName();
			}
		}

		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);

		try {
			commonUtil.sendMailForProductOrder(updateOrder, status);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Status Updated");
		} else {
			session.setAttribute("errorMsg", "status not updated");
		}
		return "redirect:/user/user-orders";
	}

	@GetMapping("/profile")
	public String profile() {
		return "/user/profile";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img,
			@RequestParam(required = false) String emailOtp, @RequestParam(required = false) String mobileOtp, Principal p,
			HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		UserDtls loggedInUser = getLoggedInUserDetails(p);
		String requestedEmail = normalizeEmail(user.getEmail());
		String requestedMobile = normalizeMobile(user.getMobileNumber());

		if (!StringUtils.hasText(requestedEmail)) {
			session.setAttribute("errorMsg", "Email address is required");
			return "redirect:/user/profile";
		}
		if (!StringUtils.hasText(requestedMobile)) {
			session.setAttribute("errorMsg", "Mobile number is required");
			return "redirect:/user/profile";
		}

		boolean emailChanged = !requestedEmail.equalsIgnoreCase(loggedInUser.getEmail());
		boolean mobileChanged = !requestedMobile.equals(normalizeMobile(loggedInUser.getMobileNumber()));

		if (emailChanged) {
			if (Boolean.TRUE.equals(userService.existsEmail(requestedEmail))) {
				session.setAttribute("errorMsg", "Email already exists");
				return "redirect:/user/profile";
			}
			String emailOtpError = validateOtp(session, EMAIL_OTP_CODE, EMAIL_OTP_VALUE, EMAIL_OTP_EXPIRY, requestedEmail,
					emailOtp, "email");
			if (emailOtpError != null) {
				session.setAttribute("errorMsg", emailOtpError);
				return "redirect:/user/profile";
			}
		}

		if (mobileChanged) {
			String mobileOtpError = validateOtp(session, MOBILE_OTP_CODE, MOBILE_OTP_VALUE, MOBILE_OTP_EXPIRY, requestedMobile,
					mobileOtp, "mobile number");
			if (mobileOtpError != null) {
				session.setAttribute("errorMsg", mobileOtpError);
				return "redirect:/user/profile";
			}
		}

		loggedInUser.setName(user.getName());
		loggedInUser.setEmail(requestedEmail);
		loggedInUser.setMobileNumber(requestedMobile);

		UserDtls updateUserProfile = userService.updateUserProfileWithContact(loggedInUser, img, requestedEmail, requestedMobile);
		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Profile not updated");
		} else {
			if (emailChanged) {
				refreshLoggedInUser(updateUserProfile, request, response);
			}
			clearOtpSession(session, EMAIL_OTP_CODE, EMAIL_OTP_VALUE, EMAIL_OTP_EXPIRY);
			clearOtpSession(session, MOBILE_OTP_CODE, MOBILE_OTP_VALUE, MOBILE_OTP_EXPIRY);
			session.setAttribute("succMsg", "Profile Updated");
		}
		return "redirect:/user/profile";
	}

	@PostMapping("/request-contact-otp")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> requestContactOtp(@RequestParam String field, @RequestParam String value,
			Principal p, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		String normalizedField = field == null ? "" : field.trim().toLowerCase();
		String normalizedValue = "email".equals(normalizedField) ? normalizeEmail(value) : normalizeMobile(value);

		if (!List.of("email", "mobile").contains(normalizedField)) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid verification field"));
		}
		if (!StringUtils.hasText(normalizedValue)) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Please enter a valid value"));
		}

		if ("email".equals(normalizedField)) {
			if (normalizedValue.equalsIgnoreCase(user.getEmail())) {
				return ResponseEntity.ok(Map.of("success", true, "message", "Email address is unchanged"));
			}
			if (Boolean.TRUE.equals(userService.existsEmail(normalizedValue))) {
				return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email already exists"));
			}
		}

		if ("mobile".equals(normalizedField) && normalizedValue.equals(normalizeMobile(user.getMobileNumber()))) {
			return ResponseEntity.ok(Map.of("success", true, "message", "Mobile number is unchanged"));
		}

		String otp = generateOtp();
		long expiry = System.currentTimeMillis() + OTP_VALIDITY_MS;

		if ("email".equals(normalizedField)) {
			storeOtp(session, EMAIL_OTP_CODE, EMAIL_OTP_VALUE, EMAIL_OTP_EXPIRY, otp, normalizedValue, expiry);
			try {
				commonUtil.sendProfileOtp(normalizedValue, user.getName(), otp);
				return ResponseEntity.ok(Map.of("success", true, "message", "OTP sent to your email address"));
			} catch (Exception e) {
				return ResponseEntity.ok(Map.of("success", true, "message",
						"Email gateway is unavailable, so a local OTP preview is shown for verification",
						"otpPreview", otp));
			}
		}

		storeOtp(session, MOBILE_OTP_CODE, MOBILE_OTP_VALUE, MOBILE_OTP_EXPIRY, otp, normalizedValue, expiry);
		return ResponseEntity.ok(Map.of("success", true, "message",
				"SMS gateway is not configured, so a local OTP preview is shown for verification", "otpPreview", otp));
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword,
			@RequestParam String confirmNewPassword, Principal p, HttpSession session) {
		UserDtls loggedInUserDetails = getLoggedInUserDetails(p);

		if (!StringUtils.hasText(currentPassword) || !StringUtils.hasText(newPassword)
				|| !StringUtils.hasText(confirmNewPassword)) {
			session.setAttribute("errorMsg", "All password fields are required");
			return "redirect:/user/settings";
		}

		if (!newPassword.equals(confirmNewPassword)) {
			session.setAttribute("errorMsg", "New password and confirm password do not match");
			return "redirect:/user/settings";
		}

		if (newPassword.length() < 6) {
			session.setAttribute("errorMsg", "New password must be at least 6 characters long");
			return "redirect:/user/settings";
		}

		boolean matches = passwordEncoder.matches(currentPassword, loggedInUserDetails.getPassword());

		if (!matches) {
			session.setAttribute("errorMsg", "Current Password incorrect");
			return "redirect:/user/settings";
		}

		if (passwordEncoder.matches(newPassword, loggedInUserDetails.getPassword())) {
			session.setAttribute("errorMsg", "New password must be different from the current password");
			return "redirect:/user/settings";
		}

		String encodePassword = passwordEncoder.encode(newPassword);
		UserDtls updateUser = userService.updatePassword(loggedInUserDetails.getId(), encodePassword);
		if (ObjectUtils.isEmpty(updateUser)) {
			session.setAttribute("errorMsg", "Password not updated due to a server error");
		} else {
			session.setAttribute("succMsg", "Password updated successfully and saved to the database");
		}

		return "redirect:/user/settings";
	}

	/* --- Wishlist Implementation --- */

	@GetMapping("/wishlist")
	public String loadWishlist(Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		List<Wishlist> wishlist = wishlistService.getWishlistByUser(user.getId());
		m.addAttribute("wishlist", wishlist);
		return "/user/wishlist";
	}

	@GetMapping("/add-wishlist")
	public String addToWishlist(@RequestParam Integer pid, Principal p, HttpServletRequest request, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		wishlistService.addToWishlist(user.getId(), pid);
		session.setAttribute("succMsg", "Product added to wishlist");
		
		String referer = request.getHeader("Referer");
		if (referer != null && !referer.isEmpty()) {
			return "redirect:" + referer;
		}
		return "redirect:/product/" + pid;
	}

	@GetMapping("/remove-wishlist")
	public String removeFromWishlist(@RequestParam Integer pid, Principal p, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		wishlistService.removeFromWishlist(user.getId(), pid);
		session.setAttribute("succMsg", "Product removed from wishlist");
		return "redirect:/user/wishlist";
	}

	/* --- Address Management Implementation --- */

	@GetMapping("/address")
	public String loadAddressPage(Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		List<Address> addresses = addressService.getAddressByUser(user.getId());
		m.addAttribute("addresses", addresses);
		return "/user/address";
	}

	@PostMapping("/save-address")
	public String saveAddress(@ModelAttribute Address address, Principal p, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		addressService.saveAddress(address, user.getId());
		session.setAttribute("succMsg", "Address saved successfully");
		return "redirect:/user/address";
	}

	@GetMapping("/delete-address")
	public String deleteAddress(@RequestParam Integer id, HttpSession session) {
		addressService.deleteAddress(id);
		session.setAttribute("succMsg", "Address deleted successfully");
		return "redirect:/user/address";
	}

	/* --- Settings Implementation --- */

	@GetMapping("/settings")
	public String loadSettingsPage() {
		return "/user/settings";
	}

	@GetMapping("/customer-care")
	public String loadCustomerCarePage(Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		List<ProductOrder> orders = orderService.getOrdersByUser(user.getId());

		List<Map<String, String>> helpCategories = List.of(
				createSupportTopic("Orders", "Track, cancel, or update order-related requests"),
				createSupportTopic("Returns & Refunds", "Get help with return windows, refunds, and exchanges"),
				createSupportTopic("Payments", "Resolve payment failures, refunds, and billing questions"),
				createSupportTopic("Account", "Profile, address, password, and sign-in assistance"),
				createSupportTopic("Offers & Wallet", "Understand coupons, promotions, and balance issues"),
				createSupportTopic("Shipping", "Delivery timelines, delays, and address updates"),
				createSupportTopic("Wishlist", "Saved items and product availability support"),
				createSupportTopic("Safety", "Report suspicious activity or account concerns"));

		List<Map<String, String>> supportNav = List.of(
				Map.of("label", "Help Centre", "value", "Overview"),
				Map.of("label", "Orders", "value", String.valueOf(orders.size())),
				Map.of("label", "Returns", "value", "Easy support"),
				Map.of("label", "Payments", "value", "24x7"),
				Map.of("label", "Account", "value", "Protected"),
				Map.of("label", "Address", "value", "Manage"),
				Map.of("label", "Wishlist", "value", "Saved items"),
				Map.of("label", "Security", "value", "Assistance"));

		List<Map<String, String>> quickHelpCards = List.of(
				createSupportTopic("GST Rate Updates", "Quick answers to policy and invoice related questions"),
				createSupportTopic("Return Policy Guide", "Check eligibility, timelines, and next steps"),
				createSupportTopic("Delivery Promise", "Understand shipment timelines and delay support"));

		List<ProductOrder> latestOrders = orders.stream().limit(5).toList();

		m.addAttribute("supportNav", supportNav);
		m.addAttribute("helpCategories", helpCategories);
		m.addAttribute("quickHelpCards", quickHelpCards);
		m.addAttribute("latestOrders", latestOrders);
		m.addAttribute("supportStats", buildSupportStats(orders));
		return "/user/customer_care";
	}

	@GetMapping("/returns")
	public String loadReturns(Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		List<ReturnRequest> returns = returnRequestService.getReturnRequestsByUser(user);
		m.addAttribute("returns", returns);
		return "/user/my_returns";
	}

	@PostMapping("/submit-return")
	public String submitReturn(@ModelAttribute ReturnRequest returnRequest, @RequestParam("file") MultipartFile file,
			Principal p, HttpSession session) throws Exception {
		UserDtls user = getLoggedInUserDetails(p);
		returnRequest.setUser(user);

		if (!file.isEmpty()) {
			String fileName = file.getOriginalFilename();
			returnRequest.setProofImage(fileName);
			
			File saveFile = new ClassPathResource("static/img").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "proof_img" + File.separator + fileName);
			
			if(!Files.exists(path.getParent())) {
				Files.createDirectories(path.getParent());
			}
			
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
		}

		ReturnRequest saveReturn = returnRequestService.saveReturnRequest(returnRequest);
		if (saveReturn != null) {
			session.setAttribute("succMsg", "Return Request Submitted Successfully");
		} else {
			session.setAttribute("errorMsg", "Something went wrong on server");
		}
		return "redirect:/user/user-orders";
	}

	private Map<String, String> createSupportTopic(String title, String subtitle) {
		Map<String, String> topic = new LinkedHashMap<>();
		topic.put("title", title);
		topic.put("subtitle", subtitle);
		return topic;
	}

	private Map<String, Object> buildSupportStats(List<ProductOrder> orders) {
		Map<String, Object> stats = new LinkedHashMap<>();
		long openIssues = orders.stream().filter(order -> !"Delivered".equalsIgnoreCase(order.getStatus())).count();
		long delivered = orders.stream().filter(order -> "Delivered".equalsIgnoreCase(order.getStatus())).count();
		stats.put("totalOrders", orders.size());
		stats.put("openIssues", openIssues);
		stats.put("delivered", delivered);
		stats.put("responseTime", "Within minutes");
		return stats;
	}

	private void refreshLoggedInUser(UserDtls user, HttpServletRequest request, HttpServletResponse response) {
		Authentication authentication = new UsernamePasswordAuthenticationToken(new CustomUser(user), null,
				new CustomUser(user).getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);

		ResponseCookie authCookie = ResponseCookie.from(AUTH_COOKIE, jwtUtil.generateToken(user.getEmail(), user.getRole()))
				.httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/")
				.maxAge(jwtUtil.getExpirationSeconds()).build();
		response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
	}

	private String validateOtp(HttpSession session, String codeKey, String valueKey, String expiryKey, String expectedValue,
			String submittedOtp, String label) {
		Object savedOtp = session.getAttribute(codeKey);
		Object savedValue = session.getAttribute(valueKey);
		Object expiryObj = session.getAttribute(expiryKey);

		if (savedOtp == null || savedValue == null || expiryObj == null) {
			return "Please request an OTP for your " + label + " change first";
		}
		if (!expectedValue.equals(savedValue.toString())) {
			return "The verified " + label + " does not match your latest input. Please request a new OTP";
		}
		if (!(expiryObj instanceof Long expiryTime) || expiryTime < System.currentTimeMillis()) {
			clearOtpSession(session, codeKey, valueKey, expiryKey);
			return "Your OTP for " + label + " has expired. Please request a new one";
		}
		if (!StringUtils.hasText(submittedOtp) || !savedOtp.toString().equals(submittedOtp.trim())) {
			return "Invalid OTP for " + label;
		}
		return null;
	}

	private void storeOtp(HttpSession session, String codeKey, String valueKey, String expiryKey, String otp, String value,
			long expiry) {
		session.setAttribute(codeKey, otp);
		session.setAttribute(valueKey, value);
		session.setAttribute(expiryKey, expiry);
	}

	private void clearOtpSession(HttpSession session, String codeKey, String valueKey, String expiryKey) {
		session.removeAttribute(codeKey);
		session.removeAttribute(valueKey);
		session.removeAttribute(expiryKey);
	}

	private String generateOtp() {
		return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
	}

	private String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase();
	}

	private String normalizeMobile(String mobile) {
		return mobile == null ? "" : mobile.replaceAll("\\s+", "");
	}
}
