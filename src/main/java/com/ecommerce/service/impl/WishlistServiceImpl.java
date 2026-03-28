package com.ecommerce.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.model.Product;
import com.ecommerce.model.UserDtls;
import com.ecommerce.model.Wishlist;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.WishlistRepository;
import com.ecommerce.service.WishlistService;

@Service
public class WishlistServiceImpl implements WishlistService {

	@Autowired
	private WishlistRepository wishlistRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Override
	public Boolean addToWishlist(Integer userId, Integer productId) {
		if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) return true;
		
		UserDtls user = userRepository.findById(userId).orElse(null);
		Product product = productRepository.findById(productId).orElse(null);
		
		if (user != null && product != null) {
			Wishlist wishlist = new Wishlist();
			wishlist.setUser(user);
			wishlist.setProduct(product);
			wishlistRepository.save(wishlist);
			return true;
		}
		return false;
	}

	@Override
	@Transactional
	public Boolean removeFromWishlist(Integer userId, Integer productId) {
		if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
			wishlistRepository.deleteByUserIdAndProductId(userId, productId);
			return true;
		}
		return false;
	}

	@Override
	public List<Wishlist> getWishlistByUser(Integer userId) {
		return wishlistRepository.findByUserId(userId);
	}

	@Override
	public Boolean isInWishlist(Integer userId, Integer productId) {
		return wishlistRepository.existsByUserIdAndProductId(userId, productId);
	}
}
