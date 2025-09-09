package main.java.main.java.com.example.productservice.controller;

import main.java.main.java.com.example.productservice.model.Product;
import main.java.main.java.com.example.productservice.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;
import java.util.Arrays;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> product = productService.getProductById(id);
        if (product.isPresent()) {
            return ResponseEntity.ok(product.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort_by,
            @RequestParam(required = false) String sort_order) {
        
        // Custom pagination and sorting
        int currentPage = (page != null && page > 0) ? page - 1 : 0; // Convert to 0-based
        int pageSize = (limit != null) ? Math.min(limit, 100) : 20; // Max 100
        
        org.springframework.data.domain.Sort.Direction direction =
            (sort_order != null && "asc".equalsIgnoreCase(sort_order)) ?
            org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
        
        org.springframework.data.domain.Sort sort = null;
        if (sort_by != null && !sort_by.trim().isEmpty()) {
            // Validate sortable fields
            String[] allowedSortFields = {"id", "name", "price", "stockQuantity", "createdAt", "updatedAt"};
            boolean isValidField = false;
            for (String field : allowedSortFields) {
                if (field.equals(sort_by)) {
                    isValidField = true;
                    break;
                }
            }
            if (isValidField) {
                sort = org.springframework.data.domain.Sort.by(direction, sort_by);
            }
        } else {
            sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        }
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(currentPage, pageSize, sort);
        
        Page<Product> products = productService.getAllProducts(pageable);
        
        // Enhanced response format
        Map<String, Object> response = new HashMap<>();
        response.put("data", products.getContent());
        response.put("total_count", products.getTotalElements());
        response.put("total_pages", products.getTotalPages());
        response.put("current_page", page != null ? page : 1);
        response.put("has_next", products.hasNext());
        response.put("has_previous", products.hasPrevious());
        response.put("page_size", pageSize);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable String sku) {
        Optional<Product> product = productService.getProductBySku(sku);
        if (product.isPresent()) {
            return ResponseEntity.ok(product.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Search endpoints
    @GetMapping("/search")
    public ResponseEntity<Page<Product>> searchProducts(
            @RequestParam(required = false) String q,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<Product> products;
        if (q != null && !q.trim().isEmpty()) {
            products = productService.searchProducts(q, pageable);
        } else {
            products = productService.getAllProducts(pageable);
        }
        return ResponseEntity.ok(products);
    }

    @GetMapping("/advanced-search")
    public ResponseEntity<Page<Product>> advancedSearch(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String tags,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        String[] tagArray = tags != null && !tags.trim().isEmpty() ? tags.split(",") : new String[0];

        Page<Product> products = productService.advancedSearch(name, description, sku, tagArray, pageable);
        return ResponseEntity.ok(products);
    }

    // Filter endpoint
    @GetMapping("/filter")
    public ResponseEntity<Page<Product>> filterProducts(
            @RequestParam(required = false) String min_price,
            @RequestParam(required = false) String max_price,
            @RequestParam(required = false) Long category_id,
            @RequestParam(required = false) String category_name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean in_stock,
            @RequestParam(required = false) Integer min_stock,
            @RequestParam(required = false) Integer max_stock,
            @RequestParam(required = false) String created_after,
            @RequestParam(required = false) String created_before,
            @RequestParam(required = false) String updated_after,
            @RequestParam(required = false) String updated_before,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String metadata_field,
            @RequestParam(required = false) String metadata_value,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        // Parse BigDecimal parameters
        java.math.BigDecimal minPrice = min_price != null ? new java.math.BigDecimal(min_price) : null;
        java.math.BigDecimal maxPrice = max_price != null ? new java.math.BigDecimal(max_price) : null;

        // Parse date parameters (ISO format)
        java.time.LocalDateTime createdAfter = created_after != null ? java.time.LocalDateTime.parse(created_after)
                : null;
        java.time.LocalDateTime createdBefore = created_before != null ? java.time.LocalDateTime.parse(created_before)
                : null;
        java.time.LocalDateTime updatedAfter = updated_after != null ? java.time.LocalDateTime.parse(updated_after)
                : null;
        java.time.LocalDateTime updatedBefore = updated_before != null ? java.time.LocalDateTime.parse(updated_before)
                : null;

        // Parse tags list
        java.util.List<String> tagList = tags != null && !tags.trim().isEmpty()
                ? java.util.Arrays.asList(tags.split(","))
                : new java.util.ArrayList<>();

        Page<Product> products = productService.filterProducts(
                minPrice, maxPrice, category_id, category_name, status,
                in_stock, min_stock, max_stock, createdAfter, createdBefore,
                updatedAfter, updatedBefore, tagList, metadata_field, metadata_value, pageable);

        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody Product productDetails) {
        try {
            Product updatedProduct = productService.updateProduct(id, productDetails);
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Product> partialUpdateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        try {
            Product updatedProduct = productService.partialUpdateProduct(id, productDetails);
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}