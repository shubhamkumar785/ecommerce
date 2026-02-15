package com.ecommerce.service.impl;

import java.util.List;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.model.Category;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.CategoryService;


@Service
public class CategoryServiceImpl implements CategoryService{




	@Autowired 
	private CategoryRepository categoryRepository;
	
	@Override
	public Category saveCategory(Category category) {
		return categoryRepository.save(category);
	}

	
	@Override
	public Boolean existCategory(String name) {
		return categoryRepository.existsByName(name);
	}
	

	
	@Override
	public List<Category> getAllCategory() {
	    return categoryRepository.findAll();
	}


}

