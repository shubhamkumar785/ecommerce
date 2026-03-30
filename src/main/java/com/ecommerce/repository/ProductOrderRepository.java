package com.ecommerce.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ecommerce.model.ProductOrder;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, Integer> {

	List<ProductOrder> findByUserId(Integer userId);

	ProductOrder findByOrderId(String orderId);

	@Query(value = """
			select coalesce(sum(case when status <> 'Cancelled' then price * quantity else 0 end), 0)
			from product_order
			""", nativeQuery = true)
	BigDecimal calculateTotalRevenue();

	@Query(value = """
			select date_format(coalesce(order_date, current_date), '%Y-%m') as month_label,
			       coalesce(sum(case when status <> 'Cancelled' then price * quantity else 0 end), 0) as total
			from product_order
			group by date_format(coalesce(order_date, current_date), '%Y-%m')
			order by month_label
			""", nativeQuery = true)
	List<Object[]> sumMonthlyRevenue();

	@Query(value = """
			select date_format(coalesce(order_date, current_date), '%Y-%m') as month_label, count(*) as total
			from product_order
			group by date_format(coalesce(order_date, current_date), '%Y-%m')
			order by month_label
			""", nativeQuery = true)
	List<Object[]> countMonthlyOrders();

	// Global Stats
	@Query("SELECT COUNT(o) FROM ProductOrder o")
	Long countTotalOrders();

	@Query("SELECT COALESCE(SUM(o.price * o.quantity), 0.0) FROM ProductOrder o WHERE o.status <> 'Cancelled'")
	Double calculateTotalRevenueGlobal();

	@Query("SELECT COALESCE(SUM(o.quantity), 0) FROM ProductOrder o WHERE o.status <> 'Cancelled'")
	Long calculateTotalQuantitySoldGlobal();

	@Query("SELECT COALESCE(SUM((o.price - COALESCE(o.costPrice, 0.0)) * o.quantity), 0.0) FROM ProductOrder o WHERE o.status <> 'Cancelled'")
	Double calculateTotalProfitGlobal();

	// Seller Specific Stats
	@Query("SELECT COUNT(o) FROM ProductOrder o WHERE o.product.seller.id = :sellerId")
	Long countTotalOrdersBySeller(Integer sellerId);

	@Query("SELECT COALESCE(SUM(o.price * o.quantity), 0.0) FROM ProductOrder o WHERE o.product.seller.id = :sellerId AND o.status <> 'Cancelled'")
	Double calculateTotalRevenueBySeller(Integer sellerId);

	@Query("SELECT COALESCE(SUM(o.quantity), 0) FROM ProductOrder o WHERE o.product.seller.id = :sellerId AND o.status <> 'Cancelled'")
	Long calculateTotalQuantitySoldBySeller(Integer sellerId);

	@Query("SELECT COALESCE(SUM((o.price - COALESCE(o.costPrice, 0.0)) * o.quantity), 0.0) FROM ProductOrder o WHERE o.product.seller.id = :sellerId AND o.status <> 'Cancelled'")
	Double calculateTotalProfitBySeller(Integer sellerId);

	List<ProductOrder> findByProductSellerId(Integer sellerId);

	@Query("SELECT o FROM ProductOrder o WHERE o.product.seller.id = :sellerId ORDER BY o.orderDate DESC")
	Page<ProductOrder> findByProductSellerId(Integer sellerId, org.springframework.data.domain.Pageable pageable);

}
