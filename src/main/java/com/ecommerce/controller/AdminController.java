package com.ecommerce.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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
import com.ecommerce.service.CategoryService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private CategoryService categoryService;

	@GetMapping("/")
	public String index() {
		return "admin/index";
	}

	@GetMapping("/loadAddProduct")
	public String loadAddProduct() {
		return "admin/add_product";
	}

	@GetMapping("/category")
	public String category(Model m) {
		m.addAttribute("category", categoryService.getAllCategory());
		return "admin/category";
	}

	@PostMapping("/saveCategory")
	public String saveCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {

		String imageName = (file != null && !file.isEmpty()) ? file.getOriginalFilename() : "default.jpg";

		category.setImageName(imageName);

		Boolean existCategory = categoryService.existCategory(category.getName());

		if (existCategory) {
			session.setAttribute("errorMsg", "Category already exist");
		} else {

			Category saveCategory = categoryService.saveCategory(category);

			if (ObjectUtils.isEmpty(saveCategory)) {
				session.setAttribute("errorMsg", "Not saved! internal server error");
			} else {

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths
						.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator + imageName);

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				session.setAttribute("succMsg", "Saved successfully");
			}
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/deleteCategory/{id}")
	public String deleteCategory(@PathVariable int id, HttpSession session) {

		Boolean deleteCategory = categoryService.deleteCategory(id);

		if (deleteCategory) {
			session.setAttribute("succMsg", "Category deleted successfully");
		} else {
			session.setAttribute("errorMsg", "Something went wrong! Try again");
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/loadEditCategory/{id}")
	public String loadEditCategory(@PathVariable int id, Model m) {
		m.addAttribute("category", categoryService.getCategoryById(id));
		return "admin/edit_category";
	}
	@PostMapping("/updateCategory")
	public String updateCategory(@ModelAttribute Category category,
	                             @RequestParam("file") MultipartFile file,
	                             HttpSession session) throws IOException {

	    Category oldCategory = categoryService.getCategoryById(category.getId());

	    if (oldCategory == null) {
	        session.setAttribute("errorMsg", "Category not found");
	        return "redirect:/admin/category";
	    }

	    String imageName = file.isEmpty()
	            ? oldCategory.getImageName()
	            : file.getOriginalFilename();

	    oldCategory.setName(category.getName());
	    oldCategory.setIsActive(category.getIsActive());
	    oldCategory.setImageName(imageName);

	    // ✅ SAVE FIRST
	    Category updateCategory = categoryService.saveCategory(oldCategory);

	    // ✅ THEN USE VARIABLE
	    if (!ObjectUtils.isEmpty(updateCategory)) {

	        if (!file.isEmpty()) {
	            File saveFile = new ClassPathResource("static/img").getFile();

	            Path path = Paths.get(saveFile.getAbsolutePath()
	                    + File.separator
	                    + "category_img"
	                    + File.separator
	                    + imageName);

	            Files.copy(file.getInputStream(), path,
	                    StandardCopyOption.REPLACE_EXISTING);
	        }

	        session.setAttribute("succMsg", "Category updated successfully");

	    } else {
	        session.setAttribute("errorMsg", "Something went wrong! try again");
	    }

	    return "redirect:/admin/loadEditCategory/" + category.getId();
	}


}
