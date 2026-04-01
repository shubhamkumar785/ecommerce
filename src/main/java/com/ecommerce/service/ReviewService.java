package com.ecommerce.service;

import java.util.List;

import com.ecommerce.model.Review;

public interface ReviewService {

    public Review saveReview(Review review);

    public Review saveOrUpdateReview(Integer userId, Integer productId, Integer rating, String comment);

    public List<Review> getReviewsByProduct(Integer productId);

    public Review getUserReviewForProduct(Integer userId, Integer productId);

    public Double getAverageRatingByProduct(Integer productId);

    public Long countProductReviews(Integer productId);

    public boolean canUserReviewProduct(Integer userId, Integer productId);

    public List<Review> getReviewsBySeller(Integer sellerId);

    public Double getAverageRating(Integer sellerId);

    public Long countSellerReviews(Integer sellerId);

}
