package com.ecommerce.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;   // ⭐ ADD THIS
import org.springframework.util.ObjectUtils;

import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.ProductService;

@Service  
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

	@Override
	public List<Product> getAllProduct() {
		return productRepository.findAll();
	}

	@Override
	public Boolean deleteProduct(Integer id) {

	    Product product = productRepository.findById(id).orElse(null);

	    if(!ObjectUtils.isEmpty(product)) {   
	        productRepository.delete(product);
	        return true;
	    }

	    return false;
	}
}
