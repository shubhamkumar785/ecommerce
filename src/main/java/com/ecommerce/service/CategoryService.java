package com.ecommerce.service;

import java.util.List;

import com.ecommerce.model.Category;

public interface CategoryService {

	public Category saveCategory(Category category);
	
	public Boolean existCategory(String name);
	
	public List<Category> getAllCategory();
}
 