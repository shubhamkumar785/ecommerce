package com.ecommerce.controller;

import java.security.Principal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

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
import org.springframework.web.bind.annotation.RequestBody;
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
import com.ecommerce.model.PendingUpiCheckout;
import com.ecommerce.model.Product;
import com.ecommerce.model.ProductOrder;
import com.ecommerce.model.UpiCheckoutInitiationRequest;
import com.ecommerce.model.UpiPaymentVerificationRequest;
import com.ecommerce.model.UserDtls;
import com.ecommerce.model.Wishlist;
import com.ecommerce.service.AddressService;
import com.ecommerce.service.CartService;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.PaymentGatewayService;
import com.ecommerce.service.ProductService;
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

@Controller
@RequestMapping("/user")
public class UserController {
	private static final String AUTH_COOKIE = "SHOPPING_CART_TOKEN";
	private static final String LAST_CONFIRMED_ORDER_ID = "lastConfirmedOrderId";
	private static final double CASH_ON_DELIVERY_LIMIT = 1000.0;
	private static final String EMAIL_OTP_CODE = "profile.email.otp.code";
	private static final String EMAIL_OTP_VALUE = "profile.email.otp.value";
	private static final String EMAIL_OTP_EXPIRY = "profile.email.otp.expiry";
	private static final String MOBILE_OTP_CODE = "profile.mobile.otp.code";
	private static final String MOBILE_OTP_VALUE = "profile.mobile.otp.value";
	private static final String MOBILE_OTP_EXPIRY = "profile.mobile.otp.expiry";
	private static final String PENDING_UPI_CHECKOUTS = "pendingUpiCheckouts";
	private static final long OTP_VALIDITY_MS = 10 * 60 * 1000L;
	private static final Pattern UPI_ID_PATTERN = Pattern
			.compile("^[a-zA-Z0-9._-]{2,256}@[a-zA-Z][a-zA-Z0-9.-]{1,63}$");

	@Autowired
	private UserService userService;
	@Autowired
	private CategoryService categoryService;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private ProductService productService;

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

	@Autowired
	private PaymentGatewayService paymentGatewayService;

	@GetMapping("/")
	public String home() {
		return "redirect:/user/dashboard";
	}

	@GetMapping("/dashboard")
	public String loadDashboard(Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		List<ProductOrder> orders = filterVisibleOrders(orderService.getOrdersByUser(user.getId()));
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
			m.addAttribute("orderPrice", orderPrice);
			m.addAttribute("totalOrderPrice", orderPrice);
		}
		m.addAttribute("cashOnDeliveryAvailable", isCashOnDeliveryEligible(carts));
		m.addAttribute("razorpayConfigured", paymentGatewayService.isConfigured());
		m.addAttribute("razorpayKeyId", paymentGatewayService.getPublicKey());
		return "/user/order";
	}

	@GetMapping("/cart-checkout")
	public String loadCartCheckoutPage(Principal p, Model m, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		if (carts == null || carts.isEmpty()) {
			session.setAttribute("errorMsg", "Your cart is empty");
			return "redirect:/user/cart";
		}

		Address selectedAddress = resolveSelectedAddress(user.getId());
		if (selectedAddress == null) {
			session.setAttribute("errorMsg", "Please add a delivery address before placing your order");
			return "redirect:/user/address";
		}

		String[] nameParts = splitName(selectedAddress.getFullName());
		populateCartSummary(m, carts);
		m.addAttribute("firstName", nameParts[0]);
		m.addAttribute("lastName", nameParts[1]);
		m.addAttribute("selectedAddress", selectedAddress);
		return "/user/cart_checkout";
	}

	@PostMapping("/cart-checkout")
	public String loadCartPaymentPage(@ModelAttribute OrderRequest request, Principal p, Model m, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		if (carts == null || carts.isEmpty()) {
			session.setAttribute("errorMsg", "Your cart is empty");
			return "redirect:/user/cart";
		}

		Address selectedAddress = resolveSelectedAddress(user.getId());
		if (selectedAddress == null) {
			session.setAttribute("errorMsg", "Please add a delivery address before placing your order");
			return "redirect:/user/address";
		}

		applyOrderRequestDefaults(request, user, selectedAddress);
		boolean cashOnDeliveryAvailable = isCashOnDeliveryEligible(carts);
		String paymentType = normalizePaymentType(request.getPaymentType());
		if ("COD".equalsIgnoreCase(paymentType) && !cashOnDeliveryAvailable) {
			paymentType = "CARD";
		}
		request.setPaymentType(paymentType);

		populateCartSummary(m, carts);
		m.addAttribute("orderRequest", request);
		m.addAttribute("deliveryFullName", buildFullName(request.getFirstName(), request.getLastName()));
		m.addAttribute("cashOnDeliveryAvailable", cashOnDeliveryAvailable);
		m.addAttribute("razorpayConfigured", paymentGatewayService.isConfigured());
		m.addAttribute("razorpayKeyId", paymentGatewayService.getPublicKey());
		return "/user/cart_payment";
	}

	@GetMapping("/buy-now")
	public String buyNowPage(@RequestParam Integer pid, @RequestParam(defaultValue = "1") Integer quantity, Principal p,
			Model m, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		Product product = validateBuyNowProduct(pid, session);
		if (product == null) {
			return "redirect:/products";
		}
		if (sanitizeQuantity(quantity) > product.getStock()) {
			session.setAttribute("errorMsg", "Only " + product.getStock() + " item(s) are available right now.");
			return "redirect:/product/" + pid;
		}

		int orderQuantity = sanitizeQuantity(quantity);
		Address selectedAddress = resolveSelectedAddress(user.getId());
		if (selectedAddress == null) {
			session.setAttribute("errorMsg", "Please add a delivery address before placing your order");
			return "redirect:/user/address";
		}

		String[] nameParts = splitName(selectedAddress.getFullName());

		populateBuyNowSummary(m, product, orderQuantity);
		m.addAttribute("firstName", nameParts[0]);
		m.addAttribute("lastName", nameParts[1]);
		m.addAttribute("selectedAddress", selectedAddress);
		m.addAttribute("hideCartButton", true);
		return "/user/buy_now";
	}

	@PostMapping("/buy-now")
	public String loadBuyNowPaymentPage(@RequestParam Integer pid, @RequestParam(defaultValue = "1") Integer quantity,
			@ModelAttribute OrderRequest request, Principal p, Model m, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		Product product = validateBuyNowProduct(pid, session);
		if (product == null) {
			return "redirect:/products";
		}
		if (sanitizeQuantity(quantity) > product.getStock()) {
			session.setAttribute("errorMsg", "Only " + product.getStock() + " item(s) are available right now.");
			return "redirect:/product/" + pid;
		}

		Address selectedAddress = resolveSelectedAddress(user.getId());
		if (selectedAddress == null) {
			session.setAttribute("errorMsg", "Please add a delivery address before placing your order");
			return "redirect:/user/address";
		}

		applyOrderRequestDefaults(request, user, selectedAddress);
		boolean cashOnDeliveryAvailable = isCashOnDeliveryEligible(product);
		String paymentType = normalizePaymentType(request.getPaymentType());
		if ("COD".equalsIgnoreCase(paymentType) && !cashOnDeliveryAvailable) {
			paymentType = "CARD";
		}
		request.setPaymentType(paymentType);

		int orderQuantity = sanitizeQuantity(quantity);
		populateBuyNowSummary(m, product, orderQuantity);
		m.addAttribute("orderRequest", request);
		m.addAttribute("deliveryFullName", buildFullName(request.getFirstName(), request.getLastName()));
		m.addAttribute("cashOnDeliveryAvailable", cashOnDeliveryAvailable);
		m.addAttribute("razorpayConfigured", paymentGatewayService.isConfigured());
		m.addAttribute("razorpayKeyId", paymentGatewayService.getPublicKey());
		m.addAttribute("hideCartButton", true);
		return "/user/buy_now_payment";
	}

	@PostMapping("/buy-now/place-order")
	public String placeBuyNowOrder(@RequestParam Integer pid, @RequestParam(defaultValue = "1") Integer quantity,
			@ModelAttribute OrderRequest request, Principal p, HttpSession session) throws Exception {
		UserDtls user = getLoggedInUserDetails(p);
		Product product = validateBuyNowProduct(pid, session);
		if (product == null) {
			return "redirect:/products";
		}

		request.setPaymentType(normalizePaymentType(request.getPaymentType()));
		if ("UPI".equalsIgnoreCase(request.getPaymentType())) {
			session.setAttribute("errorMsg", "Please complete the verified UPI payment flow before placing your order");
			return "redirect:/user/buy-now?pid=" + pid + "&quantity=" + sanitizeQuantity(quantity);
		}
		if ("COD".equalsIgnoreCase(request.getPaymentType()) && !isCashOnDeliveryEligible(product)) {
			session.setAttribute("errorMsg", "Cash on Delivery is only available for products priced below ₹1000");
			return "redirect:/user/buy-now?pid=" + pid + "&quantity=" + sanitizeQuantity(quantity);
		}

		Address selectedAddress = resolveSelectedAddress(user.getId());
		if (selectedAddress != null) {
			applyOrderRequestDefaults(request, user, selectedAddress);
		}

		ProductOrder placedOrder;
		try {
			placedOrder = orderService.saveSingleProductOrder(user.getId(), product, sanitizeQuantity(quantity),
					request);
		} catch (IllegalStateException ex) {
			session.setAttribute("errorMsg", ex.getMessage());
			return "redirect:/product/" + pid;
		}
		session.setAttribute("succMsg", "Order placed successfully");
		session.setAttribute(LAST_CONFIRMED_ORDER_ID, placedOrder.getOrderId());
		return "redirect:/user/order-confirmation?orderId=" + placedOrder.getOrderId();
	}

	@PostMapping("/save-order")
	public String saveOrder(@ModelAttribute OrderRequest request, Principal p, HttpSession session) throws Exception {
		// System.out.println(request);
		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		request.setPaymentType(normalizePaymentType(request.getPaymentType()));
		if ("UPI".equalsIgnoreCase(request.getPaymentType())) {
			session.setAttribute("errorMsg", "Please complete the verified UPI payment flow before placing your order");
			return "redirect:/user/orders";
		}
		if ("COD".equalsIgnoreCase(request.getPaymentType()) && !isCashOnDeliveryEligible(carts)) {
			session.setAttribute("errorMsg",
					"Cash on Delivery is only available when every product is priced below ₹1000");
			return "redirect:/user/orders";
		}
		try {
			orderService.saveOrder(user.getId(), request);
		} catch (IllegalStateException ex) {
			session.setAttribute("errorMsg", ex.getMessage());
			return "redirect:/user/cart";
		}

		return "redirect:/user/success";
	}

	@PostMapping("/payments/upi/initiate-buy-now")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> initiateBuyNowUpiPayment(
			@RequestBody UpiCheckoutInitiationRequest request,
			Principal p, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		Product product = validateBuyNowProduct(request.getPid(), session);
		if (product == null) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "Selected product is unavailable"));
		}
		if (!paymentGatewayService.isConfigured()) {
			return ResponseEntity.badRequest().body(
					Map.of("success", false, "message", "Razorpay test credentials are not configured yet"));
		}

		String upiId = normalizeUpiId(request.getUpiId());
		if (!isValidUpiId(upiId)) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "Enter a valid UPI ID like name@bank"));
		}

		Address selectedAddress = resolveSelectedAddress(user.getId());
		if (selectedAddress == null) {
			return ResponseEntity.badRequest().body(
					Map.of("success", false, "message", "Please add a delivery address before placing your order"));
		}

		int orderQuantity = sanitizeQuantity(request.getQuantity());
		applyOrderRequestDefaults(request, user, selectedAddress);
		request.setPaymentType("UPI");
		request.setUpiId(upiId);

		try {
			int amountInPaise = toPaise(resolveEffectiveProductPrice(product) * orderQuantity);
			return ResponseEntity.ok(buildUpiInitiationPayload(createPendingUpiCheckout(session, request, user.getId(),
					"BUY_NOW", product.getId(), orderQuantity, amountInPaise, product.getTitle())));
		} catch (IllegalStateException ex) {
			return ResponseEntity.internalServerError().body(Map.of("success", false, "message",
					StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Unable to start the Razorpay payment"));
		}
	}

	@PostMapping("/payments/upi/initiate-cart")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> initiateCartUpiPayment(@RequestBody UpiCheckoutInitiationRequest request,
			Principal p, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		List<Cart> carts = cartService.getCartsByUser(user.getId());
		if (carts == null || carts.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Your cart is empty"));
		}
		if (!paymentGatewayService.isConfigured()) {
			return ResponseEntity.badRequest().body(
					Map.of("success", false, "message", "Razorpay test credentials are not configured yet"));
		}

		String upiId = normalizeUpiId(request.getUpiId());
		if (!isValidUpiId(upiId)) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "Enter a valid UPI ID like name@bank"));
		}

		request.setPaymentType("UPI");
		request.setUpiId(upiId);
		try {
			int amountInPaise = toPaise(resolveCartSubtotal(carts));
			return ResponseEntity.ok(buildUpiInitiationPayload(createPendingUpiCheckout(session, request, user.getId(),
					"CART", null, null, amountInPaise, "Cart Checkout")));
		} catch (IllegalStateException ex) {
			return ResponseEntity.internalServerError().body(Map.of("success", false, "message",
					StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Unable to start the Razorpay payment"));
		}
	}

	@PostMapping("/payments/upi/verify")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> verifyUpiPayment(@RequestBody UpiPaymentVerificationRequest request,
			Principal p, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		PendingUpiCheckout pendingCheckout = getPendingUpiCheckouts(session).get(request.getCheckoutReference());
		if (pendingCheckout == null || pendingCheckout.getUserId() == null
				|| !pendingCheckout.getUserId().equals(user.getId())) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "Your payment session expired. Please try again."));
		}

		if (!StringUtils.hasText(request.getRazorpayOrderId())
				|| !request.getRazorpayOrderId().equals(pendingCheckout.getGatewayOrderId())) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message",
					"Payment verification failed because the payment order did not match the initiated session."));
		}

		try {
			PaymentGatewayService.PaymentVerificationResult verificationResult = paymentGatewayService.verifyPayment(
					request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());
			if (!verificationResult.success()) {
				return ResponseEntity.badRequest().body(Map.of(
						"success", false,
						"message", StringUtils.hasText(verificationResult.failureReason())
								? verificationResult.failureReason()
								: "Payment failed. No order was placed.",
						"paymentStatus", verificationResult.paymentStatus()));
			}

			OrderRequest orderRequest = enrichVerifiedOrderRequest(pendingCheckout, verificationResult);
			Map<String, PendingUpiCheckout> pendingCheckouts = getPendingUpiCheckouts(session);

			if ("BUY_NOW".equalsIgnoreCase(pendingCheckout.getCheckoutType())) {
				Product product = validateBuyNowProduct(pendingCheckout.getProductId(), session);
				if (product == null) {
					return ResponseEntity.badRequest()
							.body(Map.of("success", false, "message", "Selected product is no longer available"));
				}
				int currentAmount = toPaise(
						resolveEffectiveProductPrice(product) * sanitizeQuantity(pendingCheckout.getQuantity()));
				if (currentAmount != pendingCheckout.getAmountInPaise()) {
					return ResponseEntity.badRequest().body(
							Map.of("success", false, "message",
									"Product price changed during checkout. Please review the order and try again."));
				}

				ProductOrder placedOrder;
				try {
					placedOrder = orderService.saveSingleProductOrder(user.getId(), product,
							sanitizeQuantity(pendingCheckout.getQuantity()), orderRequest);
				} catch (IllegalStateException ex) {
					return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
				}
				pendingCheckouts.remove(request.getCheckoutReference());
				session.setAttribute("succMsg", "Payment successful");
				session.setAttribute(LAST_CONFIRMED_ORDER_ID, placedOrder.getOrderId());
				return ResponseEntity.ok(Map.of(
						"success", true,
						"message", "Payment successful. Your order has been confirmed.",
						"paymentStatus", "SUCCESS",
						"redirectUrl", "/user/order-confirmation?orderId=" + placedOrder.getOrderId()));
			}

			List<Cart> carts = cartService.getCartsByUser(user.getId());
			if (carts == null || carts.isEmpty()) {
				return ResponseEntity.badRequest()
						.body(Map.of("success", false, "message", "Your cart is empty. Please add items again."));
			}
			if (toPaise(resolveCartSubtotal(carts)) != pendingCheckout.getAmountInPaise()) {
				return ResponseEntity.badRequest().body(
						Map.of("success", false, "message",
								"Your cart changed during payment. Please review it and try the UPI payment again."));
			}

			try {
				orderService.saveOrder(user.getId(), orderRequest);
			} catch (IllegalStateException ex) {
				return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
			}
			pendingCheckouts.remove(request.getCheckoutReference());
			session.setAttribute("succMsg", "Payment successful");
			return ResponseEntity.ok(Map.of(
					"success", true,
					"message", "Payment successful. Your order has been confirmed.",
					"paymentStatus", "SUCCESS",
					"redirectUrl", "/user/success"));
		} catch (Exception ex) {
			return ResponseEntity.internalServerError()
					.body(Map.of("success", false, "message",
							StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "Payment verification failed"));
		}
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

	@GetMapping("/order-confirmation")
	public String loadOrderConfirmation(@RequestParam(required = false) String orderId, Principal p, Model m,
			HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		String resolvedOrderId = StringUtils.hasText(orderId) ? orderId
				: (String) session.getAttribute(LAST_CONFIRMED_ORDER_ID);
		if (!StringUtils.hasText(resolvedOrderId)) {
			session.setAttribute("errorMsg", "We couldn't find that order confirmation");
			return "redirect:/user/user-orders";
		}

		ProductOrder order = orderService.getOrdersByOrderId(resolvedOrderId);
		if (order == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
			session.setAttribute("errorMsg", "We couldn't find that order confirmation");
			return "redirect:/user/user-orders";
		}

		session.setAttribute(LAST_CONFIRMED_ORDER_ID, order.getOrderId());
		populateOrderConfirmationModel(m, order);
		return "/user/order_confirmation";
	}

	@GetMapping("/order-tracking")
	public String loadOrderTracking(@RequestParam(required = false) String orderId, Principal p, Model m,
			HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		String resolvedOrderId = StringUtils.hasText(orderId) ? orderId
				: (String) session.getAttribute(LAST_CONFIRMED_ORDER_ID);
		if (!StringUtils.hasText(resolvedOrderId)) {
			session.setAttribute("errorMsg", "We couldn't find that order");
			return "redirect:/user/user-orders";
		}

		ProductOrder order = orderService.getOrdersByOrderId(resolvedOrderId);
		if (order == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
			session.setAttribute("errorMsg", "We couldn't find that order");
			return "redirect:/user/user-orders";
		}

		session.setAttribute(LAST_CONFIRMED_ORDER_ID, order.getOrderId());
		populateOrderTrackingModel(m, order);
		return "/user/order_tracking";
	}

	@GetMapping("/user-orders")
	public String myOrder(@RequestParam(required = false, defaultValue = "all") String view, Model m, Principal p) {
		UserDtls loginUser = getLoggedInUserDetails(p);
		List<ProductOrder> userOrders = orderService.getOrdersByUser(loginUser.getId());
		List<ProductOrder> allOrders = filterVisibleOrders(userOrders);
		List<ProductOrder> deliveredOrders = filterDeliveredOrders(userOrders);
		List<ProductOrder> cancelledOrders = filterCancelledOrders(userOrders);

		m.addAttribute("allOrders", allOrders);
		m.addAttribute("deliveredOrders", deliveredOrders);
		m.addAttribute("cancelledOrders", cancelledOrders);
		m.addAttribute("allOrdersCount", allOrders.size());
		m.addAttribute("deliveredOrdersCount", deliveredOrders.size());
		m.addAttribute("cancelledOrdersCount", cancelledOrders.size());
		m.addAttribute("initialOrderFilter", normalizeOrderFilter(view));
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

		sendOrderStatusMailAsync(updateOrder, status);

		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Cancelled".equalsIgnoreCase(status)
					? "Order cancelled and removed from your active orders list"
					: "Status Updated");
		} else {
			session.setAttribute("errorMsg", "status not updated");
		}
		return "Cancelled".equalsIgnoreCase(status) ? "redirect:/user/user-orders?view=cancelled"
				: "redirect:/user/user-orders";
	}

	@PostMapping("/orders/cancel")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> cancelOrderInstantly(@RequestParam Integer id, Principal p) {
		UserDtls user = getLoggedInUserDetails(p);
		ProductOrder order = resolveUserOrderById(id, user);
		if (order == null) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "We couldn't find that order"));
		}
		if (isCancelledStatus(order.getStatus())) {
			return ResponseEntity.ok(buildOrderHistoryPayload(order, user.getId(),
					"Order is already in your cancelled orders section"));
		}
		if ("Delivered".equalsIgnoreCase(order.getStatus())) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message",
					"Delivered orders cannot be cancelled. Please use return or refund options instead."));
		}

		ProductOrder updatedOrder = orderService.updateOrderStatus(order.getId(), OrderStatus.CANCEL.getName());
		sendOrderStatusMailAsync(updatedOrder, OrderStatus.CANCEL.getName());
		return ResponseEntity.ok(buildOrderHistoryPayload(updatedOrder, user.getId(),
				"Order cancelled and moved to your cancelled orders section"));
	}

	@GetMapping("/profile")
	public String profile() {
		return "/user/profile";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img,
			@RequestParam(required = false) String emailOtp, @RequestParam(required = false) String mobileOtp,
			Principal p,
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
			String emailOtpError = validateOtp(session, EMAIL_OTP_CODE, EMAIL_OTP_VALUE, EMAIL_OTP_EXPIRY,
					requestedEmail,
					emailOtp, "email");
			if (emailOtpError != null) {
				session.setAttribute("errorMsg", emailOtpError);
				return "redirect:/user/profile";
			}
		}

		if (mobileChanged) {
			String mobileOtpError = validateOtp(session, MOBILE_OTP_CODE, MOBILE_OTP_VALUE, MOBILE_OTP_EXPIRY,
					requestedMobile,
					mobileOtp, "mobile number");
			if (mobileOtpError != null) {
				session.setAttribute("errorMsg", mobileOtpError);
				return "redirect:/user/profile";
			}
		}

		loggedInUser.setName(user.getName());
		loggedInUser.setEmail(requestedEmail);
		loggedInUser.setMobileNumber(requestedMobile);

		UserDtls updateUserProfile = userService.updateUserProfileWithContact(loggedInUser, img, requestedEmail,
				requestedMobile);
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
	public String addToWishlist(@RequestParam Integer pid, Principal p, HttpServletRequest request,
			HttpSession session) {
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
	public String loadCustomerCarePage(@RequestParam(required = false) String orderId,
			@RequestParam(defaultValue = "false") boolean assistant, Principal p, Model m) {
		UserDtls user = getLoggedInUserDetails(p);
		List<ProductOrder> orders = filterVisibleOrders(orderService.getOrdersByUser(user.getId()));
		ProductOrder selectedSupportOrder = resolveUserOrder(orderId, user);

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
		m.addAttribute("assistantRequested", assistant);
		m.addAttribute("selectedSupportOrder", selectedSupportOrder);
		m.addAttribute("assistantSuggestions", buildAssistantSuggestions(selectedSupportOrder));
		m.addAttribute("assistantContext", buildAssistantContext(selectedSupportOrder));
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
			Path path = Paths
					.get(saveFile.getAbsolutePath() + File.separator + "proof_img" + File.separator + fileName);

			if (!Files.exists(path.getParent())) {
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

	private ProductOrder resolveUserOrder(String orderId, UserDtls user) {
		if (!StringUtils.hasText(orderId) || user == null) {
			return null;
		}

		ProductOrder order = orderService.getOrdersByOrderId(orderId);
		if (order == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
			return null;
		}
		return order;
	}

	private ProductOrder resolveUserOrderById(Integer id, UserDtls user) {
		if (id == null || user == null) {
			return null;
		}

		ProductOrder order = orderService.getOrderById(id);
		if (order == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
			return null;
		}
		return order;
	}

	private List<ProductOrder> filterVisibleOrders(List<ProductOrder> orders) {
		if (orders == null) {
			return List.of();
		}

		return orders.stream()
				.filter(order -> order != null && !isCancelledStatus(order.getStatus()))
				.toList();
	}

	private List<ProductOrder> filterDeliveredOrders(List<ProductOrder> orders) {
		if (orders == null) {
			return List.of();
		}

		return orders.stream()
				.filter(order -> order != null && "Delivered".equalsIgnoreCase(order.getStatus()))
				.toList();
	}

	private List<ProductOrder> filterCancelledOrders(List<ProductOrder> orders) {
		if (orders == null) {
			return List.of();
		}

		return orders.stream()
				.filter(order -> order != null && isCancelledStatus(order.getStatus()))
				.toList();
	}

	private String normalizeOrderFilter(String filter) {
		if (!StringUtils.hasText(filter)) {
			return "all";
		}

		String normalized = filter.trim().toLowerCase(Locale.ENGLISH);
		return List.of("all", "delivered", "cancelled").contains(normalized) ? normalized : "all";
	}

	private Map<String, Object> buildOrderHistoryPayload(ProductOrder order, Integer userId, String message) {
		List<ProductOrder> refreshedOrders = orderService.getOrdersByUser(userId);
		return Map.of(
				"success", true,
				"message", message,
				"orderId", order.getOrderId(),
				"id", order.getId(),
				"status", resolveOrderStatusLabel(order.getStatus()),
				"allOrdersCount", filterVisibleOrders(refreshedOrders).size(),
				"deliveredOrdersCount", filterDeliveredOrders(refreshedOrders).size(),
				"cancelledOrdersCount", filterCancelledOrders(refreshedOrders).size());
	}

	private void sendOrderStatusMailAsync(ProductOrder order, String status) {
		if (order == null || !StringUtils.hasText(status)) {
			return;
		}

		CompletableFuture.runAsync(() -> {
			try {
				commonUtil.sendMailForProductOrder(order, status);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}

	private List<String> buildAssistantSuggestions(ProductOrder order) {
		List<String> suggestions = new ArrayList<>();
		suggestions.add("Where is my order?");
		suggestions.add("When will it be delivered?");
		suggestions.add("How can I change my address?");

		if (order != null) {
			suggestions.add("Tell me about payment for order " + order.getOrderId());
			suggestions.add("Can I cancel order " + order.getOrderId() + "?");
		} else {
			suggestions.add("How do returns and refunds work?");
			suggestions.add("I need help with payment");
		}

		return suggestions.stream().distinct().limit(5).toList();
	}

	private Map<String, String> buildAssistantContext(ProductOrder order) {
		if (order == null) {
			return Map.of();
		}

		LocalDate orderDate = order.getOrderDate() != null ? order.getOrderDate() : LocalDate.now();
		LocalDate estimatedDeliveryDate = orderDate.plusDays(5);

		Map<String, String> context = new LinkedHashMap<>();
		context.put("orderId", order.getOrderId());
		context.put("productTitle", order.getProduct() != null ? order.getProduct().getTitle() : "your order");
		context.put("status", resolveOrderStatusLabel(order.getStatus()));
		context.put("paymentLabel", resolvePaymentLabel(order.getPaymentType()));
		context.put("estimatedDelivery",
				estimatedDeliveryDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH)));
		context.put("orderDate", orderDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)));
		return context;
	}

	private String[] splitName(String fullName) {
		if (!StringUtils.hasText(fullName)) {
			return new String[] { "", "" };
		}

		String[] parts = fullName.trim().split("\\s+", 2);
		if (parts.length == 1) {
			return new String[] { parts[0], "" };
		}
		return parts;
	}

	private double safeAmount(Double value) {
		return value == null ? 0.0 : value;
	}

	private Product validateBuyNowProduct(Integer pid, HttpSession session) {
		Product product = productService.getLiveProductById(pid);
		if (product == null || product.getStock() <= 0) {
			session.setAttribute("errorMsg", "Out of stock. We will notify you when available.");
			return null;
		}
		return product;
	}

	private int sanitizeQuantity(Integer quantity) {
		return quantity == null || quantity < 1 ? 1 : quantity;
	}

	private boolean isCashOnDeliveryEligible(Product product) {
		return resolveEffectiveProductPrice(product) < CASH_ON_DELIVERY_LIMIT;
	}

	private boolean isCashOnDeliveryEligible(List<Cart> carts) {
		return carts != null && !carts.isEmpty()
				&& carts.stream().allMatch(cart -> cart != null && isCashOnDeliveryEligible(cart.getProduct()));
	}

	private double resolveEffectiveProductPrice(Product product) {
		if (product == null) {
			return Double.MAX_VALUE;
		}

		double discountPrice = safeAmount(product.getDiscountPrice());
		if (discountPrice > 0) {
			return discountPrice;
		}

		return safeAmount(product.getPrice());
	}

	private void populateBuyNowSummary(Model m, Product product, int orderQuantity) {
		double mrp = safeAmount(product.getPrice()) * orderQuantity;
		double sellingPrice = safeAmount(product.getDiscountPrice()) * orderQuantity;
		double discountAmount = Math.max(0.0, mrp - sellingPrice);

		m.addAttribute("product", product);
		m.addAttribute("quantity", orderQuantity);
		m.addAttribute("mrp", mrp);
		m.addAttribute("sellingPrice", sellingPrice);
		m.addAttribute("discountAmount", discountAmount);
		m.addAttribute("estimatedDelivery", "Delivery by 8 Apr, Wednesday");
	}

	private void populateCartSummary(Model m, List<Cart> carts) {
		double totalMrp = 0.0;
		double totalSellingPrice = 0.0;
		int itemsCount = 0;

		for (Cart cart : carts) {
			if (cart != null && cart.getProduct() != null) {
				int qty = cart.getQuantity() != null ? cart.getQuantity() : 1;
				totalMrp += safeAmount(cart.getProduct().getPrice()) * qty;
				totalSellingPrice += safeAmount(cart.getProduct().getDiscountPrice()) * qty;
				itemsCount++;
			}
		}

		double totalDiscount = Math.max(0.0, totalMrp - totalSellingPrice);

		m.addAttribute("carts", carts);
		m.addAttribute("itemsCount", itemsCount);
		m.addAttribute("mrp", totalMrp);
		m.addAttribute("sellingPrice", totalSellingPrice);
		m.addAttribute("discountAmount", totalDiscount);
		m.addAttribute("estimatedDelivery", "Delivery by 8 Apr, Wednesday");
	}

	private Address resolveSelectedAddress(Integer userId) {
		List<Address> addresses = addressService.getAddressByUser(userId);
		if (addresses == null || addresses.isEmpty()) {
			return null;
		}

		return addresses.stream()
				.filter(address -> Boolean.TRUE.equals(address.getIsDefault()))
				.findFirst()
				.orElse(addresses.get(0));
	}

	private void applyOrderRequestDefaults(OrderRequest request, UserDtls user, Address selectedAddress) {
		String[] nameParts = splitName(selectedAddress.getFullName());
		if (!StringUtils.hasText(request.getFirstName())) {
			request.setFirstName(nameParts[0]);
		}
		if (!StringUtils.hasText(request.getLastName())) {
			request.setLastName(nameParts[1]);
		}
		if (!StringUtils.hasText(request.getEmail())) {
			request.setEmail(user.getEmail());
		}
		if (!StringUtils.hasText(request.getMobileNo())) {
			request.setMobileNo(selectedAddress.getMobileNumber());
		}
		if (!StringUtils.hasText(request.getAddress())) {
			request.setAddress(selectedAddress.getAddressLine1());
		}
		if (!StringUtils.hasText(request.getCity())) {
			request.setCity(selectedAddress.getCity());
		}
		if (!StringUtils.hasText(request.getState())) {
			request.setState(selectedAddress.getState());
		}
		if (!StringUtils.hasText(request.getPincode())) {
			request.setPincode(selectedAddress.getPincode());
		}
	}

	private String buildFullName(String firstName, String lastName) {
		String first = StringUtils.hasText(firstName) ? firstName.trim() : "";
		String last = StringUtils.hasText(lastName) ? lastName.trim() : "";
		String fullName = (first + " " + last).trim();
		return StringUtils.hasText(fullName) ? fullName : "Delivery contact";
	}

	private String normalizePaymentType(String paymentType) {
		if (!StringUtils.hasText(paymentType)) {
			return "CARD";
		}

		String normalized = paymentType.trim().toUpperCase(Locale.ENGLISH);
		return switch (normalized) {
			case "CARD", "COD", "UPI", "GIFT" -> normalized;
			case "ONLINE" -> "CARD";
			default -> "CARD";
		};
	}

	private Map<String, Object> buildUpiInitiationPayload(PendingUpiCheckout pendingCheckout) {
		OrderRequest orderRequest = pendingCheckout.getOrderRequest();
		return Map.of(
				"success", true,
				"message", "Processing payment. Complete the UPI approval in Razorpay to confirm your order.",
				"checkoutReference", pendingCheckout.getCheckoutReference(),
				"razorpayOrderId", pendingCheckout.getGatewayOrderId(),
				"razorpayKeyId", paymentGatewayService.getPublicKey(),
				"amount", pendingCheckout.getAmountInPaise(),
				"currency", pendingCheckout.getCurrency(),
				"customerName", buildFullName(orderRequest.getFirstName(), orderRequest.getLastName()),
				"customerEmail", orderRequest.getEmail(),
				"customerContact", normalizeRazorpayContact(orderRequest.getMobileNo()));
	}

	private PendingUpiCheckout createPendingUpiCheckout(HttpSession session, OrderRequest request, Integer userId,
			String checkoutType, Integer productId, Integer quantity, int amountInPaise, String description) {
		try {
			String checkoutReference = UUID.randomUUID().toString();
			String receipt = ("BUY_NOW".equalsIgnoreCase(checkoutType) ? "buy_" : "cart_")
					+ checkoutReference.replace("-", "").substring(0, 20);
			PaymentGatewayService.PaymentOrder paymentOrder = paymentGatewayService.createOrder(
					new PaymentGatewayService.PaymentOrderRequest(amountInPaise, "INR", receipt,
							Map.of("checkout_type", checkoutType, "checkout_reference", checkoutReference,
									"description", description)));

			PendingUpiCheckout pendingCheckout = new PendingUpiCheckout();
			pendingCheckout.setCheckoutReference(checkoutReference);
			pendingCheckout.setCheckoutType(checkoutType);
			pendingCheckout.setUserId(userId);
			pendingCheckout.setProductId(productId);
			pendingCheckout.setQuantity(quantity);
			pendingCheckout.setAmountInPaise(paymentOrder.amountInPaise());
			pendingCheckout.setCurrency(paymentOrder.currency());
			pendingCheckout.setGatewayOrderId(paymentOrder.gatewayOrderId());
			pendingCheckout.setUpiId(normalizeUpiId(request.getUpiId()));
			pendingCheckout.setOrderRequest(copyOrderRequest(request));
			getPendingUpiCheckouts(session).put(checkoutReference, pendingCheckout);
			return pendingCheckout;
		} catch (Exception ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, PendingUpiCheckout> getPendingUpiCheckouts(HttpSession session) {
		Object stored = session.getAttribute(PENDING_UPI_CHECKOUTS);
		if (stored instanceof Map<?, ?> map) {
			return (Map<String, PendingUpiCheckout>) map;
		}

		Map<String, PendingUpiCheckout> pending = new HashMap<>();
		session.setAttribute(PENDING_UPI_CHECKOUTS, pending);
		return pending;
	}

	private OrderRequest enrichVerifiedOrderRequest(PendingUpiCheckout pendingCheckout,
			PaymentGatewayService.PaymentVerificationResult verificationResult) {
		OrderRequest request = copyOrderRequest(pendingCheckout.getOrderRequest());
		request.setPaymentType("UPI");
		request.setUpiId(StringUtils.hasText(verificationResult.payerUpiId()) ? verificationResult.payerUpiId()
				: pendingCheckout.getUpiId());
		request.setPaymentStatus("SUCCESS");
		request.setPaymentGatewayProvider(verificationResult.gatewayProvider());
		request.setPaymentGatewayOrderId(verificationResult.gatewayOrderId());
		request.setPaymentGatewayPaymentId(verificationResult.gatewayPaymentId());
		request.setPaymentFailureCode(verificationResult.failureCode());
		request.setPaymentFailureReason(verificationResult.failureReason());
		return request;
	}

	private OrderRequest copyOrderRequest(OrderRequest source) {
		OrderRequest target = new OrderRequest();
		target.setFirstName(source.getFirstName());
		target.setLastName(source.getLastName());
		target.setEmail(source.getEmail());
		target.setMobileNo(source.getMobileNo());
		target.setAddress(source.getAddress());
		target.setCity(source.getCity());
		target.setState(source.getState());
		target.setPincode(source.getPincode());
		target.setPaymentType(source.getPaymentType());
		target.setUpiId(source.getUpiId());
		target.setPaymentStatus(source.getPaymentStatus());
		target.setPaymentGatewayProvider(source.getPaymentGatewayProvider());
		target.setPaymentGatewayOrderId(source.getPaymentGatewayOrderId());
		target.setPaymentGatewayPaymentId(source.getPaymentGatewayPaymentId());
		target.setPaymentFailureCode(source.getPaymentFailureCode());
		target.setPaymentFailureReason(source.getPaymentFailureReason());
		return target;
	}

	private double resolveCartSubtotal(List<Cart> carts) {
		if (carts == null || carts.isEmpty()) {
			return 0.0;
		}
		return safeAmount(carts.get(carts.size() - 1).getTotalOrderPrice());
	}

	private int toPaise(double amount) {
		return (int) Math.round(Math.max(0.0, amount) * 100);
	}

	private String normalizeUpiId(String upiId) {
		return StringUtils.hasText(upiId) ? upiId.trim().toLowerCase(Locale.ENGLISH) : "";
	}

	private boolean isValidUpiId(String upiId) {
		return StringUtils.hasText(upiId) && UPI_ID_PATTERN.matcher(upiId.trim()).matches();
	}

	private String normalizeRazorpayContact(String mobileNo) {
		if (!StringUtils.hasText(mobileNo)) {
			return "";
		}

		String digits = mobileNo.replaceAll("[^0-9]", "");
		if (digits.startsWith("91") && digits.length() > 10) {
			return "+" + digits;
		}
		return digits.length() == 10 ? "+91" + digits : "+" + digits;
	}

	private void populateOrderConfirmationModel(Model m, ProductOrder order) {
		LocalDate orderDate = order.getOrderDate() != null ? order.getOrderDate() : LocalDate.now();
		LocalDate estimatedDeliveryDate = orderDate.plusDays(5);
		double totalAmount = safeAmount(order.getPrice()) * (order.getQuantity() == null ? 1 : order.getQuantity());
		String deliveryFullName = order.getOrderAddress() == null ? "Delivery contact"
				: buildFullName(order.getOrderAddress().getFirstName(), order.getOrderAddress().getLastName());

		DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
		DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH);

		m.addAttribute("order", order);
		m.addAttribute("deliveryFullName", deliveryFullName);
		m.addAttribute("estimatedDeliveryShort", estimatedDeliveryDate.format(dayFormatter));
		m.addAttribute("estimatedDeliveryLong", estimatedDeliveryDate.format(fullFormatter));
		m.addAttribute("totalAmount", totalAmount);
		m.addAttribute("isCashOnDelivery", "COD".equalsIgnoreCase(order.getPaymentType()));
		m.addAttribute("paymentLabel", resolvePaymentLabel(order.getPaymentType()));
		m.addAttribute("orderStatusLabel", resolveOrderStatusLabel(order.getStatus()));
	}

	private void populateOrderTrackingModel(Model m, ProductOrder order) {
		populateOrderConfirmationModel(m, order);

		LocalDate orderDate = order.getOrderDate() != null ? order.getOrderDate() : LocalDate.now();
		LocalDate estimatedDeliveryDate = orderDate.plusDays(5);
		int quantity = order.getQuantity() == null || order.getQuantity() < 1 ? 1 : order.getQuantity();
		double listingPrice = safeAmount(order.getProduct() != null ? order.getProduct().getPrice() : 0.0) * quantity;
		double sellingPrice = safeAmount(order.getPrice()) * quantity;
		double totalSavings = Math.max(0.0, listingPrice - sellingPrice);
		boolean cancelled = isCancelledStatus(order.getStatus());
		boolean delivered = "Delivered".equalsIgnoreCase(order.getStatus());

		m.addAttribute("trackingSteps", buildTrackingSteps(order, estimatedDeliveryDate));
		m.addAttribute("listingPrice", listingPrice);
		m.addAttribute("sellingPrice", sellingPrice);
		m.addAttribute("totalSavings", totalSavings);
		m.addAttribute("canCancelOrder", !cancelled && !delivered);
		m.addAttribute("isCancelledOrder", cancelled);
		m.addAttribute("statusSummary", resolveTrackingSummary(order.getStatus(), estimatedDeliveryDate));
		m.addAttribute("statusHint", resolveTrackingHint(order.getStatus(), estimatedDeliveryDate));
	}

	private List<Map<String, Object>> buildTrackingSteps(ProductOrder order, LocalDate estimatedDeliveryDate) {
		LocalDate orderDate = order.getOrderDate() != null ? order.getOrderDate() : LocalDate.now();
		int currentStage = resolveTrackingStage(order.getStatus());
		boolean cancelled = isCancelledStatus(order.getStatus());
		DateTimeFormatter compactFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);

		List<Map<String, Object>> steps = new ArrayList<>();
		steps.add(createTrackingStep(
				"Order confirmed",
				"Your order was placed on " + orderDate.format(compactFormatter),
				cancelled || currentStage > 0,
				!cancelled && currentStage == 0));
		steps.add(createTrackingStep(
				"Packed & ready to ship",
				"Expected by " + orderDate.plusDays(1).format(compactFormatter),
				currentStage > 1,
				!cancelled && currentStage == 1));
		steps.add(createTrackingStep(
				"Out for delivery",
				"Planned for " + estimatedDeliveryDate.minusDays(1).format(compactFormatter),
				currentStage > 2,
				!cancelled && currentStage == 2));
		steps.add(createTrackingStep(
				"Delivered",
				(currentStage == 3 ? "Delivered on " : "Expected by ") + estimatedDeliveryDate.format(compactFormatter),
				false,
				!cancelled && currentStage == 3));
		return steps;
	}

	private Map<String, Object> createTrackingStep(String title, String description, boolean completed,
			boolean current) {
		Map<String, Object> step = new LinkedHashMap<>();
		step.put("title", title);
		step.put("description", description);
		step.put("completed", completed);
		step.put("current", current);
		return step;
	}

	private int resolveTrackingStage(String status) {
		if ("Delivered".equalsIgnoreCase(status)) {
			return 3;
		}
		if ("Out for Delivery".equalsIgnoreCase(status)) {
			return 2;
		}
		if ("Product Packed".equalsIgnoreCase(status)) {
			return 1;
		}
		return 0;
	}

	private String resolveTrackingSummary(String status, LocalDate estimatedDeliveryDate) {
		if (isCancelledStatus(status)) {
			return "This order has been cancelled. Reach out to support if you need help with a refund or replacement.";
		}
		if ("Delivered".equalsIgnoreCase(status)) {
			return "Delivered on "
					+ estimatedDeliveryDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH));
		}
		if ("Out for Delivery".equalsIgnoreCase(status)) {
			return "Your package is out for delivery and should arrive today.";
		}
		if ("Product Packed".equalsIgnoreCase(status)) {
			return "Your package is packed and moving to the final delivery hub.";
		}
		return "Your order is confirmed and preparing for dispatch.";
	}

	private String resolveTrackingHint(String status, LocalDate estimatedDeliveryDate) {
		if (isCancelledStatus(status)) {
			return "You can still review the item details, payment method, and delivery information from this page.";
		}
		if ("Delivered".equalsIgnoreCase(status)) {
			return "Need a return or refund? Open My Orders to start a request for delivered items.";
		}
		if ("Out for Delivery".equalsIgnoreCase(status)) {
			return "Keep your phone nearby. The delivery partner may call before arrival.";
		}
		return "Expected delivery by "
				+ estimatedDeliveryDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH));
	}

	private String resolvePaymentLabel(String paymentType) {
		if ("COD".equalsIgnoreCase(paymentType)) {
			return "Cash on Delivery";
		}
		if (!StringUtils.hasText(paymentType)) {
			return "Card";
		}
		return paymentType.toUpperCase(Locale.ENGLISH);
	}

	private String resolveOrderStatusLabel(String status) {
		if (isCancelledStatus(status)) {
			return "Cancelled";
		}
		if ("Delivered".equalsIgnoreCase(status)) {
			return "Delivered";
		}
		if ("Out for Delivery".equalsIgnoreCase(status)) {
			return "Out for delivery";
		}
		if ("Product Packed".equalsIgnoreCase(status)) {
			return "Packed";
		}
		return "Order confirmed";
	}

	private boolean isCancelledStatus(String status) {
		return "Cancelled".equalsIgnoreCase(status);
	}

	private void refreshLoggedInUser(UserDtls user, HttpServletRequest request, HttpServletResponse response) {
		Authentication authentication = new UsernamePasswordAuthenticationToken(new CustomUser(user), null,
				new CustomUser(user).getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);

		ResponseCookie authCookie = ResponseCookie
				.from(AUTH_COOKIE, jwtUtil.generateToken(user.getEmail(), user.getRole()))
				.httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/")
				.maxAge(jwtUtil.getExpirationSeconds()).build();
		response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
	}

	private String validateOtp(HttpSession session, String codeKey, String valueKey, String expiryKey,
			String expectedValue,
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

	private void storeOtp(HttpSession session, String codeKey, String valueKey, String expiryKey, String otp,
			String value,
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
