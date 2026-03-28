package com.ecommerce.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;

@RestController
@RequestMapping("/api")
public class ProductRestController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getFilteredProducts(
            @RequestParam(defaultValue = "") String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "") String color,
            @RequestParam(defaultValue = "") String size,
            @RequestParam(required = false) Double rating,
            @RequestParam(defaultValue = "") String sort,
            @RequestParam(defaultValue = "false") Boolean inStock) {

        List<Product> products = productRepository.findFilteredProducts(
                category.isEmpty() ? null : category,
                minPrice,
                maxPrice,
                color.isEmpty() ? null : color,
                size.isEmpty() ? null : size,
                rating);

        // Apply in-stock filter
        if (Boolean.TRUE.equals(inStock)) {
            products = products.stream()
                    .filter(p -> p.getStock() > 0)
                    .collect(Collectors.toList());
        }

        // Apply sorting
        if ("price_asc".equals(sort)) {
            products = products.stream()
                    .sorted((a, b) -> Double.compare(
                            a.getDiscountPrice() != null ? a.getDiscountPrice() : 0,
                            b.getDiscountPrice() != null ? b.getDiscountPrice() : 0))
                    .collect(Collectors.toList());
        } else if ("price_desc".equals(sort)) {
            products = products.stream()
                    .sorted((a, b) -> Double.compare(
                            b.getDiscountPrice() != null ? b.getDiscountPrice() : 0,
                            a.getDiscountPrice() != null ? a.getDiscountPrice() : 0))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> result = products.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("title", p.getTitle());
            map.put("category", p.getCategory());
            map.put("price", p.getPrice());
            map.put("discountPrice", p.getDiscountPrice());
            map.put("discount", p.getDiscount());
            map.put("stock", p.getStock());
            map.put("image", p.getImage() != null ? p.getImage() : "default.png");
            map.put("color", p.getColor() != null ? p.getColor() : "");
            map.put("size", p.getSize() != null ? p.getSize() : "");
            map.put("rating", p.getRating() != null ? p.getRating() : 0.0);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
