package main.java.main.java.com.example.inventoryservice.service;

import main.java.main.java.com.example.inventoryservice.model.InventoryItem;
import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchService {
    
    Page<InventoryItem> searchInventoryByWarehouse(Long warehouseId, String status, Integer minQuantity, Integer maxQuantity, Pageable pageable);
    
    Page<InventoryItem> searchInventoryByProduct(Long productId, String status, Pageable pageable);
    
    List<InventoryItem> findLowStockItems(Integer threshold, Long warehouseId, String severity);
    
    Page<InventoryItem> advancedSearch(String warehouseLocation, String status, Integer minAvailable, Integer maxAvailable, Pageable pageable);
    
    List<InventoryItem> searchByWarehouseLocation(String location);
    
    Long countLowStockItems(Integer threshold);
    
    Page<InventoryItem> searchOutOfStockItems(Pageable pageable);
    
    List<InventoryItem> findCriticalStockItems(Long warehouseId);
}