package main.java.main.java.com.example.productservice.service;

import main.java.main.java.com.example.productservice.model.Product;
import main.java.main.java.com.example.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FilterServiceImpl implements FilterService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public Page<Product> filterProducts(
            BigDecimal minPrice, BigDecimal maxPrice,
            Long categoryId, String categoryName,
            String status,
            Boolean inStock, Integer minStock, Integer maxStock,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            LocalDateTime updatedAfter, LocalDateTime updatedBefore,
            List<String> tags,
            String metadataField, String metadataValue,
            Pageable pageable) {

        Specification<Product> specification = buildFilterSpecification(
                minPrice, maxPrice, categoryId, categoryName, status,
                inStock, minStock, maxStock, createdAfter, createdBefore,
                updatedAfter, updatedBefore, tags, metadataField, metadataValue
        );

        return productRepository.findAll(specification, pageable);
    }

    @Override
    public Specification<Product> buildFilterSpecification(
            BigDecimal minPrice, BigDecimal maxPrice,
            Long categoryId, String categoryName,
            String status,
            Boolean inStock, Integer minStock, Integer maxStock,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            LocalDateTime updatedAfter, LocalDateTime updatedBefore,
            List<String> tags,
            String metadataField, String metadataValue) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Price range filter
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // Category filter
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            if (categoryName != null && !categoryName.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("category").get("name")),
                    "%" + categoryName.toLowerCase() + "%"
                ));
            }

            // Status filter
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), Product.ProductStatus.valueOf(status.toUpperCase())));
            }

            // Stock filters
            if (inStock != null && inStock) {
                predicates.add(criteriaBuilder.greaterThan(root.get("stockQuantity"), 0));
            }
            if (minStock != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("stockQuantity"), minStock));
            }
            if (maxStock != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("stockQuantity"), maxStock));
            }

            // Date range filters
            if (createdAfter != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
            }
            if (createdBefore != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
            }
            if (updatedAfter != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), updatedAfter));
            }
            if (updatedBefore != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), updatedBefore));
            }

            // Tags filter (contains any)
            if (tags != null && !tags.isEmpty()) {
                List<Predicate> tagPredicates = new ArrayList<>();
                for (String tag : tags) {
                    if (tag != null && !tag.trim().isEmpty()) {
                        // For ElementCollection, we need to join with the collection
                        Join<Object, String> tagsJoin = root.join("tags");
                        tagPredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(tagsJoin),
                            "%" + tag.toLowerCase() + "%"
                        ));
                    }
                }
                if (!tagPredicates.isEmpty()) {
                    predicates.add(criteriaBuilder.or(tagPredicates.toArray(new Predicate[0])));
                }
            }

            // Metadata filter (JSONB contains key-value)
            if (metadataField != null && !metadataField.trim().isEmpty() && 
                metadataValue != null && !metadataValue.trim().isEmpty()) {
                // For PostgreSQL JSONB, use native query or function
                // This is a simplified approach - in production, use @Query with native SQL
                predicates.add(criteriaBuilder.isTrue(
                    criteriaBuilder.function("metadata", Boolean.class, root.get("metadata"),
                        criteriaBuilder.literal(metadataField), criteriaBuilder.literal(metadataValue))
                ));
            }

            // Combine all predicates with AND
            return predicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}