package com.ecommerce.controller;

import java.security.Principal;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

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
	public String addToCart(@RequestParam Integer pid, @RequestParam Integer uid, HttpSession session) {
		Cart saveCart = cartService.saveCart(pid, uid);

		if (ObjectUtils.isEmpty(saveCart)) {
			session.setAttribute("errorMsg", "Product add to cart failed");
		} else {
			session.setAttribute("succMsg", "Product added to cart");
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
		}
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
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {
		UserDtls updateUserProfile = userService.updateUserProfile(user, img);
		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("errorMsg", "Profile not updated");
		} else {
			session.setAttribute("succMsg", "Profile Updated");
		}
		return "redirect:/user/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
			HttpSession session) {
		UserDtls loggedInUserDetails = getLoggedInUserDetails(p);

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
	public String addToWishlist(@RequestParam Integer pid, Principal p, HttpSession session) {
		UserDtls user = getLoggedInUserDetails(p);
		wishlistService.addToWishlist(user.getId(), pid);
		session.setAttribute("succMsg", "Product added to wishlist");
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
}