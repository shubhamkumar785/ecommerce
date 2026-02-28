package com.ecommerce.service;
import java.util.List;

import com.ecommerce.model.Cart;

public interface CartService {

	public Cart saveCart(Integer productId, Integer userId);

	public List<Cart> getCartsByUser(Integer userId);

}
