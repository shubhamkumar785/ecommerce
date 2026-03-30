package com.ecommerce.service;

import java.util.List;

import com.ecommerce.model.Review;

public interface ReviewService {

    public Review saveReview(Review review);

    public List<Review> getReviewsByProduct(Integer productId);

    public List<Review> getReviewsBySeller(Integer sellerId);

    public Double getAverageRating(Integer sellerId);

    public Long countSellerReviews(Integer sellerId);

}
