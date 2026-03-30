package com.ecommerce.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.dto.DashboardStats;
import com.ecommerce.repository.ProductOrderRepository;
import com.ecommerce.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private ProductOrderRepository orderRepository;

    @Override
    public DashboardStats getGlobalStats() {
        return DashboardStats.builder()
                .totalOrders(orderRepository.countTotalOrders())
                .totalRevenue(orderRepository.calculateTotalRevenueGlobal())
                .totalQuantitySold(orderRepository.calculateTotalQuantitySoldGlobal())
                .totalProfit(orderRepository.calculateTotalProfitGlobal())
                .build();
    }

    @Override
    public DashboardStats getSellerStats(Integer sellerId) {
        return DashboardStats.builder()
                .totalOrders(orderRepository.countTotalOrdersBySeller(sellerId))
                .totalRevenue(orderRepository.calculateTotalRevenueBySeller(sellerId))
                .totalQuantitySold(orderRepository.calculateTotalQuantitySoldBySeller(sellerId))
                .totalProfit(orderRepository.calculateTotalProfitBySeller(sellerId))
                .build();
    }
}
