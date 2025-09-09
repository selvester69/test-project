package main.java.main.java.com.example.inventoryservice.controller;

import main.java.main.java.com.example.inventoryservice.model.InventoryItem;
import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import main.java.main.java.com.example.inventoryservice.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory Management", description = "CRUD operations for inventory management")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/items")
    @Operation(summary = "Create new inventory item", description = "Create a new inventory item for a product in a warehouse")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Inventory item created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or duplicate entry"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    public ResponseEntity<InventoryItem> createInventoryItem(@Valid @RequestBody InventoryItem inventoryItem) {
        InventoryItem createdItem = inventoryService.createInventoryItem(inventoryItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @GetMapping("/items/{id}")
    @Operation(summary = "Get inventory item by ID", description = "Retrieve a specific inventory item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory item found"),
        @ApiResponse(responseCode = "404", description = "Inventory item not found")
    })
    public ResponseEntity<InventoryItem> getInventoryItemById(
            @Parameter(description = "Inventory item ID", required = true) @PathVariable Long id) {
        Optional<InventoryItem> inventoryItem = inventoryService.getInventoryItemById(id);
        return inventoryItem.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/items")
    @Operation(summary = "Get all inventory items", description = "Retrieve paginated list of all inventory items")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory items retrieved successfully")
    })
    public ResponseEntity<Page<InventoryItem>> getAllInventoryItems(Pageable pageable) {
        Page<InventoryItem> inventoryItems = inventoryService.getAllInventoryItems(pageable);
        return ResponseEntity.ok(inventoryItems);
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "Update inventory item", description = "Update complete inventory item details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory item updated successfully"),
        @ApiResponse(responseCode = "404", description = "Inventory item not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<InventoryItem> updateInventoryItem(
            @Parameter(description = "Inventory item ID", required = true) @PathVariable Long id,
            @Valid @RequestBody InventoryItem inventoryItemDetails) {
        try {
            InventoryItem updatedItem = inventoryService.updateInventoryItem(id, inventoryItemDetails);
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/items/{id}")
    @Operation(summary = "Partial update inventory item", description = "Update specific fields of inventory item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory item partially updated"),
        @ApiResponse(responseCode = "404", description = "Inventory item not found")
    })
    public ResponseEntity<InventoryItem> partialUpdateInventoryItem(
            @Parameter(description = "Inventory item ID", required = true) @PathVariable Long id,
            @RequestBody InventoryItem inventoryItemDetails) {
        try {
            InventoryItem updatedItem = inventoryService.updateInventoryItem(id, inventoryItemDetails);
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Delete inventory item", description = "Soft delete an inventory item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Inventory item deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Inventory item not found")
    })
    public ResponseEntity<Void> deleteInventoryItem(
            @Parameter(description = "Inventory item ID", required = true) @PathVariable Long id) {
        try {
            inventoryService.softDeleteInventoryItem(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get inventory by product ID", description = "Retrieve inventory information for a specific product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory information found"),
        @ApiResponse(responseCode = "404", description = "No inventory found for product")
    })
    public ResponseEntity<InventoryItem> getInventoryByProductId(
            @Parameter(description = "Product ID", required = true) @PathVariable Long productId) {
        Optional<InventoryItem> inventoryItem = inventoryService.getInventoryByProductId(productId);
        return inventoryItem.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/product/{productId}/warehouse/{warehouseId}")
    @Operation(summary = "Get inventory by product and warehouse", description = "Retrieve specific inventory for product in warehouse")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory found"),
        @ApiResponse(responseCode = "404", description = "Inventory not found")
    })
    public ResponseEntity<InventoryItem> getInventoryByProductAndWarehouse(
            @Parameter(description = "Product ID", required = true) @PathVariable Long productId,
            @Parameter(description = "Warehouse ID", required = true) @PathVariable Long warehouseId) {
        Optional<Warehouse> warehouse = warehouseRepository.findById(warehouseId);
        if (warehouse.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<InventoryItem> inventoryItem = inventoryService.getInventoryByProductAndWarehouse(productId, warehouseId);
        return inventoryItem.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get all inventory in warehouse", description = "Retrieve all inventory items in a specific warehouse")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Warehouse inventory retrieved")
    })
    public ResponseEntity<List<InventoryItem>> getInventoryByWarehouse(
            @Parameter(description = "Warehouse ID", required = true) @PathVariable Long warehouseId) {
        List<InventoryItem> inventoryItems = inventoryService.getInventoryByWarehouse(warehouseId);
        return ResponseEntity.ok(inventoryItems);
    }

    @GetMapping("/warehouse/{warehouseId}/page")
    @Operation(summary = "Get paginated inventory in warehouse", description = "Retrieve paginated inventory items in a warehouse")
    public ResponseEntity<Page<InventoryItem>> getInventoryByWarehousePage(
            @Parameter(description = "Warehouse ID", required = true) @PathVariable Long warehouseId,
            Pageable pageable) {
        Page<InventoryItem> inventoryItems = inventoryService.getInventoryByWarehouse(warehouseId, pageable);
        return ResponseEntity.ok(inventoryItems);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock items", description = "Retrieve all items with stock below threshold (default: 10)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Low stock items retrieved")
    })
    public ResponseEntity<List<InventoryItem>> getLowStockItems(
            @RequestParam(defaultValue = "10") Integer threshold) {
        List<InventoryItem> lowStockItems = inventoryService.getLowStockItems(threshold);
        return ResponseEntity.ok(lowStockItems);
    }

    @GetMapping("/warehouse/{warehouseId}/low-stock")
    @Operation(summary = "Get low stock items in warehouse", description = "Retrieve low stock items in specific warehouse")
    public ResponseEntity<List<InventoryItem>> getLowStockInWarehouse(
            @Parameter(description = "Warehouse ID", required = true) @PathVariable Long warehouseId) {
        Optional<Warehouse> warehouse = warehouseRepository.findById(warehouseId);
        if (warehouse.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<InventoryItem> lowStockItems = inventoryService.getLowStockItemsInWarehouse(warehouse);
        return ResponseEntity.ok(lowStockItems);
    }

    @PatchMapping("/stock/{productId}/warehouse/{warehouseId}")
    @Operation(summary = "Update stock quantity", description = "Update stock quantity for product in specific warehouse")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
        @ApiResponse(responseCode = "404", description = "Product or warehouse not found")
    })
    public ResponseEntity<InventoryItem> updateStockQuantity(
            @Parameter(description = "Product ID", required = true) @PathVariable Long productId,
            @Parameter(description = "Warehouse ID", required = true) @PathVariable Long warehouseId,
            @RequestParam Integer newQuantity) {
        Optional<Warehouse> warehouse = warehouseRepository.findById(warehouseId);
        if (warehouse.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InventoryItem updatedItem = inventoryService.updateStockQuantity(productId, warehouse.get(), newQuantity);
        return ResponseEntity.ok(updatedItem);
    }

    @PatchMapping("/reserve/{productId}/warehouse/{warehouseId}")
    @Operation(summary = "Adjust reserved quantity", description = "Reserve or release stock quantity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reservation updated"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation amount"),
        @ApiResponse(responseCode = "404", description = "Inventory not found")
    })
    public ResponseEntity<InventoryItem> adjustReservedQuantity(
            @Parameter(description = "Product ID", required = true) @PathVariable Long productId,
            @Parameter(description = "Warehouse ID", required = true) @PathVariable Long warehouseId,
            @RequestParam Integer quantityChange) {
        Optional<Warehouse> warehouse = warehouseRepository.findById(warehouseId);
        if (warehouse.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            InventoryItem updatedItem = inventoryService.adjustReservedQuantity(productId, warehouse.get(), quantityChange);
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/product/{productId}/total-stock")
    @Operation(summary = "Get total stock for product", description = "Calculate total stock across all warehouses")
    public ResponseEntity<Map<String, Object>> getTotalStockForProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable Long productId) {
        Integer totalStock = inventoryService.getTotalStockForProduct(productId);
        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("totalStock", totalStock);
        response.put("availableStock", totalStock); // Simplified - can be enhanced with reservations
        return ResponseEntity.ok(response);
    }

    @GetMapping("/warehouse/{warehouseId}/total-stock")
    @Operation(summary = "Get total stock in warehouse", description = "Calculate total stock in specific warehouse")
    public ResponseEntity<Map<String, Object>> getTotalStockInWarehouse(
            @Parameter(description = "Warehouse ID", required = true) @PathVariable Long warehouseId) {
        Integer totalStock = inventoryService.getTotalStockInWarehouse(warehouseId);
        Map<String, Object> response = new HashMap<>();
        response.put("warehouseId", warehouseId);
        response.put("totalStock", totalStock);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initialize/{productId}/warehouse/{warehouseId}")
    @Operation(summary = "Initialize inventory for new product", description = "Create initial inventory entry for new product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Inventory initialized successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found")
    })
    public ResponseEntity<InventoryItem> initializeInventoryForProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable Long productId,
            @Parameter(description = "Warehouse ID", required = true) @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") Integer initialQuantity) {
        Optional<Warehouse> warehouse = warehouseRepository.findById(warehouseId);
        if (warehouse.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InventoryItem initializedItem = inventoryService.initializeInventoryForProduct(productId, warehouseId, initialQuantity);
        return ResponseEntity.status(HttpStatus.CREATED).body(initializedItem);
    }
}