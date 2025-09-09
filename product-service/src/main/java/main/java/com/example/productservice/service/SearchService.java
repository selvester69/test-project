package main.java.main.java.com.example.productservice.service;

import main.java.main.java.com.example.productservice.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchService {
    Page<Product> searchProducts(String query, Pageable pageable);
    Page<Product> advancedSearch(String name, String description, String sku, String[] tags, Pageable pageable);
}