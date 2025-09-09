package main.java.main.java.com.example.inventoryservice.service;

import main.java.main.java.com.example.inventoryservice.model.InventoryItem;
import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import main.java.main.java.com.example.inventoryservice.repository.InventoryItemRepository;
import main.java.main.java.com.example.inventoryservice.repository.WarehouseRepository;
import org.apache.kafka.common.header.inbound.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryEventService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryEventService.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private InventoryService inventoryService;

    // Publish stock changed event
    @Async
    @Transactional
    public void publishStockChangedEvent(InventoryItem inventoryItem, String operation, String oldQuantity, String newQuantity) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "STOCK_CHANGED");
            event.put("productId", inventoryItem.getProductId());
            event.put("warehouseId", inventoryItem.getWarehouse().getId());
            event.put("oldQuantity", oldQuantity);
            event.put("newQuantity", newQuantity);
            event.put("availableQuantity", inventoryItem.getAvailableQuantity());
            event.put("status", inventoryItem.getStatus().name());
            event.put("operation", operation);
            event.put("timestamp", System.currentTimeMillis());
            event.put("service", "inventory-service");

            ListenableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send("stock.changed", inventoryItem.getProductId().toString(), event);

            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    logger.info("Stock changed event published successfully for product {}: partition={}, offset={}", 
                        inventoryItem.getProductId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }

                @Override
                public void onFailure(Throwable ex) {
                    logger.error("Failed to publish stock changed event for product {}: {}", inventoryItem.getProductId(), ex.getMessage());
                    // Implement retry or dead letter queue logic here
                }
            });

        } catch (Exception e) {
            logger.error("Error publishing stock changed event for product {}: {}", inventoryItem.getProductId(), e.getMessage());
        }
    }

    // Publish low stock alert
    @Async
    public void publishLowStockAlert(InventoryItem inventoryItem) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "INVENTORY_LOW");
            event.put("productId", inventoryItem.getProductId());
            event.put("warehouseId", inventoryItem.getWarehouse().getId());
            event.put("availableQuantity", inventoryItem.getAvailableQuantity());
            event.put("threshold", 10);
            event.put("timestamp", System.currentTimeMillis());
            event.put("service", "inventory-service");

            ListenableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send("inventory.low", inventoryItem.getProductId().toString(), event);

            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    logger.info("Low stock alert published for product {}: partition={}, offset={}", 
                        inventoryItem.getProductId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }

                @Override
                public void onFailure(Throwable ex) {
                    logger.error("Failed to publish low stock alert for product {}: {}", inventoryItem.getProductId(), ex.getMessage());
                }
            });

        } catch (Exception e) {
            logger.error("Error publishing low stock alert for product {}: {}", inventoryItem.getProductId(), e.getMessage());
        }
    }

    // Publish backorder notification
    @Async
    public void publishBackorderNotification(Long productId, Long warehouseId, Integer requestedQuantity) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BACKORDER_NOTIFICATION");
            event.put("productId", productId);
            event.put("warehouseId", warehouseId);
            event.put("requestedQuantity", requestedQuantity);
            event.put("timestamp", System.currentTimeMillis());
            event.put("service", "inventory-service");

            ListenableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send("backorder.notification", productId.toString(), event);

            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    logger.info("Backorder notification published for product {}: partition={}, offset={}", 
                        productId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }

                @Override
                public void onFailure(Throwable ex) {
                    logger.error("Failed to publish backorder notification for product {}: {}", productId, ex.getMessage());
                }
            });

        } catch (Exception e) {
            logger.error("Error publishing backorder notification for product {}: {}", productId, e.getMessage());
        }
    }

    // Consume product created events
    @KafkaListener(topics = "product.created", groupId = "inventory-group")
    @Transactional
    public void handleProductCreated(Map<String, Object> productEvent, Acknowledgment acknowledgment) {
        try {
            logger.info("Received product created event: {}", productEvent);
            
            Long productId = (Long) productEvent.get("productId");
            String sku = (String) productEvent.get("sku");
            String name = (String) productEvent.get("name");
            
            if (productId == null) {
                logger.warn("Product ID is null in product created event, skipping");
                acknowledgment.acknowledge();
                return;
            }

            // Get all active warehouses
            List<Warehouse> activeWarehouses = warehouseRepository.findByStatus(Warehouse.WarehouseStatus.ACTIVE);
            
            if (activeWarehouses.isEmpty()) {
                logger.warn("No active warehouses found for product {} initialization", productId);
                acknowledgment.acknowledge();
                return;
            }

            // Initialize inventory for each active warehouse with default quantity 0
            for (Warehouse warehouse : activeWarehouses) {
                try {
                    // Check if inventory already exists to avoid duplicates
                    if (!inventoryService.existsByProductIdAndWarehouseId(productId, warehouse.getId())) {
                        InventoryItem initialItem = inventoryService.initializeInventoryForProduct(
                            productId, warehouse.getId(), 0);
                        
                        // Publish stock changed event for initialization
                        publishStockChangedEvent(initialItem, "INITIALIZE", "0", "0");
                        
                        logger.info("Initialized inventory for product {} in warehouse {}", productId, warehouse.getId());
                    }
                } catch (Exception e) {
                    logger.error("Failed to initialize inventory for product {} in warehouse {}: {}", 
                        productId, warehouse.getId(), e.getMessage());
                    // Continue with other warehouses even if one fails
                }
            }

            acknowledgment.acknowledge();
            logger.info("Successfully processed product created event for productId: {}", productId);
            
        } catch (Exception e) {
            logger.error("Error processing product created event: {}", e.getMessage());
            // Don't acknowledge to trigger retry
            // acknowledgment.nack(1000); // Manual nack with delay for retry
        }
    }

    // Consume stock adjustment requests from order service
    @KafkaListener(topics = "order.stock.adjust", groupId = "inventory-group")
    @Transactional
    public void handleStockAdjustmentRequest(Map<String, Object> adjustmentEvent, Acknowledgment acknowledgment) {
        try {
            logger.info("Received stock adjustment request: {}", adjustmentEvent);
            
            Long productId = (Long) adjustmentEvent.get("productId");
            Long warehouseId = (Long) adjustmentEvent.get("warehouseId");
            Integer quantityChange = (Integer) adjustmentEvent.get("quantityChange");
            String operation = (String) adjustmentEvent.get("operation"); // "RESERVE", "RELEASE", "CANCEL"
            String orderId = (String) adjustmentEvent.get("orderId");
            
            if (productId == null || warehouseId == null || quantityChange == null) {
                logger.warn("Invalid stock adjustment request: missing required fields");
                acknowledgment.acknowledge();
                return;
            }

            Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (warehouseOpt.isEmpty()) {
                logger.error("Warehouse not found for ID: {}", warehouseId);
                acknowledgment.acknowledge();
                return;
            }

            Warehouse warehouse = warehouseOpt.get();
            String oldQuantity = null;
            InventoryItem updatedItem = null;

            if ("RESERVE".equals(operation)) {
                // Reserve stock for order
                oldQuantity = inventoryService.getInventoryByProductAndWarehouse(productId, warehouseId)
                    .map(item -> item.getReservedQuantity().toString())
                    .orElse("0");
                updatedItem = inventoryService.adjustReservedQuantity(productId, warehouse, quantityChange);
            } else if ("RELEASE".equals(operation)) {
                // Release reserved stock
                oldQuantity = inventoryService.getInventoryByProductAndWarehouse(productId, warehouseId)
                    .map(item -> item.getReservedQuantity().toString())
                    .orElse("0");
                updatedItem = inventoryService.adjustReservedQuantity(productId, warehouse, -quantityChange);
            } else if ("CANCEL".equals(operation)) {
                // Cancel order - release all reserved stock for this order
                // This would require tracking reservations per order, simplified here
                oldQuantity = "0";
                updatedItem = inventoryService.adjustReservedQuantity(productId, warehouse, -quantityChange);
            }

            if (updatedItem != null) {
                String newQuantity = updatedItem.getReservedQuantity().toString();
                // Publish stock changed event
                publishStockChangedEvent(updatedItem, operation, oldQuantity, newQuantity);
                
                // Send confirmation back to order service
                publishStockAdjustmentConfirmation(productId, warehouseId, orderId, operation, quantityChange, true);
                
                logger.info("Successfully processed stock adjustment for product {} in warehouse {}, operation: {}", 
                    productId, warehouseId, operation);
            }

            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing stock adjustment request: {}", e.getMessage());
            // Publish failure confirmation
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "STOCK_ADJUSTMENT_FAILED");
            event.put("productId", adjustmentEvent.get("productId"));
            event.put("warehouseId", adjustmentEvent.get("warehouseId"));
            event.put("orderId", adjustmentEvent.get("orderId"));
            event.put("error", e.getMessage());
            event.put("timestamp", System.currentTimeMillis());
            
            kafkaTemplate.send("order.stock.adjust.response", (String) adjustmentEvent.get("orderId"), event);
            
            // Don't acknowledge to trigger retry
        }
    }

    private void publishStockAdjustmentConfirmation(Long productId, Long warehouseId, String orderId, 
                                                   String operation, Integer quantityChange, boolean success) {
        try {
            Map<String, Object> confirmation = new HashMap<>();
            confirmation.put("eventType", "STOCK_ADJUSTMENT_CONFIRMED");
            confirmation.put("productId", productId);
            confirmation.put("warehouseId", warehouseId);
            confirmation.put("orderId", orderId);
            confirmation.put("operation", operation);
            confirmation.put("quantityChange", quantityChange);
            confirmation.put("success", success);
            confirmation.put("timestamp", System.currentTimeMillis());
            confirmation.put("service", "inventory-service");

            kafkaTemplate.send("order.stock.adjust.response", orderId, confirmation);
            logger.info("Stock adjustment confirmation sent for order {}: {}", orderId, success ? "SUCCESS" : "FAILED");
        } catch (Exception e) {
            logger.error("Failed to send stock adjustment confirmation for order {}: {}", orderId, e.getMessage());
        }
    }
}