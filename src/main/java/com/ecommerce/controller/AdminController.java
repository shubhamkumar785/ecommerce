package com.ecommerce.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ecommerce.model.UserDtls;
import com.ecommerce.service.UserService;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private UserService userService;

	@GetMapping("/")
	public String index(Principal p, Model m) {
		if (p != null) {
			UserDtls admin = userService.getUserByEmail(p.getName());
			m.addAttribute("adminName", admin.getName());
			m.addAttribute("adminEmail", admin.getEmail());
		} else {
			m.addAttribute("adminName", "Administrator");
			m.addAttribute("adminEmail", "admin@example.com");
		}
		return "admin/dashboard";
	}

	@GetMapping("/products")
	public String products() {
		return "admin/products"; // Placeholder if template exists
	}

	@GetMapping("/orders")
	public String orders() {
		return "admin/orders"; // Placeholder if template exists
	}

	@GetMapping("/category")
	public String category() {
		return "admin/category"; // Placeholder if template exists
	}

	@GetMapping("/loadAddProduct")
	public String addProduct() {
		return "admin/add_product"; // Placeholder if template exists
	}
}
