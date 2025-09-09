package main.java.main.java.com.example.productservice.service;

import main.java.main.java.com.example.productservice.model.Product;
import main.java.main.java.com.example.productservice.model.Product.ProductStatus;
import main.java.main.java.com.example.productservice.repository.ProductRepository;
import main.java.main.java.com.example.productservice.repository.CategoryRepository;
import main.java.main.java.com.example.productservice.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SearchService searchService;

    @Autowired
    private FilterService filterService;

    @Override
    @io.micrometer.core.annotation.Timed(value = "product.create", description = "Time taken to create a product")
    @org.springframework.cache.annotation.CacheEvict(value = {"searchResults", "advancedSearchResults", "productCache"}, allEntries = true)
    public Product createProduct(@Valid Product product) {
        // Check SKU uniqueness
        if (productRepository.existsBySku(product.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + product.getSku());
        }

        // Validate category exists
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            if (!categoryRepository.existsById(product.getCategory().getId())) {
                throw new IllegalArgumentException("Category not found: " + product.getCategory().getId());
            }
        }

        // Set default status if not provided
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        // Initialize collections if null
        if (product.getTags() == null) {
            product.setTags(new java.util.ArrayList<>());
        }
        if (product.getMetadata() == null) {
            product.setMetadata(new java.util.HashMap<>());
        }

        // Set timestamps
        if (product.getCreatedAt() == null) {
            product.setCreatedAt(java.time.LocalDateTime.now());
        }
        if (product.getUpdatedAt() == null) {
            product.setUpdatedAt(java.time.LocalDateTime.now());
        }

        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Long id, @Valid Product productDetails) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        // Check SKU uniqueness (excluding current product)
        if (!existingProduct.getSku().equals(productDetails.getSku()) &&
                productRepository.existsBySku(productDetails.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + productDetails.getSku());
        }

        // Validate category exists
        if (productDetails.getCategory() != null && productDetails.getCategory().getId() != null) {
            if (!categoryRepository.existsById(productDetails.getCategory().getId())) {
                throw new IllegalArgumentException("Category not found: " + productDetails.getCategory().getId());
            }
        }

        // Update all fields
        existingProduct.setName(productDetails.getName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setPrice(productDetails.getPrice());
        existingProduct.setCategory(productDetails.getCategory());
        existingProduct.setSku(productDetails.getSku());
        existingProduct.setStockQuantity(productDetails.getStockQuantity());
        existingProduct.setStatus(
                productDetails.getStatus() != null ? productDetails.getStatus() : existingProduct.getStatus());
        existingProduct
                .setTags(productDetails.getTags() != null ? productDetails.getTags() : existingProduct.getTags());
        existingProduct.setMetadata(
                productDetails.getMetadata() != null ? productDetails.getMetadata() : existingProduct.getMetadata());
        existingProduct.setUpdatedAt(java.time.LocalDateTime.now());

        return productRepository.save(existingProduct);
    }

    @Override
    public Product partialUpdateProduct(Long id, Product productDetails) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        // Check SKU uniqueness if provided and changed
        if (productDetails.getSku() != null && !existingProduct.getSku().equals(productDetails.getSku()) &&
                productRepository.existsBySku(productDetails.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + productDetails.getSku());
        }

        // Validate category if provided
        if (productDetails.getCategory() != null && productDetails.getCategory().getId() != null) {
            if (!categoryRepository.existsById(productDetails.getCategory().getId())) {
                throw new IllegalArgumentException("Category not found: " + productDetails.getCategory().getId());
            }
        }

        // Update only provided fields
        if (productDetails.getName() != null) {
            existingProduct.setName(productDetails.getName());
        }
        if (productDetails.getDescription() != null) {
            existingProduct.setDescription(productDetails.getDescription());
        }
        if (productDetails.getPrice() != null) {
            existingProduct.setPrice(productDetails.getPrice());
        }
        if (productDetails.getCategory() != null) {
            existingProduct.setCategory(productDetails.getCategory());
        }
        if (productDetails.getSku() != null) {
            existingProduct.setSku(productDetails.getSku());
        }
        if (productDetails.getStockQuantity() != null) {
            existingProduct.setStockQuantity(productDetails.getStockQuantity());
        }
        if (productDetails.getStatus() != null) {
            existingProduct.setStatus(productDetails.getStatus());
        }
        if (productDetails.getTags() != null) {
            existingProduct.setTags(productDetails.getTags());
        }
        if (productDetails.getMetadata() != null) {
            existingProduct.setMetadata(productDetails.getMetadata());
        }
        existingProduct.setUpdatedAt(java.time.LocalDateTime.now());

        return productRepository.save(existingProduct);
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public Optional<Product> getProductBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    @Override
    @io.micrometer.core.annotation.Timed(value = "product.delete", description = "Time taken to delete a product")
    @org.springframework.cache.annotation.CacheEvict(value = {"searchResults", "advancedSearchResults", "productCache"}, key = "'product_' + #id")
    public void deleteProduct(Long id) {
        Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        
        // Soft delete by setting status to DISCONTINUED
        existingProduct.setStatus(ProductStatus.DISCONTINUED);
        existingProduct.setUpdatedAt(java.time.LocalDateTime.now());
        productRepository.save(existingProduct);
    }

    @Override
    public boolean existsBySku(String sku) {
        return productRepository.existsBySku(sku);
    }

    @Override
    public Page<Product> searchProducts(String query, Pageable pageable) {
        return searchService.searchProducts(query, pageable);
    }

    @Override
    public Page<Product> advancedSearch(String name, String description, String sku, String[] tags, Pageable pageable) {
        return searchService.advancedSearch(name, description, sku, tags, pageable);
    }
}