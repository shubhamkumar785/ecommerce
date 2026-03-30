package com.ecommerce.service;

import com.ecommerce.dto.DashboardStats;

public interface DashboardService {
    DashboardStats getGlobalStats();
    DashboardStats getSellerStats(Integer sellerId);
}
