package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DashboardStats {
    private Long totalOrders;
    private Double totalRevenue;
    private Long totalQuantitySold;
    private Double totalProfit;

    public DashboardStats() {}

    public DashboardStats(Long totalOrders, Double totalRevenue, Long totalQuantitySold, Double totalProfit) {
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.totalQuantitySold = totalQuantitySold;
        this.totalProfit = totalProfit;
    }

    public static DashboardStatsBuilder builder() {
        return new DashboardStatsBuilder();
    }

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Long getTotalQuantitySold() { return totalQuantitySold; }
    public void setTotalQuantitySold(Long totalQuantitySold) { this.totalQuantitySold = totalQuantitySold; }

    public Double getTotalProfit() { return totalProfit; }
    public void setTotalProfit(Double totalProfit) { this.totalProfit = totalProfit; }

    public static class DashboardStatsBuilder {
        private Long totalOrders;
        private Double totalRevenue;
        private Long totalQuantitySold;
        private Double totalProfit;

        public DashboardStatsBuilder totalOrders(Long totalOrders) { this.totalOrders = totalOrders; return this; }
        public DashboardStatsBuilder totalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; return this; }
        public DashboardStatsBuilder totalQuantitySold(Long totalQuantitySold) { this.totalQuantitySold = totalQuantitySold; return this; }
        public DashboardStatsBuilder totalProfit(Double totalProfit) { this.totalProfit = totalProfit; return this; }

        public DashboardStats build() {
            return new DashboardStats(totalOrders, totalRevenue, totalQuantitySold, totalProfit);
        }
    }
}
