package main.java.main.java.com.example.inventoryservice.service;

import main.java.main.java.com.example.inventoryservice.model.InventoryItem;
import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface InventoryService {
    
    InventoryItem createInventoryItem(InventoryItem inventoryItem);
    
    InventoryItem updateInventoryItem(Long id, InventoryItem inventoryItemDetails);
    
    Optional<InventoryItem> getInventoryItemById(Long id);
    
    Page<InventoryItem> getAllInventoryItems(Pageable pageable);
    
    InventoryItem updateStockQuantity(Long productId, Warehouse warehouse, Integer newQuantity);
    
    InventoryItem adjustReservedQuantity(Long productId, Warehouse warehouse, Integer quantityChange);
    
    Optional<InventoryItem> getInventoryByProductId(Long productId);
    
    Optional<InventoryItem> getInventoryByProductAndWarehouse(Long productId, Long warehouseId);
    
    List<InventoryItem> getInventoryByWarehouse(Long warehouseId);
    
    Page<InventoryItem> getInventoryByWarehouse(Long warehouseId, Pageable pageable);
    
    List<InventoryItem> getLowStockItems(Integer threshold);
    
    List<InventoryItem> getLowStockItemsInWarehouse(Warehouse warehouse);
    
    Integer getTotalStockForProduct(Long productId);
    
    Integer getTotalStockInWarehouse(Long warehouseId);
    
    boolean existsByProductIdAndWarehouseId(Long productId, Long warehouseId);
    
    void deleteInventoryItem(Long id);
    
    void softDeleteInventoryItem(Long id);
    
    InventoryItem initializeInventoryForProduct(Long productId, Long warehouseId, Integer initialQuantity);
}