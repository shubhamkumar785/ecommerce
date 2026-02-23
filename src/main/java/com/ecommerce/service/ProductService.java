package com.ecommerce.service;

import java.util.List;

import com.ecommerce.model.Product;

public interface ProductService {
	
	public Product saveProduct(Product product);
	
	public List<Product> getAllProduct();

	public Boolean deleteProduct(Integer id);
}
