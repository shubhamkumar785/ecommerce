package com.ecommerce.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
		UserDtls seller = getLoggedInSeller(principal);
		Page<ProductOrder> page = orderService.getOrdersBySeller(seller.getId(), pageNo, pageSize);

		model.addAttribute("orders", page.getContent());
		model.addAttribute("ordersSize", page.getContent().size());
		model.addAttribute("orderCount", page.getTotalElements());
		model.addAttribute("pageNo", page.getNumber());
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("totalPages", page.getTotalPages());
		model.addAttribute("isFirst", page.isFirst());
		model.addAttribute("isLast", page.isLast());

		return "seller/orders";
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
}
