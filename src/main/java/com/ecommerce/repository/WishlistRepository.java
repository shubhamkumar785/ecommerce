package com.ecommerce.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ecommerce.model.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {
	List<Wishlist> findByUserId(Integer userId);
	Boolean existsByUserIdAndProductId(Integer userId, Integer productId);
	void deleteByUserIdAndProductId(Integer userId, Integer productId);
}
