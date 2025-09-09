package main.java.main.java.com.example.productservice.repository;

import main.java.main.java.com.example.productservice.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    
    Optional<Product> findBySku(String sku);
    
    boolean existsBySku(String sku);
    
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE'")
    Page<Product> findAllActive(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.status != 'DISCONTINUED'")
    Page<Product> findAllAvailable(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "EXISTS (SELECT 1 FROM p.tags t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY " +
           "CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) THEN 1 ELSE 2 END, " +
           "CASE WHEN LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) THEN 1 ELSE 2 END")
    Page<Product> searchByQuery(@Param("query") String query, Pageable pageable);
}