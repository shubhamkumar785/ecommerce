package com.ecommerce.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.ecommerce.service.DashboardService;
import com.ecommerce.dto.DashboardStats;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.ProductOrder;
import com.ecommerce.model.ReturnRequest;
import com.ecommerce.model.Review;
import com.ecommerce.model.UserDtls;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.ReturnRequestService;
import com.ecommerce.service.ReviewService;
import com.ecommerce.service.UserService;
import com.ecommerce.util.CommonUtil;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/seller")
public class SellerController {

	@Autowired
	private ProductService productService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private UserService userService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private DashboardService dashboardService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private ReturnRequestService returnRequestService;

	@Autowired
	private ReviewService reviewService;

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {
		if (p != null) {
			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
		}
		List<Category> allCategory = categoryService.getAllCategory();
		m.addAttribute("categories", allCategory);
	}

	@GetMapping("/dashboard")
	public String dashboard(Principal p, Model m) {
		UserDtls seller = commonUtil.getLoggedInUserDetails(p);
		Page<Product> productPage = productService.getProductsBySeller(seller.getId(), 0, 1);
		
		DashboardStats stats = dashboardService.getSellerStats(seller.getId());
		
		m.addAttribute("totalProducts", productPage.getTotalElements());
		m.addAttribute("totalOrders", stats.getTotalOrders());
		m.addAttribute("totalRevenue", stats.getTotalRevenue());
		m.addAttribute("totalProfit", stats.getTotalProfit());
		m.addAttribute("totalQuantitySold", stats.getTotalQuantitySold());
		
		return "seller/dashboard";
	}

	@GetMapping("/loadAddProduct")
	public String loadAddProduct() {
		return "seller/add_product";
	}

	@GetMapping("/products")
	public String loadViewProduct(Model m, Principal p, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
		UserDtls seller = commonUtil.getLoggedInUserDetails(p);
		Page<Product> page = productService.getProductsBySeller(seller.getId(), pageNo, pageSize);
		m.addAttribute("products", page.getContent());
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());
		return "seller/products";
	}

	@PostMapping("/saveProduct")
	public String saveProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image,
			HttpSession session, Principal p) throws IOException {

		UserDtls seller = commonUtil.getLoggedInUserDetails(p);
		String imageName = image.isEmpty() ? "default.png" : image.getOriginalFilename();

		product.setImage(imageName);
		product.setDiscount(0);
		product.setDiscountPrice(product.getPrice());
		product.setSeller(seller);
		product.setIsActive(true);

		Product saveProduct = productService.saveProduct(product);

		if (!ObjectUtils.isEmpty(saveProduct)) {
			File saveFile = new ClassPathResource("static/img").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "product_img" + File.separator
					+ image.getOriginalFilename());

			Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			session.setAttribute("succMsg", "Product Saved Successfully");
		} else {
			session.setAttribute("errorMsg", "Something went wrong on server");
		}

		return "redirect:/seller/loadAddProduct";
	}

	@GetMapping("/deleteProduct/{id}")
	public String deleteProduct(@PathVariable int id, HttpSession session) {
		Boolean deleteProduct = productService.deleteProduct(id);
		if (deleteProduct) {
			session.setAttribute("succMsg", "Product deleted successfully");
		} else {
			session.setAttribute("errorMsg", "Something went wrong on server");
		}
		return "redirect:/seller/products";
	}

	@GetMapping("/editProduct/{id}")
	public String editProduct(@PathVariable int id, Model m) {
		m.addAttribute("product", productService.getProductById(id));
		return "seller/edit_product";
	}

	@PostMapping("/updateProduct")
	public String updateProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image,
			HttpSession session) {

		if (product.getDiscount() < 0 || product.getDiscount() > 100) {
			session.setAttribute("errorMsg", "invalid Discount");
		} else {
			Product updateProduct = productService.updateProduct(product, image);
			if (!ObjectUtils.isEmpty(updateProduct)) {
				session.setAttribute("succMsg", "Product updated successfully");
			} else {
				session.setAttribute("errorMsg", "Something went wrong on server");
			}
		}
		return "redirect:/seller/editProduct/" + product.getId();
	}

	@GetMapping("/profile")
	public String profile() {
		return "seller/profile";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {
		UserDtls updateUserProfile = userService.updateUserProfile(user, img);
		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Profile not updated");
		} else {
			session.setAttribute("succMsg", "Profile Updated");
		}
		return "redirect:/seller/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
			HttpSession session) {
		UserDtls loggedInUserDetails = commonUtil.getLoggedInUserDetails(p);

		boolean matches = passwordEncoder.matches(currentPassword, loggedInUserDetails.getPassword());

		if (matches) {
			String encodePassword = passwordEncoder.encode(newPassword);
			loggedInUserDetails.setPassword(encodePassword);
			UserDtls updateUser = userService.updateUser(loggedInUserDetails);
			if (ObjectUtils.isEmpty(updateUser)) {
				session.setAttribute("errorMsg", "Password not updated !! Error in server");
			} else {
				session.setAttribute("succMsg", "Password Updated successfully");
			}
		} else {
			session.setAttribute("errorMsg", "Current Password incorrect");
		}

		return "redirect:/seller/profile";
	}

	@GetMapping("/orders")
	public String getAllOrders(Principal p, Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
		UserDtls seller = commonUtil.getLoggedInUserDetails(p);
		Page<ProductOrder> page = orderService.getOrdersBySeller(seller.getId(), pageNo, pageSize);
		List<ReturnRequest> returnRequests = returnRequestService.getReturnRequestsBySeller(seller.getId());

		m.addAttribute("orders", page.getContent());
		m.addAttribute("returnRequests", returnRequests);
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "seller/orders";
	}

	@PostMapping("/update-order-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam String status, HttpSession session) {
		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);
		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Status Updated");
		} else {
			session.setAttribute("errorMsg", "Status not updated");
		}
		return "redirect:/seller/orders";
	}

	@PostMapping("/update-return-status")
	public String updateReturnStatus(@RequestParam Integer id, @RequestParam String status,
			@RequestParam String adminComment, HttpSession session) {
		ReturnRequest updateReturnStatus = returnRequestService.updateReturnStatus(id, status, adminComment);
		if (!ObjectUtils.isEmpty(updateReturnStatus)) {
			session.setAttribute("succMsg", "Return Status Updated");
		} else {
			session.setAttribute("errorMsg", "Return Status not updated");
		}
		return "redirect:/seller/orders";
	}

	@GetMapping("/shop-profile")
	public String shopProfile(Principal p, Model m) {
		UserDtls seller = commonUtil.getLoggedInUserDetails(p);
		Double avgRating = reviewService.getAverageRating(seller.getId());
		List<Review> reviews = reviewService.getReviewsBySeller(seller.getId());
		Long totalReviews = reviewService.countSellerReviews(seller.getId());

		m.addAttribute("avgRating", String.format("%.1f", avgRating));
		m.addAttribute("reviews", reviews);
		m.addAttribute("totalReviews", totalReviews);
		return "seller/shop_profile";
	}
}
