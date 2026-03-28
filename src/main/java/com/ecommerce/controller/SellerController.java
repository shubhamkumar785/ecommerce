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

import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.UserDtls;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.ProductService;
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
		
		m.addAttribute("totalProducts", productPage.getTotalElements());
		m.addAttribute("totalOrders", 150); // MOCKED for UI
		m.addAttribute("totalRevenue", 45000); // MOCKED for UI
		
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
}
