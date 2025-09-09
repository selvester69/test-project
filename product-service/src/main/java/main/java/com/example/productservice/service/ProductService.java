package main.java.main.java.com.example.productservice.service;

import main.java.main.java.com.example.productservice.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ProductService {
    Product createProduct(Product product);

    Product updateProduct(Long id, Product productDetails);

    Product partialUpdateProduct(Long id, Product productDetails);

    Optional<Product> getProductById(Long id);

    Page<Product> getAllProducts(Pageable pageable);

    Optional<Product> getProductBySku(String sku);

    void deleteProduct(Long id);

    boolean existsBySku(String sku);

    // Search methods
    Page<Product> searchProducts(String query, Pageable pageable);

    Page<Product> advancedSearch(String name, String description, String sku, String[] tags, Pageable pageable);
    
    // Filter methods
    Page<Product> filterProducts(
        BigDecimal minPrice, BigDecimal maxPrice,
        Long categoryId, String categoryName,
        String status,
        Boolean inStock, Integer minStock, Integer maxStock,
        LocalDateTime createdAfter, LocalDateTime createdBefore,
        LocalDateTime updatedAfter, LocalDateTime updatedBefore,
        List<String> tags,
        String metadataField, String metadataValue,
        Pageable pageable
    );
}