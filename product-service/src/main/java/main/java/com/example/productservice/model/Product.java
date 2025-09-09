package main.java.main.java.com.example.productservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
@org.hibernate.annotations.Table(appliesTo = "products", indexes = {
    @org.hibernate.annotations.Index(name = "idx_product_name", columnNames = "name"),
    @org.hibernate.annotations.Index(name = "idx_product_sku", columnNames = "sku"),
    @org.hibernate.annotations.Index(name = "idx_product_category", columnNames = "category_id"),
    @org.hibernate.annotations.Index(name = "idx_product_status", columnNames = "status"),
    @org.hibernate.annotations.Index(name = "idx_product_price", columnNames = "price"),
    @org.hibernate.annotations.Index(name = "idx_product_created_at", columnNames = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotBlank(message = "SKU is required")
    @Size(min = 1, max = 100, message = "SKU must be between 1 and 100 characters")
    @Column(nullable = false, unique = true)
    private String sku;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(nullable = false)
    private Integer stockQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    private List<String> tags = new java.util.ArrayList<>();

    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new java.util.HashMap<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ProductStatus {
        ACTIVE, INACTIVE, DISCONTINUED
    }
}