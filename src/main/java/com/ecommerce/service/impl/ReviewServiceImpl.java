package com.ecommerce.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.model.Review;
import com.ecommerce.repository.ReviewRepository;
import com.ecommerce.service.ReviewService;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Override
    public Review saveReview(Review review) {
        if (review == null) {
            return null;
        }
        return reviewRepository.save(review);
    }

    @Override
    public List<Review> getReviewsByProduct(Integer productId) {
        return reviewRepository.findByProductId(productId);
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
}
