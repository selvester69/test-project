package main.java.main.java.com.example.inventoryservice.repository;

import main.java.main.java.com.example.inventoryservice.model.InventoryItem;
import main.java.main.java.com.example.inventoryservice.model.InventoryItem.InventoryStatus;
import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    
    boolean existsByProductIdAndWarehouseId(Long productId, Warehouse warehouse);
    
    Optional<InventoryItem> findByProductIdAndWarehouseId(Long productId, Warehouse warehouse);
    
    List<InventoryItem> findByProductId(Long productId);
    
    Page<InventoryItem> findByProductId(Long productId, Pageable pageable);
    
    List<InventoryItem> findByWarehouseId(Long warehouseId);
    
    Page<InventoryItem> findByWarehouseId(Long warehouseId, Pageable pageable);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.status = :status")
    Page<InventoryItem> findByStatus(@Param("status") InventoryStatus status, Pageable pageable);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.warehouse = :warehouse AND i.status = 'LOW_STOCK'")
    List<InventoryItem> findLowStockItemsInWarehouse(@Param("warehouse") Warehouse warehouse);
    
    @Query(value = "SELECT SUM(i.total_quantity) FROM inventory_items i WHERE i.product_id = ?1", nativeQuery = true)
    Integer getTotalStockForProduct(Long productId);
    
    @Query("SELECT SUM(i.totalQuantity) FROM InventoryItem i WHERE i.warehouse.id = :warehouseId")
    Integer getTotalStockInWarehouse(@Param("warehouseId") Long warehouseId);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.getAvailableQuantity() < :threshold")
    List<InventoryItem> findLowStockItems(@Param("threshold") Integer threshold);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.productId IN :productIds")
    List<InventoryItem> findByProductIds(@Param("productIds") List<Long> productIds);
    
    @Query(value = "SELECT i.* FROM inventory_items i JOIN warehouses w ON i.warehouse_id = w.id WHERE w.location = ?1", nativeQuery = true)
    List<InventoryItem> findByWarehouseLocation(String location);
}