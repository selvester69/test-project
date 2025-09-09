package main.java.main.java.com.example.inventoryservice.repository;

import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import main.java.main.java.com.example.inventoryservice.model.Warehouse.WarehouseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    
    boolean existsByName(String name);
    
    Optional<Warehouse> findByName(String name);
    
    Page<Warehouse> findByLocationContainingIgnoreCase(String location, Pageable pageable);
    
    List<Warehouse> findByStatus(WarehouseStatus status);
    
    @Query("SELECT w FROM Warehouse w WHERE w.capacity > 0 AND w.status = 'ACTIVE' ORDER BY w.capacity DESC")
    List<Warehouse> findActiveWarehousesWithCapacity();
    
    @Query(value = "SELECT SUM(i.totalQuantity) FROM inventory_items i JOIN warehouses w ON i.warehouse_id = w.id WHERE w.id = ?1", nativeQuery = true)
    Integer getTotalStockInWarehouse(Long warehouseId);
}