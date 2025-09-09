package main.java.main.java.com.example.productservice.controller;

import main.java.main.java.com.example.productservice.model.Product;
import main.java.main.java.com.example.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Product Management", description = "API for managing products with CRUD, search, filter, and pagination capabilities")
@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    @Autowired
    private ProductService productService;

    @Operation(
        summary = "Create a new product",
        description = "Create a new product with all required fields. SKU must be unique and category must exist."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input, SKU already exists, or category not found",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @Operation(
        summary = "Get product by ID",
        description = "Retrieve a single product by its unique identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
        @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {
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

    @Operation(
        summary = "Search products",
        description = "Search products across name, description, SKU, and tags with case-insensitive partial matching. Returns paginated results."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<Product>> searchProducts(
            @Parameter(description = "Search query term", example = "laptop") @RequestParam(required = false) String q,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<Product> products;
        if (q != null && !q.trim().isEmpty()) {
            products = productService.searchProducts(q, pageable);
        } else {
            products = productService.getAllProducts(pageable);
        }
        return ResponseEntity.ok(products);
    }

    @Operation(
        summary = "Advanced search products",
        description = "Perform advanced search with specific field matching for name, description, SKU, and multiple tags."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Advanced search results retrieved",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/advanced-search")
    public ResponseEntity<Page<Product>> advancedSearch(
            @Parameter(description = "Product name to search", example = "MacBook") @RequestParam(required = false) String name,
            @Parameter(description = "Product description to search", example = "Pro") @RequestParam(required = false) String description,
            @Parameter(description = "Product SKU to search", example = "MBP2023") @RequestParam(required = false) String sku,
            @Parameter(description = "Comma-separated tags to search", example = "laptop,apple") @RequestParam(required = false) String tags,
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

    @Operation(
        summary = "Update product (full update)",
        description = "Replace the entire product with new data. All fields are required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Product.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or SKU conflict",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
        @Parameter(description = "Product ID", example = "1") @PathVariable Long id,
        @Valid @RequestBody Product productDetails) {
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