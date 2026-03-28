package com.ecommerce.service;

import java.util.List;
import com.ecommerce.model.Wishlist;

public interface WishlistService {
	public Boolean addToWishlist(Integer userId, Integer productId);
	public Boolean removeFromWishlist(Integer userId, Integer productId);
	public List<Wishlist> getWishlistByUser(Integer userId);
	public Boolean isInWishlist(Integer userId, Integer productId);
}
