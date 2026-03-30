package com.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByProductId(Integer productId);

    @Query("SELECT r FROM Review r WHERE r.product.seller.id = :sellerId ORDER BY r.createdAt DESC")
    List<Review> findBySellerId(@Param("sellerId") Integer sellerId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.seller.id = :sellerId")
    Double getAverageRatingBySeller(@Param("sellerId") Integer sellerId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.seller.id = :sellerId")
    Long countReviewsBySeller(@Param("sellerId") Integer sellerId);
}
