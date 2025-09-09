package main.java.main.java.com.example.productservice.service;

import main.java.main.java.com.example.productservice.model.Product;
import main.java.main.java.com.example.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Cacheable(value = "searchResults", key = "#query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<Product> searchProducts(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return productRepository.findAll(pageable);
        }
        return productRepository.searchByQuery(query.toLowerCase(), pageable);
    }

    @Override
    @Cacheable(value = "advancedSearchResults", key = "#name + '_' + #description + '_' + #sku + '_' + #tags + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<Product> advancedSearch(String name, String description, String sku, String[] tags, Pageable pageable) {
        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), 
                    "%" + name.toLowerCase() + "%"
                ));
            }

            if (description != null && !description.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), 
                    "%" + description.toLowerCase() + "%"
                ));
            }

            if (sku != null && !sku.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("sku")), 
                    "%" + sku.toLowerCase() + "%"
                ));
            }

            if (tags != null && tags.length > 0) {
                for (String tag : tags) {
                    if (tag != null && !tag.trim().isEmpty()) {
                        predicates.add(criteriaBuilder.like(
                            root.get("tags"), 
                            "%" + tag.toLowerCase() + "%"
                        ));
                    }
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable);
    }
}