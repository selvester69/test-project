package main.java.main.java.com.example.productservice.service;

import main.java.main.java.com.example.productservice.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface FilterService {
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
    
    Specification<Product> buildFilterSpecification(
        BigDecimal minPrice, BigDecimal maxPrice,
        Long categoryId, String categoryName,
        String status,
        Boolean inStock, Integer minStock, Integer maxStock,
        LocalDateTime createdAfter, LocalDateTime createdBefore,
        LocalDateTime updatedAfter, LocalDateTime updatedBefore,
        List<String> tags,
        String metadataField, String metadataValue
    );
}