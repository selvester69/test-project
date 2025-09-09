package test.java.main.java.com.example.inventoryservice.service;

import main.java.main.java.com.example.inventoryservice.model.InventoryItem;
import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import main.java.main.java.com.example.inventoryservice.model.InventoryItem.InventoryStatus;
import main.java.main.java.com.example.inventoryservice.repository.InventoryItemRepository;
import main.java.main.java.com.example.inventoryservice.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Warehouse warehouse;
    private InventoryItem inventoryItem;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Test Warehouse");
        warehouse.setLocation("Test Location");

        inventoryItem = new InventoryItem();
        inventoryItem.setId(1L);
        inventoryItem.setProductId(100L);
        inventoryItem.setWarehouse(warehouse);
        inventoryItem.setTotalQuantity(50);
        inventoryItem.setReservedQuantity(10);
        inventoryItem.setStatus(InventoryStatus.ACTIVE);
    }

    @Test
    void createInventoryItem_Success() {
        // Arrange
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.existsByProductIdAndWarehouseId(100L, warehouse)).thenReturn(false);
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        // Act
        InventoryItem result = inventoryService.createInventoryItem(inventoryItem);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(100L, result.getProductId());
        assertEquals(1L, result.getWarehouse().getId());
        verify(inventoryItemRepository).save(any(InventoryItem.class));
    }

    @Test
    void createInventoryItem_WarehouseNotFound_ThrowsException() {
        // Arrange
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.createInventoryItem(inventoryItem);
        });
        verify(warehouseRepository).findById(1L);
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void createInventoryItem_DuplicateEntry_ThrowsException() {
        // Arrange
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.existsByProductIdAndWarehouseId(100L, warehouse)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.createInventoryItem(inventoryItem);
        });
        verify(inventoryItemRepository).existsByProductIdAndWarehouseId(100L, warehouse);
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void updateInventoryItem_Success() {
        // Arrange
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        InventoryItem updateDetails = new InventoryItem();
        updateDetails.setTotalQuantity(75);
        updateDetails.setReservedQuantity(15);

        // Act
        InventoryItem result = inventoryService.updateInventoryItem(1L, updateDetails);

        // Assert
        assertNotNull(result);
        assertEquals(75, result.getTotalQuantity());
        assertEquals(15, result.getReservedQuantity());
        assertEquals(InventoryStatus.ACTIVE, result.getStatus()); // Should still be ACTIVE
        verify(inventoryItemRepository).save(any(InventoryItem.class));
    }

    @Test
    void updateInventoryItem_NotFound_ThrowsException() {
        // Arrange
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.empty());

        InventoryItem updateDetails = new InventoryItem();
        updateDetails.setTotalQuantity(75);

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            inventoryService.updateInventoryItem(1L, updateDetails);
        });
        verify(inventoryItemRepository).findById(1L);
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void updateStockQuantity_Success_ExistingItem() {
        // Arrange
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByProductIdAndWarehouseId(100L, warehouse)).thenReturn(Optional.of(inventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        // Act
        InventoryItem result = inventoryService.updateStockQuantity(100L, warehouse, 75);

        // Assert
        assertNotNull(result);
        assertEquals(75, result.getTotalQuantity());
        verify(inventoryItemRepository).save(argThat(item -> item.getTotalQuantity() == 75));
    }

    @Test
    void updateStockQuantity_Success_NewItem() {
        // Arrange
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByProductIdAndWarehouseId(100L, warehouse)).thenReturn(Optional.empty());
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        // Act
        InventoryItem result = inventoryService.updateStockQuantity(100L, warehouse, 25);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getProductId());
        assertEquals(1L, result.getWarehouse().getId());
        assertEquals(25, result.getTotalQuantity());
        verify(inventoryItemRepository).save(argThat(item -> 
            item.getProductId().equals(100L) && item.getTotalQuantity().equals(25)));
    }

    @Test
    void adjustReservedQuantity_Success() {
        // Arrange
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByProductIdAndWarehouseId(100L, warehouse)).thenReturn(Optional.of(inventoryItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);

        // Act
        InventoryItem result = inventoryService.adjustReservedQuantity(100L, warehouse, 5);

        // Assert
        assertNotNull(result);
        assertEquals(15, result.getReservedQuantity()); // 10 + 5
        assertEquals(35, result.getAvailableQuantity()); // 50 - 15
        verify(inventoryItemRepository).save(argThat(item -> item.getReservedQuantity() == 15));
    }

    @Test
    void adjustReservedQuantity_InsufficientStock_ThrowsException() {
        // Arrange
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByProductIdAndWarehouseId(100L, warehouse)).thenReturn(Optional.of(inventoryItem));
        
        // Try to reserve more than available (available = 40, try to reserve 50)
        inventoryItem.setReservedQuantity(40); // Make available = 10

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.adjustReservedQuantity(100L, warehouse, 50);
        });
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void adjustReservedQuantity_NegativeQuantity_ThrowsException() {
        // Arrange
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(inventoryItemRepository.findByProductIdAndWarehouseId(100L, warehouse)).thenReturn(Optional.of(inventoryItem));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.adjustReservedQuantity(100L, warehouse, -15); // Would make reserved = -5
        });
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void getInventoryByProductId_Success() {
        // Arrange
        List<InventoryItem> items = List.of(inventoryItem);
        when(inventoryItemRepository.findByProductId(100L)).thenReturn(items);

        // Act
        Optional<InventoryItem> result = inventoryService.getInventoryByProductId(100L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L