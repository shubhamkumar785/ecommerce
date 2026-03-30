package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardStats {
    private Long totalOrders;
    private Double totalRevenue;
    private Long totalQuantitySold;
    private Double totalProfit;
}
