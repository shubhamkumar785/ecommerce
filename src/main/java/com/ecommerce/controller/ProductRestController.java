package com.ecommerce.controller;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.ecommerce.model.UserDtls;
import com.ecommerce.model.Cart;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.WishlistService;
import com.ecommerce.service.UserService;
import com.ecommerce.service.CartService;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import java.security.Principal;

@RestController
@RequestMapping("/api")
public class ProductRestController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartService cartService;
    
    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/home-data")
    public ResponseEntity<Map<String, Object>> getHomeData() {
        List<Product> liveProducts = productService.getAllActiveProducts("");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("categories", categoryService.getAllActiveCategory().stream()
                .map(category -> {
                    long productCount = liveProducts.stream()
                            .filter(product -> category.getName() != null
                                    && category.getName().equalsIgnoreCase(product.getCategory()))
                            .count();

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", category.getName());
                    item.put("imageName", category.getImageName());
                    item.put("productCount", productCount);
                    return item;
                })
                .filter(item -> ((Long) item.get("productCount")) > 0)
                .collect(Collectors.toList()));

        Product heroProduct = liveProducts.stream()
                .filter(product -> product.getStock() > 0)
                .sorted(Comparator
                        .comparingInt(Product::getDiscount).reversed()
                        .thenComparing(product -> product.getRating() == null ? 0.0 : product.getRating(),
                                Comparator.reverseOrder())
                        .thenComparing(Product::getId, Comparator.reverseOrder()))
                .findFirst()
                .orElse(liveProducts.stream().findFirst().orElse(null));

        Product secondaryProduct = liveProducts.stream()
                .filter(product -> heroProduct == null || !product.getId().equals(heroProduct.getId()))
                .filter(product -> product.getStock() > 0)
                .sorted(Comparator
                        .comparing((Product product) -> product.getRating() == null ? 0.0 : product.getRating())
                        .reversed()
                        .thenComparingInt(Product::getDiscount).reversed()
                        .thenComparing(Product::getId, Comparator.reverseOrder()))
                .findFirst()
                .orElse(liveProducts.stream()
                        .filter(product -> heroProduct == null || !product.getId().equals(heroProduct.getId()))
                        .findFirst()
                        .orElse(null));

        response.put("heroBanner", buildHomeBanner(heroProduct, "Featured", "Live from the database"));
        response.put("secondaryBanner", buildHomeBanner(secondaryProduct, "Trending now", "Fresh picks for you"));

        response.put("categoryHighlights", liveProducts.stream()
                .collect(Collectors.groupingBy(product -> product.getCategory() == null ? "" : product.getCategory(),
                        LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().isBlank())
                .map(entry -> buildCategoryHighlight(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparing((Map<String, Object> item) -> ((Integer) item.get("productCount"))).reversed()
                        .thenComparing(item -> ((Integer) item.get("discount")), Comparator.reverseOrder()))
                .limit(2)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

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

    @GetMapping("/cart/add")
    public ResponseEntity<Map<String, Object>> addToCart(@RequestParam Integer pid, Principal p) {
        Map<String, Object> response = new HashMap<>();
        
        if (p == null) {
            response.put("status", "error");
            response.put("message", "Please login to add products to cart");
            return ResponseEntity.status(401).body(response);
        }

        String email = p.getName();
        UserDtls user = userService.getUserByEmail(email);
        
        Cart saveCart = cartService.saveCart(pid, user.getId());

        if (ObjectUtils.isEmpty(saveCart)) {
            response.put("status", "error");
            response.put("message", "Failed to add product to cart");
            return ResponseEntity.status(500).body(response);
        } else {
            response.put("status", "success");
            response.put("message", "Product added to cart successfully");
            response.put("cartCount", cartService.getCountCart(user.getId()));
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/wishlist/add")
    public ResponseEntity<Map<String, Object>> addToWishlist(@RequestParam Integer pid, Principal p) {
        Map<String, Object> response = new HashMap<>();
        
        if (p == null) {
            response.put("status", "error");
            response.put("message", "Please login to add to wishlist");
            return ResponseEntity.status(401).body(response);
        }

        String email = p.getName();
        UserDtls user = userService.getUserByEmail(email);
        
        Boolean added = wishlistService.addToWishlist(user.getId(), pid);

        if (added) {
            response.put("status", "success");
            response.put("message", "Product added to wishlist successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "info");
            response.put("message", "Product is already in your wishlist");
            return ResponseEntity.ok(response);
        }
    }

    private Map<String, Object> buildHomeBanner(Product product, String badge, String subtitleFallback) {
        if (product == null) {
            return null;
        }

        Map<String, Object> banner = new LinkedHashMap<>();
        banner.put("id", product.getId());
        banner.put("title", product.getTitle());
        banner.put("category", product.getCategory());
        banner.put("image", product.getImage() != null ? product.getImage() : "default.png");
        banner.put("discount", product.getDiscount());
        banner.put("stock", product.getStock());
        banner.put("price", product.getPrice());
        banner.put("discountPrice", product.getDiscountPrice());
        banner.put("rating", product.getRating() != null ? product.getRating() : 0.0);
        banner.put("badge", badge);
        banner.put("subtitle",
                product.getCategory() != null && !product.getCategory().isBlank() ? product.getCategory()
                        : subtitleFallback);
        return banner;
    }

    private Map<String, Object> buildCategoryHighlight(String category, List<Product> products) {
        Product leadProduct = products.stream()
                .sorted(Comparator
                        .comparingInt(Product::getDiscount).reversed()
                        .thenComparing(product -> product.getRating() == null ? 0.0 : product.getRating(),
                                Comparator.reverseOrder())
                        .thenComparing(Product::getId, Comparator.reverseOrder()))
                .findFirst()
                .orElse(products.get(0));

        Map<String, Object> highlight = new LinkedHashMap<>();
        highlight.put("category", category);
        highlight.put("productCount", products.size());
        highlight.put("discount", leadProduct.getDiscount());
        highlight.put("image", leadProduct.getImage() != null ? leadProduct.getImage() : "default.png");
        highlight.put("productId", leadProduct.getId());
        highlight.put("title", leadProduct.getTitle());
        highlight.put("rating", leadProduct.getRating() != null ? leadProduct.getRating() : 0.0);
        return highlight;
    }
}
