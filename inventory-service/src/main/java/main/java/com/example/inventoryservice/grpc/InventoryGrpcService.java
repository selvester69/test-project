package main.java.main.java.com.example.inventoryservice.grpc;

import io.grpc.stub.StreamObserver;
import main.java.main.java.com.example.inventoryservice.model.InventoryItem;
import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import main.java.main.java.com.example.inventoryservice.service.InventoryService;
import main.java.main.java.com.example.inventoryservice.service.InventoryEventService;
import net.devh.boot.grpc.server.service.GrpcService;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@GrpcService
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryEventService eventService;

    @Override
    public void checkStockAvailability(Inventory.CheckStockRequest request, StreamObserver<Inventory.StockCheckResponse> responseObserver) {
        try {
            long productId = request.getProductId();
            long warehouseId = request.getWarehouseId();
            int quantityNeeded = request.getQuantityNeeded();

            Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (warehouseOpt.isEmpty()) {
                Inventory.StockCheckResponse response = Inventory.StockCheckResponse.newBuilder()
                    .setAvailable(false)
                    .setMessage("Warehouse not found")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Optional<InventoryItem> inventoryOpt = inventoryService.getInventoryByProductAndWarehouse(productId, warehouseId);
            boolean available = false;
            int availableQuantity = 0;
            String message = "Stock not available";

            if (inventoryOpt.isPresent()) {
                InventoryItem item = inventoryOpt.get();
                availableQuantity = item.getAvailableQuantity();
                if (availableQuantity >= quantityNeeded) {
                    available = true;
                    message = "Stock available";
                }
            }

            Inventory.StockCheckResponse response = Inventory.StockCheckResponse.newBuilder()
                .setAvailable(available)
                .setAvailableQuantity(availableQuantity)
                .setMessage(message)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            Inventory.StockCheckResponse response = Inventory.StockCheckResponse.newBuilder()
                .setAvailable(false)
                .setMessage("Error checking stock: " + e.getMessage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void reserveStock(Inventory.ReserveStockRequest request, StreamObserver<Inventory.ReserveStockResponse> responseObserver) {
        try {
            long productId = request.getProductId();
            long warehouseId = request.getWarehouseId();
            int quantity = request.getQuantity();
            String orderId = request.getOrderId();

            Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (warehouseOpt.isEmpty()) {
                Inventory.ReserveStockResponse response = Inventory.ReserveStockResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Warehouse not found")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Check if reservation is possible
            Optional<InventoryItem> inventoryOpt = inventoryService.getInventoryByProductAndWarehouse(productId, warehouseId);
            if (inventoryOpt.isEmpty()) {
                Inventory.ReserveStockResponse response = Inventory.ReserveStockResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("No inventory found for product in warehouse")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            InventoryItem item = inventoryOpt.get();
            int available = item.getAvailableQuantity();
            if (available < quantity) {
                Inventory.ReserveStockResponse response = Inventory.ReserveStockResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Insufficient stock. Available: " + available + ", Requested: " + quantity)
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Perform reservation
            InventoryItem updatedItem = inventoryService.adjustReservedQuantity(productId, warehouseOpt.get(), quantity);
            
            // Publish stock changed event
            eventService.publishStockChangedEvent(updatedItem, "RESERVE", (available - quantity) + "", available + "");

            Inventory.ReserveStockResponse response = Inventory.ReserveStockResponse.newBuilder()
                .setSuccess(true)
                .setReservationId(orderId + "_" + System.currentTimeMillis())
                .setReservedQuantity(quantity)
                .setMessage("Stock reserved successfully")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            Inventory.ReserveStockResponse response = Inventory.ReserveStockResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Error reserving stock: " + e.getMessage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void releaseStock(Inventory.ReleaseStockRequest request, StreamObserver<Inventory.ReleaseStockResponse> responseObserver) {
        try {
            long productId = request.getProductId();
            long warehouseId = request.getWarehouseId();
            String orderId = request.getOrderId();
            String reservationId = request.getReservationId();

            Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (warehouseOpt.isEmpty()) {
                Inventory.ReleaseStockResponse response = Inventory.ReleaseStockResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Warehouse not found")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Release the reserved quantity (simplified - in real implementation, track exact reservation)
            Optional<InventoryItem> inventoryOpt = inventoryService.getInventoryByProductAndWarehouse(productId, warehouseId);
            if (inventoryOpt.isEmpty()) {
                Inventory.ReleaseStockResponse response = Inventory.ReleaseStockResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("No inventory found")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            InventoryItem item = inventoryOpt.get();
            int reserved = item.getReservedQuantity();
            int releaseQuantity = Math.min(reserved, 10); // Simplified - release fixed amount or track per order

            if (releaseQuantity > 0) {
                InventoryItem updatedItem = inventoryService.adjustReservedQuantity(productId, warehouseOpt.get(), -releaseQuantity);
                
                // Publish stock changed event
                eventService.publishStockChangedEvent(updatedItem, "RELEASE", reserved + "", (reserved - releaseQuantity) + "");

                Inventory.ReleaseStockResponse response = Inventory.ReleaseStockResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Released " + releaseQuantity + " units of stock")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                Inventory.ReleaseStockResponse response = Inventory.ReleaseStockResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("No stock to release")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }

        } catch (Exception e) {
            Inventory.ReleaseStockResponse response = Inventory.ReleaseStockResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Error releasing stock: " + e.getMessage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getInventoryDetails(Inventory.GetInventoryRequest request, StreamObserver<Inventory.GetInventoryResponse> responseObserver) {
        try {
            long productId = request.getProductId();
            List<Long> warehouseIds = request.getWarehouseIdsList();

            Inventory.GetInventoryResponse.Builder responseBuilder = Inventory.GetInventoryResponse.newBuilder();

            int totalAvailable = 0;

            if (warehouseIds.isEmpty()) {
                // Get all warehouses for this product
                List<InventoryItem> items = inventoryItemRepository.findByProductId(productId);
                for (InventoryItem item : items) {
                    Inventory.GetInventoryResponse.InventoryLocation.Builder locationBuilder = 
                        Inventory.GetInventoryResponse.InventoryLocation.newBuilder();
                    locationBuilder.setWarehouseId(item.getWarehouse().getId());
                    locationBuilder.setTotalQuantity(item.getTotalQuantity());
                    locationBuilder.setReservedQuantity(item.getReservedQuantity());
                    locationBuilder.setAvailableQuantity(item.getAvailableQuantity());
                    locationBuilder.setStatus(item.getStatus().name());
                    responseBuilder.addLocations(locationBuilder.build());
                    totalAvailable += item.getAvailableQuantity();
                }
            } else {
                // Get specific warehouses
                for (long warehouseId : warehouseIds) {
                    Optional<InventoryItem> itemOpt = inventoryService.getInventoryByProductAndWarehouse(productId, warehouseId);
                    if (itemOpt.isPresent()) {
                        InventoryItem item = itemOpt.get();
                        Inventory.GetInventoryResponse.InventoryLocation.Builder locationBuilder = 
                            Inventory.GetInventoryResponse.InventoryLocation.newBuilder();
                        locationBuilder.setWarehouseId(warehouseId);
                        locationBuilder.setTotalQuantity(item.getTotalQuantity());
                        locationBuilder.setReservedQuantity(item.getReservedQuantity());
                        locationBuilder.setAvailableQuantity(item.getAvailableQuantity());
                        locationBuilder.setStatus(item.getStatus().name());
                        responseBuilder.addLocations(locationBuilder.build());
                        totalAvailable += item.getAvailableQuantity();
                    }
                }
            }

            responseBuilder.setTotalAvailable(totalAvailable);
            responseBuilder.setOverallStatus(totalAvailable > 0 ? "IN_STOCK" : "OUT_OF_STOCK");

            Inventory.GetInventoryResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            Inventory.GetInventoryResponse response = Inventory.GetInventoryResponse.newBuilder()
                .setTotalAvailable(0)
                .setOverallStatus("ERROR")
                .setMessage("Error retrieving inventory: " + e.getMessage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void updateStockQuantity(Inventory.StockUpdateRequest request, StreamObserver<Inventory.StockUpdateResponse> responseObserver) {
        try {
            long productId = request.getProductId();
            long warehouseId = request.getWarehouseId();
            int newQuantity = request.getNewQuantity();
            String operation = request.getOperation();
            String source = request.getSource();

            Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (warehouseOpt.isEmpty()) {
                Inventory.StockUpdateResponse response = Inventory.StockUpdateResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Warehouse not found")
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            Optional<InventoryItem> inventoryOpt = inventoryService.getInventoryByProductAndWarehouse(productId, warehouseId);
            int previousQuantity = 0;
            
            if (inventoryOpt.isPresent()) {
                previousQuantity = inventoryOpt.get().getTotalQuantity();
            }

            InventoryItem updatedItem = inventoryService.updateStockQuantity(productId, warehouseOpt.get(), newQuantity);
            
            // Publish stock changed event
            eventService.publishStockChangedEvent(updatedItem, operation, previousQuantity + "", newQuantity + "");

            Inventory.StockUpdateResponse response = Inventory.StockUpdateResponse.newBuilder()
                .setSuccess(true)
                .setPreviousQuantity(previousQuantity)
                .setNewQuantity(newQuantity)
                .setMessage("Stock updated successfully from " + source)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            Inventory.StockUpdateResponse response = Inventory.StockUpdateResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Error updating stock: " + e.getMessage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getLowStockAlerts(Inventory.Empty request, StreamObserver<Inventory.LowStockAlertsResponse> responseObserver) {
        try {
            List<InventoryItem> lowStockItems = inventoryService.getLowStockItems(10);
            
            Inventory.LowStockAlertsResponse.Builder responseBuilder = Inventory.LowStockAlertsResponse.newBuilder();

            for (InventoryItem item : lowStockItems) {
                Inventory.LowStockAlertsResponse.Alert.Builder alertBuilder = Inventory.LowStockAlertsResponse.Alert.newBuilder();
                alertBuilder.setProductId(item.getProductId());
                alertBuilder.setWarehouseId(item.getWarehouse().getId());
                alertBuilder.setAvailableQuantity(item.getAvailableQuantity());
                alertBuilder.setThreshold(10);
                alertBuilder.setSeverity(item.getAvailableQuantity() <= 5 ? "CRITICAL" : "WARNING");
                // Product and warehouse names would come from additional queries
                responseBuilder.addAlerts(alertBuilder.build());
            }

            responseBuilder.setTotalAlerts(lowStockItems.size());

            Inventory.LowStockAlertsResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            Inventory.LowStockAlertsResponse response = Inventory.LowStockAlertsResponse.newBuilder()
                .setTotalAlerts(0)
                .setMessage("Error retrieving low stock alerts: " + e.getMessage())
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}