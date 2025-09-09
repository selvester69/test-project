package main.java.main.java.com.example.inventoryservice.service;

import main.java.main.java.com.example.inventoryservice.model.InventoryItem;
import main.java.main.java.com.example.inventoryservice.model.InventoryItem.InventoryStatus;
import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import main.java.main.java.com.example.inventoryservice.repository.InventoryItemRepository;
import main.java.main.java.com.example.inventoryservice.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Override
    @io.micrometer.core.annotation.Timed(value = "inventory.search.warehouse", description = "Time taken to search inventory by warehouse")
    @Cacheable(value = "filterResults", key = "#warehouseId + '_' + #status + '_' + #minQuantity + '_' + #maxQuantity + '_' + #pageable")
    public Page<InventoryItem> searchInventoryByWarehouse(Long warehouseId, String status, Integer minQuantity, Integer maxQuantity, Pageable pageable) {
        Specification<InventoryItem> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Warehouse filter
            predicates.add(criteriaBuilder.equal(root.get("warehouse").get("id"), warehouseId));
            
            // Status filter
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), InventoryStatus.valueOf(status)));
            }
            
            // Quantity filters
            if (minQuantity != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalQuantity"), minQuantity));
            }
            if (maxQuantity != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalQuantity"), maxQuantity));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return inventoryItemRepository.findAll(spec, pageable);
    }

    @Override
    @io.micrometer.core.annotation.Timed(value = "inventory.search.product", description = "Time taken to search inventory by product")
    @Cacheable(value = "filterResults", key = "#productId + '_' + #status + '_' + #pageable")
    public Page<InventoryItem> searchInventoryByProduct(Long productId, String status, Pageable pageable) {
        Specification<InventoryItem> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(criteriaBuilder.equal(root.get("productId"), productId));
            
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), InventoryStatus.valueOf(status)));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return inventoryItemRepository.findAll(spec, pageable);
    }

    @Override
    @io.micrometer.core.annotation.Timed(value = "inventory.lowstock", description = "Time taken to find low stock items")
    @Cacheable(value = "filterResults", key = "'low_stock_' + #threshold + '_' + #warehouseId + '_' + #severity")
    public List<InventoryItem> findLowStockItems(Integer threshold, Long warehouseId, String severity) {
        threshold = (threshold != null && threshold > 0) ? threshold : 10;
        
        List<InventoryItem> lowStockItems = inventoryItemRepository.findLowStockItems(threshold);
        
        if (warehouseId != null) {
            lowStockItems.removeIf(item -> !item.getWarehouse().getId().equals(warehouseId));
        }
        
        if (severity != null) {
            lowStockItems.removeIf(item -> {
                if ("CRITICAL".equals(severity) && item.getAvailableQuantity() > 5) {
                    return true;
                }
                if ("WARNING".equals(severity) && item.getAvailableQuantity() > 0 && item.getAvailableQuantity() <= 5) {
                    return true;
                }
                return false;
            });
        }
        
        return lowStockItems;
    }

    @Override
    @io.micrometer.core.annotation.Timed(value = "inventory.advanced.search", description = "Time taken for advanced inventory search")
    @Cacheable(value = "filterResults", key = "#warehouseLocation + '_' + #status + '_' + #minAvailable + '_' + #maxAvailable + '_' + #pageable")
    public Page<InventoryItem> advancedSearch(String warehouseLocation, String status, Integer minAvailable, Integer maxAvailable, Pageable pageable) {
        Specification<InventoryItem> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Warehouse location filter
            if (warehouseLocation != null && !warehouseLocation.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("warehouse").get("location")), 
                    "%" + warehouseLocation.toLowerCase() + "%"
                ));
            }
            
            // Status filter
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), InventoryStatus.valueOf(status)));
            }
            
            // Available quantity filters (calculated field)
            if (minAvailable != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    criteriaBuilder.diff(root.get("totalQuantity"), root.get("reservedQuantity")), 
                    minAvailable
                ));
            }
            if (maxAvailable != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    criteriaBuilder.diff(root.get("totalQuantity"), root.get("reservedQuantity")), 
                    maxAvailable
                ));
            }
            
            // Order by available quantity for better UX
            query.orderBy(criteriaBuilder.desc(criteriaBuilder.diff(root.get("totalQuantity"), root.get("reservedQuantity"))));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return inventoryItemRepository.findAll(spec, pageable);
    }

    @Override
    @io.micrometer.core.annotation.Timed(value = "inventory.search.location", description = "Time taken to search by warehouse location")
    @Cacheable(value = "filterResults", key = "'warehouse_location_' + #location")
    public List<InventoryItem> searchByWarehouseLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return inventoryItemRepository.findByWarehouseLocation(location.trim());
    }

    @Override
    @io.micrometer.core.annotation.Timed(value = "inventory.count.lowstock", description = "Time taken to count low stock items")
    @Cacheable(value = "filterResults", key = "'low_stock_count_' + #threshold")
    public Long countLowStockItems(Integer threshold) {
        threshold = (threshold != null && threshold > 0) ? threshold : 10;
        List<InventoryItem> lowStockItems = inventoryItemRepository.findLowStockItems(threshold);
        return (long) lowStockItems.size();
    }

    @Override
    @io.micrometer.core.annotation.Timed(value = "inventory.search.outofstock", description = "Time taken to search out of stock items")
    @Cacheable(value = "filterResults", key = "'out_of_stock_' + #pageable")
    public Page<InventoryItem> searchOutOfStockItems(Pageable pageable) {
        return inventoryItemRepository.findByStatus(InventoryStatus.OUT_OF_STOCK, pageable);
    }

    @Override
    @io.micrometer.core.annotation.Timed(value = "inventory.critical.stock", description = "Time taken to find critical stock items")
    @Cacheable(value = "filterResults", key = "'critical_stock_' + #warehouseId")
    public List<InventoryItem> findCriticalStockItems(Long warehouseId) {
        Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
        if (warehouseOpt.isEmpty()) {
            return new ArrayList<>();
        }
        
        return inventoryItemRepository.findLowStockItemsInWarehouse(warehouseOpt.get()).stream()
            .filter(item -> item.getAvailableQuantity() <= 5)
            .toList();
    }

    // Cache eviction methods to be called from other services
    @CacheEvict(value = "filterResults", allEntries = true)
    public void evictAllSearchCaches() {
        // Called when inventory data changes significantly
    }

    @CacheEvict(value = "filterResults", key = "#productId + '_search'")
    public void evictProductSearchCache(Long productId) {
        // Called when product inventory changes
    }

    @CacheEvict(value = "filterResults", key = "#warehouseId + '_search'")
    public void evictWarehouseSearchCache(Long warehouseId) {
        // Called when warehouse inventory changes
    }
}