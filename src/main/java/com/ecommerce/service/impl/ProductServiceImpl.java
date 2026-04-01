package com.ecommerce.service.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.model.Product;
import com.ecommerce.model.UserDtls;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private UserRepository userRepository;

	@Override
	public Product saveProduct(Product product) {
		prepareProductForSave(product, product.getImage());
		return productRepository.save(product);
	}

	@Override
	public Product saveSellerProduct(Product product, MultipartFile image, Integer sellerId) {
		UserDtls seller = userRepository.findById(sellerId).orElse(null);
		if (seller == null || product == null) {
			return null;
		}

		String imageName = image == null || image.isEmpty() ? resolveDefaultImage(product.getCategory())
				: StringUtils.cleanPath(image.getOriginalFilename());

		product.setSeller(seller);
		prepareProductForSave(product, imageName);

		Product savedProduct = productRepository.save(product);
		storeProductImage(image, imageName);
		return savedProduct;
	}

	@Override
	public List<Product> getAllProducts() {
		return productRepository.findAll();
	}

	@Override
	public Page<Product> getAllProductsPagination(Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return productRepository.findAll(pageable);
	}

	@Override
	public Boolean deleteProduct(Integer id) {
		Product product = productRepository.findById(id).orElse(null);

		if (!ObjectUtils.isEmpty(product)) {
			productRepository.delete(product);
			return true;
		}
		return false;
	}

	@Override
	public Product getProductById(Integer id) {
		Product product = productRepository.findById(id).orElse(null);
		return product;
	}

	@Override
	public Product getLiveProductById(Integer id) {
		return productRepository.findByIdAndIsActiveTrueAndSellerIsNotNull(id).orElse(null);
	}

	@Override
	public Product updateProduct(Product product, MultipartFile image) {

		Product dbProduct = getProductById(product.getId());

		String imageName = image == null || image.isEmpty() ? dbProduct.getImage() : StringUtils.cleanPath(image.getOriginalFilename());

		dbProduct.setTitle(product.getTitle());
		dbProduct.setDescription(product.getDescription());
		dbProduct.setCategory(product.getCategory());
		dbProduct.setPrice(product.getPrice());
		dbProduct.setStock(product.getStock());
		dbProduct.setDiscount(product.getDiscount());
		dbProduct.setColor(product.getColor());
		dbProduct.setSize(product.getSize());
		dbProduct.setCostPrice(product.getCostPrice());
		dbProduct.setImage(imageName);
		dbProduct.setIsActive(product.getIsActive());
		dbProduct.setIsReturnable(product.getIsReturnable());
		dbProduct.setReturnWindow(product.getReturnWindow());
		prepareProductForSave(dbProduct, imageName);

		Product updateProduct = productRepository.save(dbProduct);

		if (!ObjectUtils.isEmpty(updateProduct)) {
			storeProductImage(image, imageName);
			return product;
		}
		return null;
	}

	@Override
	public List<Product> getAllActiveProducts(String category) {
		List<Product> products = null;
		if (ObjectUtils.isEmpty(category)) {
			products = productRepository.findByIsActiveTrueAndSellerIsNotNullOrderByIdDesc();
		} else {
			products = productRepository.findByIsActiveTrueAndSellerIsNotNullAndCategoryIgnoreCaseOrderByIdDesc(category);
		}

		return products;
	}

	@Override
	public List<Product> searchProduct(String ch) {
		return productRepository
				.searchActiveProducts(null, ch, PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "id"))).getContent();
	}

	@Override
	public Page<Product> searchProductPagination(Integer pageNo, Integer pageSize, String ch) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		return productRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(ch, ch, pageable);
	}

	@Override
	public Page<Product> getAllActiveProductPagination(Integer pageNo, Integer pageSize, String category) {

		Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "id"));
		Page<Product> pageProduct = null;

		if (ObjectUtils.isEmpty(category)) {
			pageProduct = productRepository.findByIsActiveTrueAndSellerIsNotNull(pageable);
		} else {
			pageProduct = productRepository.findByIsActiveTrueAndSellerIsNotNullAndCategoryIgnoreCase(category, pageable);
		}
		return pageProduct;
	}

	@Override
	public Page<Product> searchActiveProductPagination(Integer pageNo, Integer pageSize, String category, String ch) {

		Page<Product> pageProduct = null;
		Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "id"));

		pageProduct = productRepository.searchActiveProducts(category, ch, pageable);
		return pageProduct;
	}

	@Override
	public Page<Product> getProductsBySeller(Integer sellerId, Integer pageNo, Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "id"));
		return productRepository.findBySellerId(sellerId, pageable);
	}

	private void prepareProductForSave(Product product, String imageName) {
		if (product == null) {
			return;
		}

		product.setTitle(StringUtils.hasText(product.getTitle()) ? product.getTitle().trim() : product.getTitle());
		product.setDescription(
				StringUtils.hasText(product.getDescription()) ? product.getDescription().trim() : product.getDescription());
		product.setCategory(
				StringUtils.hasText(product.getCategory()) ? product.getCategory().trim() : product.getCategory());
		product.setColor(StringUtils.hasText(product.getColor()) ? product.getColor().trim() : null);
		product.setSize(StringUtils.hasText(product.getSize()) ? product.getSize().trim() : null);
		product.setImage(StringUtils.hasText(imageName) ? imageName : resolveDefaultImage(product.getCategory()));
		product.setDiscount(normalizeDiscount(product.getDiscount()));
		product.setStock(Math.max(product.getStock(), 0));
		product.setPrice(product.getPrice() == null ? 0.0 : product.getPrice());
		product.setDiscountPrice(calculateDiscountPrice(product.getPrice(), product.getDiscount()));
		product.setCostPrice(product.getCostPrice() == null ? 0.0 : product.getCostPrice());
		product.setRating(product.getRating() == null ? 0.0 : product.getRating());
		if (product.getIsActive() == null) {
			product.setIsActive(true);
		}
		if (product.getIsReturnable() == null) {
			product.setIsReturnable(Boolean.TRUE);
		}
		if (product.getReturnWindow() == null || product.getReturnWindow() < 0) {
			product.setReturnWindow(7);
		}
	}

	private int normalizeDiscount(int discount) {
		if (discount < 0) {
			return 0;
		}
		return Math.min(discount, 90);
	}

	private Double calculateDiscountPrice(Double price, int discount) {
		double basePrice = price == null ? 0.0 : price;
		double discountedPrice = basePrice - (basePrice * (discount / 100.0));
		return Math.round(discountedPrice * 100.0) / 100.0;
	}

	private String resolveDefaultImage(String category) {
		if (!StringUtils.hasText(category)) {
			return "appli.png";
		}

		return switch (category.trim().toLowerCase()) {
		case "laptop" -> "laptop.jpg";
		case "clothes", "footwear" -> "pant.png";
		case "beauty" -> "beuty.png";
		case "grocery" -> "groccery.jpg";
		default -> "appli.png";
		};
	}

	private void storeProductImage(MultipartFile image, String imageName) {
		if (image == null || image.isEmpty() || !StringUtils.hasText(imageName)) {
			return;
		}

		try {
			File saveFile = new ClassPathResource("static/img").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "product_img" + File.separator + imageName);
			if (!Files.exists(path.getParent())) {
				Files.createDirectories(path.getParent());
			}
			Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
