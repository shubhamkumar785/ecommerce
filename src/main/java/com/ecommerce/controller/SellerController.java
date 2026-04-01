package com.ecommerce.controller;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.dto.DashboardStats;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.ProductOrder;
import com.ecommerce.model.UserDtls;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.DashboardService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/seller")
public class SellerController {

	@Autowired
	private UserService userService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private DashboardService dashboardService;

	@Autowired
	private OrderService orderService;

	@ModelAttribute
	public void addCommonAttributes(Principal principal, Model model) {
		if (principal != null) {
			UserDtls seller = getLoggedInSeller(principal);
			model.addAttribute("user", seller);
			model.addAttribute("countCart", 0);
		}
		model.addAttribute("categorys", categoryService.getAllActiveCategory());
	}

	@GetMapping("/dashboard")
	public String dashboard(Principal principal, Model model) {
		UserDtls seller = getLoggedInSeller(principal);
		DashboardStats stats = dashboardService.getSellerStats(seller.getId());
		Page<Product> recentProducts = productService.getProductsBySeller(seller.getId(), 0, 5);

		model.addAttribute("stats", stats);
		model.addAttribute("recentProducts", recentProducts.getContent());
		model.addAttribute("productCount", recentProducts.getTotalElements());
		return "seller/dashboard";
	}

	@GetMapping("/loadAddProduct")
	public String loadAddProduct(Model model) {
		model.addAttribute("product", new Product());
		model.addAttribute("categories", categoryService.getAllActiveCategory());
		return "seller/add_product";
	}

	@PostMapping("/save-product")
	public String saveProduct(@ModelAttribute Product product, @RequestParam("imageFile") MultipartFile imageFile,
			Principal principal, HttpSession session) {
		UserDtls seller = getLoggedInSeller(principal);
		Product savedProduct = productService.saveSellerProduct(product, imageFile, seller.getId());

		if (ObjectUtils.isEmpty(savedProduct)) {
			session.setAttribute("errorMsg", "Product could not be saved");
			return "redirect:/seller/loadAddProduct";
		}

		session.setAttribute("succMsg",
				"Product saved successfully and is now available in the live catalog");
		return "redirect:/seller/products";
	}

	@GetMapping("/products")
	public String sellerProducts(Principal principal, Model model,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "12") Integer pageSize) {
		UserDtls seller = getLoggedInSeller(principal);
		Page<Product> page = productService.getProductsBySeller(seller.getId(), pageNo, pageSize);

		model.addAttribute("products", page.getContent());
		model.addAttribute("productsSize", page.getContent().size());
		model.addAttribute("productCount", page.getTotalElements());
		model.addAttribute("pageNo", page.getNumber());
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("totalPages", page.getTotalPages());
		model.addAttribute("isFirst", page.isFirst());
		model.addAttribute("isLast", page.isLast());

		return "seller/products";
	}

	@GetMapping("/orders")
	public String sellerOrders(Principal principal, Model model,
			@RequestParam(name = "filter", defaultValue = "all") String filter,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
		UserDtls seller = getLoggedInSeller(principal);
		List<ProductOrder> allSellerOrders = orderService.getOrdersBySeller(seller.getId());
		String activeOrderFilter = normalizeSellerOrderFilter(filter);
		List<ProductOrder> filteredOrders = filterSellerOrders(allSellerOrders, activeOrderFilter).stream()
				.sorted(Comparator.comparing(ProductOrder::getOrderDate,
						Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(ProductOrder::getId, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
		Map<String, Long> orderSummary = buildSellerOrderSummary(allSellerOrders);
		int safePageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
		int filteredOrderCount = filteredOrders.size();
		int totalPages = filteredOrderCount == 0 ? 0 : (int) Math.ceil((double) filteredOrderCount / safePageSize);
		int safePageNo = totalPages == 0 ? 0 : Math.min(Math.max(pageNo, 0), totalPages - 1);
		int fromIndex = Math.min(safePageNo * safePageSize, filteredOrderCount);
		int toIndex = Math.min(fromIndex + safePageSize, filteredOrderCount);
		List<ProductOrder> pagedOrders = filteredOrders.subList(fromIndex, toIndex);

		model.addAttribute("orders", pagedOrders);
		model.addAttribute("ordersSize", pagedOrders.size());
		model.addAttribute("orderCount", filteredOrderCount);
		model.addAttribute("filteredOrderCount", filteredOrderCount);
		model.addAttribute("sellerOrderSummary", orderSummary);
		model.addAttribute("activeOrderFilter", activeOrderFilter);
		model.addAttribute("activeOrderFilterLabel", resolveSellerOrderFilterLabel(activeOrderFilter));
		model.addAttribute("activeOrderFilterDescription", resolveSellerOrderFilterDescription(activeOrderFilter));
		model.addAttribute("pageNo", safePageNo);
		model.addAttribute("pageSize", safePageSize);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("isFirst", safePageNo == 0);
		model.addAttribute("isLast", totalPages == 0 || safePageNo >= totalPages - 1);

		return "seller/orders";
	}

	@GetMapping("/orders/summary")
	@ResponseBody
	public ResponseEntity<Map<String, Long>> sellerOrdersSummary(Principal principal) {
		UserDtls seller = getLoggedInSeller(principal);
		return ResponseEntity.ok(buildSellerOrderSummary(orderService.getOrdersBySeller(seller.getId())));
	}

	@GetMapping("/shop-profile")
	public String shopProfile(Principal principal, Model model) {
		UserDtls seller = getLoggedInSeller(principal);
		Page<Product> page = productService.getProductsBySeller(seller.getId(), 0, 5);

		model.addAttribute("sellerProducts", page.getContent());
		model.addAttribute("productCount", page.getTotalElements());
		return "seller/shop_profile";
	}

	@GetMapping("/profile")
	public String accountSettings(Principal principal, Model model) {
		UserDtls seller = getLoggedInSeller(principal);
		model.addAttribute("sellerProfile", seller);
		return "seller/profile";
	}

	private UserDtls getLoggedInSeller(Principal principal) {
		return userService.getUserByEmail(principal.getName());
	}

	private Map<String, Long> buildSellerOrderSummary(List<ProductOrder> orders) {
		List<ProductOrder> safeOrders = orders == null ? List.of() : orders;
		long totalOrders = safeOrders.size();
		long cancelledOrders = safeOrders.stream()
				.filter(order -> order != null && isCancelledStatus(order.getStatus()))
				.count();
		long completedOrders = safeOrders.stream()
				.filter(order -> order != null && isCompletedStatus(order.getStatus()))
				.count();

		return Map.of(
				"totalOrders", totalOrders,
				"cancelledOrders", cancelledOrders,
				"completedOrders", completedOrders);
	}

	private boolean isCancelledStatus(String status) {
		return "Cancelled".equalsIgnoreCase(status);
	}

	private boolean isCompletedStatus(String status) {
		return "Delivered".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status);
	}

	private String normalizeSellerOrderFilter(String filter) {
		if (filter == null || filter.isBlank()) {
			return "all";
		}

		String normalized = filter.trim().toLowerCase(Locale.ENGLISH);
		return switch (normalized) {
		case "cancelled", "received", "all" -> normalized;
		default -> "all";
		};
	}

	private List<ProductOrder> filterSellerOrders(List<ProductOrder> orders, String filter) {
		List<ProductOrder> safeOrders = orders == null ? List.of() : orders;
		return switch (filter) {
		case "cancelled" -> safeOrders.stream()
				.filter(order -> order != null && isCancelledStatus(order.getStatus()))
				.toList();
		case "received" -> safeOrders.stream()
				.filter(order -> order != null && isCompletedStatus(order.getStatus()))
				.toList();
		default -> safeOrders;
		};
	}

	private String resolveSellerOrderFilterLabel(String filter) {
		return switch (filter) {
		case "cancelled" -> "Cancelled Orders";
		case "received" -> "Orders Received / Completed";
		default -> "Total Orders";
		};
	}

	private String resolveSellerOrderFilterDescription(String filter) {
		return switch (filter) {
		case "cancelled" -> "Showing every cancelled order for your products.";
		case "received" -> "Showing all successfully delivered or completed orders.";
		default -> "Showing every order placed for your products.";
		};
	}
}
