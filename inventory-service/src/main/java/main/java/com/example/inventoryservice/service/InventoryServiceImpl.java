package main.java.main.java.com.example.inventoryservice.service;

import main.java.main.java.com.example.inventoryservice.model.InventoryItem;
import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import main.java.main.java.com.example.inventoryservice.model.InventoryItem.InventoryStatus;
import main.java.main.java.com.example.inventoryservice.repository.InventoryItemRepository;
import main.java.main.java.com.example.inventoryservice.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Override
    @io.micrometer.core.annotation.Timed(value = "inventory.create", description = "Time taken to create inventory item")
    @org.springframework.cache.annotation.CacheEvict(value = "stockCache", allEntries = true)
    public InventoryItem createInventoryItem(@Valid InventoryItem inventoryItem) {
        // Validate warehouse exists
        if (inventoryItem.getWarehouse() == null || inventoryItem.getWarehouse().getId() == null) {
            throw new IllegalArgumentException("Warehouse is required");
        }
        Optional<Warehouse> warehouseOpt = warehouseRepository.findById(inventoryItem.getWarehouse().getId());
        if (warehouseOpt.isEmpty()) {
            throw new IllegalArgumentException("Warehouse not found: " + inventoryItem.getWarehouse().getId());
        }
        inventoryItem.setWarehouse(warehouseOpt.get());

        // Check if inventory item already exists for this product and warehouse
        if (inventoryItemRepository.existsByProductIdAndWarehouseId(inventoryItem.getProductId(), inventoryItem.getWarehouse())) {
            throw new IllegalArgumentException("Inventory item already exists for product " + inventoryItem.getProductId() + 
                " in warehouse " + inventoryItem.getWarehouse().getId());
        }

        // Validate productId is provided
        if (inventoryItem.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }

        // Set default values if not provided
        if (inventoryItem.getTotalQuantity() == null) {
            inventoryItem.setTotalQuantity(0);
        }
        if (inventoryItem.getReservedQuantity() == null) {
            inventoryItem.setReservedQuantity(0);
        }

        // Update status based on available quantity
        inventoryItem.updateStatus();

        return inventoryItemRepository.save(inventoryItem);
    }

    @Override
    @io.micrometer.core.annotation.Timed(value = "inventory.update", description = "Time taken to update inventory item")
    @org.springframework.cache.annotation.CacheEvict(value = {"stockCache", "filterResults"}, key = "#id")
    public InventoryItem updateInventoryItem(Long id, @Valid InventoryItem inventoryItemDetails) {
        InventoryItem existingItem = inventoryItemRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Inventory item not found: " + id));

        // Update fields if provided
        if (inventoryItemDetails.getProductId() != null) {
            existingItem.setProductId(inventoryItemDetails.getProductId());
        }
        if (inventoryItemDetails.getWarehouse() != null && inventoryItemDetails.getWarehouse().getId() != null) {
            Optional<Warehouse> warehouseOpt = warehouseRepository.findById(inventoryItemDetails.getWarehouse().getId());
            if (warehouseOpt.isEmpty()) {
                throw new IllegalArgumentException("Warehouse not found: " + inventoryItemDetails.getWarehouse().getId());
            }
            existingItem.setWarehouse(warehouseOpt.get());
        }
        if (inventoryItemDetails.getTotalQuantity() != null) {
            existingItem.setTotalQuantity(inventoryItemDetails.getTotalQuantity());
        }
        if (inventoryItemDetails.getReservedQuantity() != null) {
            existingItem.setReservedQuantity(inventoryItemDetails.getReservedQuantity());
        }
        if (inventoryItemDetails.getMetadata() != null) {
            existingItem.setMetadata(inventoryItemDetails.getMetadata());
        }

        // Update status
        existingItem.updateStatus();

        return inventoryItemRepository.save(existingItem);
    }

    @Override
    @Cacheable(value = "stockCache", key = "#id")
    public Optional<InventoryItem> getInventoryItemById(Long id) {
        return inventoryItemRepository.findById(id);
    }

    @Override
    public Page<InventoryItem> getAllInventoryItems(Pageable pageable) {
        return inventoryItemRepository.findAll(pageable);
    }

    @Override
    @Cacheable(value = "stockCache", key = "#productId + '_' + #warehouse.id")
    public Optional<InventoryItem> getInventoryByProductAndWarehouse(Long productId, Long warehouseId) {
        Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
        if (warehouseOpt.isEmpty()) {
            return Optional.empty();
        }
        return inventoryItemRepository.findByProductIdAndWarehouseId(productId, warehouseOpt.get());
    }

    @Override
    @Transactional
    @CacheEvict(value = "stockCache", key = "#productId + '_' + #warehouse.id")
    public InventoryItem updateStockQuantity(Long productId, Warehouse warehouse, Integer newQuantity) {
        Optional<InventoryItem> inventoryOpt = inventoryItemRepository.findByProductIdAndWarehouseId(productId, warehouse);
        InventoryItem inventoryItem;

        if (inventoryOpt.isPresent()) {
            inventoryItem = inventoryOpt.get();
            inventoryItem.setTotalQuantity(newQuantity);
        } else {
            // Create new inventory item if doesn't exist
            inventoryItem = new InventoryItem();
            inventoryItem.setProductId(productId);
            inventoryItem.setWarehouse(warehouse);
            inventoryItem.setTotalQuantity(newQuantity);
            inventoryItem.setReservedQuantity(0);
        }

        // Update status
        inventoryItem.updateStatus();

        inventoryItem = inventoryItemRepository.save(inventoryItem);
        
        // Evict related caches
        evictProductStockCache(productId);
        evictWarehouseStockCache(warehouse.getId());

        return inventoryItem;
    }

    @Override
    @Transactional
    @CacheEvict(value = "stockCache", key = "#productId + '_' + #warehouse.id")
    public InventoryItem adjustReservedQuantity(Long productId, Warehouse warehouse, Integer quantityChange) {
        Optional<InventoryItem> inventoryOpt = inventoryItemRepository.findByProductIdAndWarehouseId(productId, warehouse);
        if (inventoryOpt.isEmpty()) {
            throw new NoSuchElementException("Inventory item not found for product " + productId + " in warehouse " + warehouse.getId());
        }

        InventoryItem inventoryItem = inventoryOpt.get();
        int newReserved = inventoryItem.getReservedQuantity() + quantityChange;
        
        // Validate reservation doesn't exceed available stock
        int available = inventoryItem.getAvailableQuantity();
        if (newReserved > inventoryItem.getTotalQuantity()) {
            throw new IllegalArgumentException("Cannot reserve more than available stock. Available: " + available);
        }
        if (newReserved < 0) {
            throw new IllegalArgumentException("Reserved quantity cannot be negative");
        }

        inventoryItem.setReservedQuantity(newReserved);
        inventoryItem.updateStatus();

        inventoryItem = inventoryItemRepository.save(inventoryItem);
        
        // Evict related caches
        evictProductStockCache(productId);
        evictWarehouseStockCache(warehouse.getId());

        return inventoryItem;
    }

    @Override
    @Cacheable(value = "stockCache", key = "#productId")
    public Optional<InventoryItem> getInventoryByProductId(Long productId) {
        List<InventoryItem> items = inventoryItemRepository.findByProductId(productId);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0)); // Return first or primary location
    }

    @Override
    @Cacheable(value = "stockCache", key = "#warehouseId")
    public List<InventoryItem> getInventoryByWarehouse(Long warehouseId) {
        return inventoryItemRepository.findByWarehouseId(warehouseId);
    }

    @Override
    public Page<InventoryItem> getInventoryByWarehouse(Long warehouseId, Pageable pageable) {
        return inventoryItemRepository.findByWarehouseId(warehouseId, pageable);
    }

    @Override
    @Cacheable(value = "stockCache", key = "'low_stock_' + #threshold")
    public List<InventoryItem> getLowStockItems(Integer threshold) {
        return inventoryItemRepository.findLowStockItems(threshold);
    }

    @Override
    @Cacheable(value = "stockCache", key = "'low_stock_warehouse_' + #warehouse.id")
    public List<InventoryItem> getLowStockItemsInWarehouse(Warehouse warehouse) {
        return inventoryItemRepository.findLowStockItemsInWarehouse(warehouse);
    }

    @Override
    @Cacheable(value = "stockCache", key = "#productId + '_total'")
    public Integer getTotalStockForProduct(Long productId) {
        Integer total = inventoryItemRepository.getTotalStockForProduct(productId);
        return total != null ? total : 0;
    }

    @Override
    @Cacheable(value = "stockCache", key = "#warehouseId + '_total'")
    public Integer getTotalStockInWarehouse(Long warehouseId) {
        Integer total = inventoryItemRepository.getTotalStockInWarehouse(warehouseId);
        return total != null ? total : 0;
    }

    @Override
    public boolean existsByProductIdAndWarehouseId(Long productId, Long warehouseId) {
        Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
        if (warehouseOpt.isEmpty()) {
            return false;
        }
        return inventoryItemRepository.existsByProductIdAndWarehouseId(productId, warehouseOpt.get());
    }

    @Override
    @CacheEvict(value = "stockCache", key = "#id")
    public void deleteInventoryItem(Long id) {
        if (!inventoryItemRepository.existsById(id)) {
            throw new NoSuchElementException("Inventory item not found: " + id);
        }
        inventoryItemRepository.deleteById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "stockCache", key = "#id")
    public void softDeleteInventoryItem(Long id) {
        InventoryItem item = inventoryItemRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Inventory item not found: " + id));
        item.setStatus(InventoryStatus.OUT_OF_STOCK);
        inventoryItemRepository.save(item);
    }

    @Override
    @Transactional
    public InventoryItem initializeInventoryForProduct(Long productId, Long warehouseId, Integer initialQuantity) {
        Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
        if (warehouseOpt.isEmpty()) {
            throw new IllegalArgumentException("Warehouse not found: " + warehouseId);
        }

        InventoryItem newItem = new InventoryItem();
        newItem.setProductId(productId);
        newItem.setWarehouse(warehouseOpt.get());
        newItem.setTotalQuantity(initialQuantity);
        newItem.setReservedQuantity(0);
        newItem.updateStatus();

        InventoryItem savedItem = inventoryItemRepository.save(newItem);
        
        // Evict caches
        evictProductStockCache(productId);
        evictWarehouseStockCache(warehouseId);

        return savedItem;
    }

    private void evictProductStockCache(Long productId) {
        // Implementation for cache eviction can be expanded based on caching strategy
    }

    private void evictWarehouseStockCache(Long warehouseId) {
        // Implementation for cache eviction can be expanded based on caching strategy
    }
}