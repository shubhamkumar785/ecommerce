package com.ecommerce.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.model.Product;
import com.ecommerce.model.Review;
import com.ecommerce.model.UserDtls;
import com.ecommerce.repository.ProductOrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ReviewRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ReviewService;
import com.ecommerce.util.OrderStatus;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOrderRepository productOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Review saveReview(Review review) {
        if (review == null) {
            return null;
        }
        Review savedReview = reviewRepository.save(review);
        syncProductRating(savedReview.getProduct().getId());
        return savedReview;
    }

    @Override
    public Review saveOrUpdateReview(Integer userId, Integer productId, Integer rating, String comment) {
        if (!canUserReviewProduct(userId, productId)) {
            throw new IllegalArgumentException("Only customers with a delivered order can review this product.");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Please select a rating between 1 and 5.");
        }

        String safeComment = comment == null ? "" : comment.trim();
        if (safeComment.length() > 1000) {
            throw new IllegalArgumentException("Review comment must be 1000 characters or less.");
        }

        UserDtls user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        Review review = reviewRepository.findByUserIdAndProductId(userId, productId)
                .orElseGet(Review::new);

        if (review.getId() == null) {
            review.setUser(user);
            review.setProduct(product);
        }

        review.setRating(rating);
        review.setComment(safeComment);

        Review savedReview = reviewRepository.save(review);
        syncProductRating(productId);
        return savedReview;
    }

    @Override
    public List<Review> getReviewsByProduct(Integer productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Override
    public Review getUserReviewForProduct(Integer userId, Integer productId) {
        return reviewRepository.findByUserIdAndProductId(userId, productId).orElse(null);
    }

    @Override
    public Double getAverageRatingByProduct(Integer productId) {
        return roundToSingleDecimal(reviewRepository.getAverageRatingByProduct(productId));
    }

    @Override
    public Long countProductReviews(Integer productId) {
        return reviewRepository.countByProductId(productId);
    }

    @Override
    public boolean canUserReviewProduct(Integer userId, Integer productId) {
        return productOrderRepository.existsByUserIdAndProductIdAndStatus(userId, productId,
                OrderStatus.DELIVERED.getName());
    }

    @Override
    public List<Review> getReviewsBySeller(Integer sellerId) {
        return reviewRepository.findBySellerId(sellerId);
    }

    @Override
    public Double getAverageRating(Integer sellerId) {
        return reviewRepository.getAverageRatingBySeller(sellerId);
    }

    @Override
    public Long countSellerReviews(Integer sellerId) {
        return reviewRepository.countReviewsBySeller(sellerId);
    }

    private void syncProductRating(Integer productId) {
        productRepository.findById(productId).ifPresent(product -> {
            product.setRating(getAverageRatingByProduct(productId));
            productRepository.save(product);
        });
    }

    private Double roundToSingleDecimal(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}
