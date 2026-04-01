package com.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.model.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

	List<Product> findByIsActiveTrueAndSellerIsNotNullOrderByIdDesc();

	Page<Product> findByIsActiveTrueAndSellerIsNotNull(Pageable pageable);

	List<Product> findByCategory(String category);

	List<Product> findByIsActiveTrueAndSellerIsNotNullAndCategoryIgnoreCaseOrderByIdDesc(String category);

	Page<Product> findByIsActiveTrueAndSellerIsNotNullAndCategoryIgnoreCase(String category, Pageable pageable);

	List<Product> findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(String ch, String ch2);

	Page<Product> findByCategory(Pageable pageable, String category);

	Page<Product> findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(String ch, String ch2,
			Pageable pageable);

	@Query("""
			SELECT p FROM Product p
			WHERE p.isActive = true
			  AND p.seller IS NOT NULL
			  AND (:category IS NULL OR :category = '' OR lower(p.category) = lower(:category))
			  AND (
			       :search IS NULL
			       OR :search = ''
			       OR lower(p.title) LIKE lower(concat('%', :search, '%'))
			       OR lower(p.category) LIKE lower(concat('%', :search, '%'))
			  )
			""")
	Page<Product> searchActiveProducts(@Param("category") String category, @Param("search") String search,
			Pageable pageable);

	Optional<Product> findByIdAndIsActiveTrueAndSellerIsNotNull(Integer id);

	@Query("SELECT p FROM Product p WHERE p.seller.id = :sellerId")
	Page<Product> findBySellerId(@org.springframework.web.bind.annotation.RequestParam("sellerId") Integer sellerId,
			Pageable pageable);

	@Query("SELECT p FROM Product p WHERE p.isActive = true " +
			"AND p.seller IS NOT NULL " +
			"AND (:category IS NULL OR :category = '' OR p.category = :category) " +
			"AND (:minPrice IS NULL OR p.discountPrice >= :minPrice) " +
			"AND (:maxPrice IS NULL OR p.discountPrice <= :maxPrice) " +
			"AND (:color IS NULL OR :color = '' OR p.color = :color) " +
			"AND (:size IS NULL OR :size = '' OR p.size = :size) " +
			"AND (:rating IS NULL OR p.rating >= :rating) " +
			"ORDER BY p.id DESC")
	List<Product> findFilteredProducts(
			@Param("category") String category,
			@Param("minPrice") Double minPrice,
			@Param("maxPrice") Double maxPrice,
			@Param("color") String color,
			@Param("size") String size,
			@Param("rating") Double rating);
}
