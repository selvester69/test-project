package main.java.main.java.com.example.inventoryservice.service;

import main.java.main.java.com.example.inventoryservice.model.InventoryItem;
import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import main.java.main.java.com.example.inventoryservice.repository.InventoryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaginationService {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private SearchService searchService;

    /**
     * Get paginated stock reports with advanced sorting options
     */
    @io.micrometer.core.annotation.Timed(value = "inventory.pagination.stockreport", description = "Time taken to generate paginated stock report")
    @Cacheable(value = "stockReports", key = "#warehouseId + '_' + #sortBy + '_' + #sortOrder + '_' + #page + '_' + #size")
    public Page<InventoryItem> getPaginatedStockReport(Long warehouseId, String sortBy, String sortOrder, 
                                                      int page, int size, String statusFilter) {
        Sort sort = createSort(sortBy, sortOrder);
        Pageable pageable = PageRequest.of(page, size, sort);

        if (warehouseId != null) {
            if (statusFilter != null && !statusFilter.isEmpty()) {
                return searchService.searchInventoryByWarehouse(warehouseId, statusFilter, null, null, pageable);
            }
            return inventoryItemRepository.findByWarehouseId(warehouseId, pageable);
        }

        // Global stock report
        if (statusFilter != null && !statusFilter.isEmpty()) {
            return inventoryItemRepository.findByStatus(InventoryStatus.valueOf(statusFilter), pageable);
        }

        return inventoryItemRepository.findAll(pageable);
    }

    /**
     * Cursor-based pagination for large datasets (more efficient for very large tables)
     */
    @io.micrometer.core.annotation.Timed(value = "inventory.pagination.cursor", description = "Time taken for cursor-based pagination")
    @Cacheable(value = "cursorPagination", key = "#table + '_' + #cursor + '_' + #direction + '_' + #limit")
    public List<InventoryItem> getCursorPaginatedResults(String table, String cursor, String direction, int limit, Long warehouseId) {
        // Implement cursor-based pagination using native queries for better performance
        // Cursor would typically be encoded last_id or timestamp
        String cursorValue = decodeCursor(cursor);
        
        if ("inventory_items".equals(table)) {
            if (warehouseId != null) {
                return inventoryItemRepository.findByWarehouseIdWithCursor(warehouseId, cursorValue, direction, limit);
            }
            return inventoryItemRepository.findAllWithCursor(cursorValue, direction, limit);
        }
        
        return List.of();
    }

    /**
     * Multi-field sorting for complex stock reports
     */
    public Page<InventoryItem> getMultiFieldSortedStockReport(List<String> sortFields, List<String> sortDirections, 
                                                              Pageable pageable, Long warehouseId) {
        // Create complex sort combining multiple fields
        Sort sort = Sort.unsorted();
        for (int i = 0; i < sortFields.size(); i++) {
            String field = sortFields.get(i);
            String direction = sortDirections.get(i);
            Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
            
            switch (field.toLowerCase()) {
                case "available_quantity":
                    sort = sort.and(Sort.by(dir, "totalQuantity", "reservedQuantity")); // Custom logic for calculated field
                    break;
                case "warehouse_location":
                    sort = sort.and(Sort.by(dir, "warehouse.location"));
                    break;
                case "status":
                    sort = sort.and(Sort.by(dir, "status"));
                    break;
                case "created_at":
                    sort = sort.and(Sort.by(dir, "createdAt"));
                    break;
                default:
                    sort = sort.and(Sort.by(dir, field));
            }
        }
        
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        
        if (warehouseId != null) {
            return inventoryItemRepository.findByWarehouseId(warehouseId, sortedPageable);
        }
        
        return inventoryItemRepository.findAll(sortedPageable);
    }

    /**
     * Get stock report with aggregation and grouping
     */
    @io.micrometer.core.annotation.Timed(value = "inventory.aggregation.report", description = "Time taken for aggregated stock report")
    @Cacheable(value = "aggregatedReports", key = "#groupBy + '_' + #warehouseId + '_' + #timeRange")
    public Object getAggregatedStockReport(String groupBy, Long warehouseId, String timeRange) {
        // Implementation for aggregated reports (total stock by warehouse, by category, time-based trends)
        // This would use native queries or JPA projections for performance
        
        switch (groupBy.toLowerCase()) {
            case "warehouse":
                return getStockByWarehouseReport(warehouseId);
            case "status":
                return getStockByStatusReport(warehouseId);
            case "location":
                return getStockByLocationReport(warehouseId);
            case "time":
                return getStockTrendReport(warehouseId, timeRange);
            default:
                return getOverallStockSummary(warehouseId);
        }
    }

    private Sort createSort(String sortBy, String sortOrder) {
        if (sortBy == null || sortBy.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "availableQuantity"); // Default sorting
        }
        
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        
        return switch (sortBy.toLowerCase()) {
            case "available_quantity", "availablequantity" -> Sort.by(direction, "totalQuantity", "reservedQuantity");
            case "warehouse_location", "location" -> Sort.by(direction, "warehouse.location");
            case "status" -> Sort.by(direction, "status");
            case "created_at", "createdat" -> Sort.by(direction, "createdAt");
            case "updated_at", "updatedat" -> Sort.by(direction, "updatedAt");
            case "total_quantity", "totalquantity" -> Sort.by(direction, "totalQuantity");
            case "reserved_quantity", "reservedquantity" -> Sort.by(direction, "reservedQuantity");
            default -> Sort.by(direction, sortBy);
        };
    }

    private String decodeCursor(String cursor) {
        // Implement cursor decoding (base64 or custom encoding of last ID/timestamp)
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
        // Simplified implementation - in production use proper encoding
        return cursor;
    }

    // Aggregated report implementations
    private Object getStockByWarehouseReport(Long warehouseId) {
        // Implementation using native query or JPA projection
        if (warehouseId != null) {
            Integer totalStock = inventoryItemRepository.getTotalStockInWarehouse(warehouseId);
            return Map.of("warehouseId", warehouseId, "totalStock", totalStock, "itemsCount", inventoryItemRepository.findByWarehouseId(warehouseId).size());
        }
        // Global report - would need more complex query
        return Map.of("totalStock", "Aggregated across all warehouses");
    }

    private Object getStockByStatusReport(Long warehouseId) {
        // Count items by status
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("ACTIVE", inventoryItemRepository.findByStatus(InventoryStatus.ACTIVE, Pageable.unpaged()).getTotalElements());
        statusCounts.put("LOW_STOCK", inventoryItemRepository.findByStatus(InventoryStatus.LOW_STOCK, Pageable.unpaged()).getTotalElements());
        statusCounts.put("OUT_OF_STOCK", inventoryItemRepository.findByStatus(InventoryStatus.OUT_OF_STOCK, Pageable.unpaged()).getTotalElements());
        
        return Map.of("warehouseId", warehouseId, "statusCounts", statusCounts);
    }

    private Object getStockByLocationReport(Long warehouseId) {
        // Group by warehouse location
        return Map.of("warehouseId", warehouseId, "locationBasedStock", "Implementation needed");
    }

    private Object getStockTrendReport(Long warehouseId, String timeRange) {
        // Time-based trend analysis
        return Map.of("warehouseId", warehouseId, "timeRange", timeRange, "trendData", "Implementation needed");
    }

    private Object getOverallStockSummary(Long warehouseId) {
        Integer totalStock = inventoryItemRepository.getTotalStockInWarehouse(warehouseId);
        Long activeItems = inventoryItemRepository.findByStatus(InventoryStatus.ACTIVE, Pageable.unpaged()).getTotalElements();
        Long lowStockItems = inventoryItemRepository.findByStatus(InventoryStatus.LOW_STOCK, Pageable.unpaged()).getTotalElements();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("warehouseId", warehouseId);
        summary.put("totalStock", totalStock);
        summary.put("activeItems", activeItems);
        summary.put("lowStockItems", lowStockItems);
        summary.put("outOfStockItems", inventoryItemRepository.findByStatus(InventoryStatus.OUT_OF_STOCK, Pageable.unpaged()).getTotalElements());
        
        return summary;
    }

    // Cache management methods
    public void invalidateStockReportCache(Long warehouseId) {
        // Invalidate specific warehouse caches
    }

    public void invalidateGlobalStockCache() {
        // Invalidate global stock report caches
    }
}