package test.java.main.java.com.example.inventoryservice.controller;

import main.java.main.java.com.example.inventoryservice.InventoryServiceApplication;
import main.java.main.java.com.example.inventoryservice.model.InventoryItem;
import main.java.main.java.com.example.inventoryservice.model.Warehouse;
import main.java.main.java.com.example.inventoryservice.model.InventoryItem.InventoryStatus;
import main.java.main.java.com.example.inventoryservice.repository.InventoryItemRepository;
import main.java.main.java.com.example.inventoryservice.repository.WarehouseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = InventoryServiceApplication.class
)
@AutoConfigureMockMvc
@Sql(scripts = {"/test-data.sql", "/cleanup.sql"}, executionPhase = Sql.ExecutionPhase.TEST_EXECUTION)
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    private Warehouse testWarehouse;
    private InventoryItem testInventoryItem;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        inventoryItemRepository.deleteAll();
        warehouseRepository.deleteAll();

        // Create test data
        testWarehouse = new Warehouse();
        testWarehouse.setName("Test Warehouse 1");
        testWarehouse.setLocation("Test City");
        testWarehouse.setCapacity(1000);
        testWarehouse = warehouseRepository.save(testWarehouse);

        testInventoryItem = new InventoryItem();
        testInventoryItem.setProductId(100L);
        testInventoryItem.setWarehouse(testWarehouse);
        testInventoryItem.setTotalQuantity(50);
        testInventoryItem.setReservedQuantity(10);
        testInventoryItem.setStatus(InventoryStatus.ACTIVE);
    }

    @Test
    void createInventoryItem_Success() throws Exception {
        // Arrange
        InventoryItem newItem = new InventoryItem();
        newItem.setProductId(200L);
        newItem.setWarehouse(testWarehouse);
        newItem.setTotalQuantity(100);
        newItem.setReservedQuantity(0);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/inventory/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.productId").value(200L))
                .andExpect(jsonPath("$.totalQuantity").value(100))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Verify database
        List<InventoryItem> savedItems = inventoryItemRepository.findAll();
        assertEquals(2, savedItems.size()); // Original + new item
    }

    @Test
    void createInventoryItem_InvalidData_BadRequest() throws Exception {
        // Arrange
        InventoryItem invalidItem = new InventoryItem();
        invalidItem.setProductId(null); // Invalid - required field
        invalidItem.setWarehouse(testWarehouse);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/inventory/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getInventoryItemById_Success() throws Exception {
        // Arrange - Save test item
        InventoryItem savedItem = inventoryItemRepository.save(testInventoryItem);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/items/{id}", savedItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedItem.getId()))
                .andExpect(jsonPath("$.productId").value(100L))
                .andExpect(jsonPath("$.totalQuantity").value(50))
                .andExpect(jsonPath("$.availableQuantity").value(40))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getInventoryItemById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/items/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllInventoryItems_Pagination() throws Exception {
        // Arrange - Create multiple items
        InventoryItem item2 = new InventoryItem();
        item2.setProductId(101L);
        item2.setWarehouse(testWarehouse);
        item2.setTotalQuantity(25);
        item2.setReservedQuantity(5);
        inventoryItemRepository.save(item2);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/items")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "totalQuantity,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void updateInventoryItem_Success() throws Exception {
        // Arrange - Save test item
        InventoryItem savedItem = inventoryItemRepository.save(testInventoryItem);

        InventoryItem updateData = new InventoryItem();
        updateData.setTotalQuantity(75);
        updateData.setReservedQuantity(20);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/api/inventory/items/{id}", savedItem.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedItem.getId()))
                .andExpect(jsonPath("$.totalQuantity").value(75))
                .andExpect(jsonPath("$.reservedQuantity").value(20))
                .andExpect(jsonPath("$.availableQuantity").value(55))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Verify database update
        Optional<InventoryItem> updatedItem = inventoryItemRepository.findById(savedItem.getId());
        assertTrue(updatedItem.isPresent());
        assertEquals(75, updatedItem.get().getTotalQuantity());
        assertEquals(20, updatedItem.get().getReservedQuantity());
    }

    @Test
    void partialUpdateInventoryItem_Success() throws Exception {
        // Arrange - Save test item
        InventoryItem savedItem = inventoryItemRepository.save(testInventoryItem);

        // Only update total quantity
        InventoryItem partialUpdate = new InventoryItem();
        partialUpdate.setTotalQuantity(100);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/inventory/items/{id}", savedItem.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedItem.getId()))
                .andExpect(jsonPath("$.totalQuantity").value(100))
                .andExpect(jsonPath("$.reservedQuantity").value(10)) // Unchanged
                .andExpect(jsonPath("$.availableQuantity").value(90)); // Updated

        // Verify database
        Optional<InventoryItem> updatedItem = inventoryItemRepository.findById(savedItem.getId());
        assertTrue(updatedItem.isPresent());
        assertEquals(100, updatedItem.get().getTotalQuantity());
        assertEquals(10, updatedItem.get().getReservedQuantity()); // Should remain unchanged
    }

    @Test
    void deleteInventoryItem_Success() throws Exception {
        // Arrange - Save test item
        InventoryItem savedItem = inventoryItemRepository.save(testInventoryItem);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/inventory/items/{id}", savedItem.getId()))
                .andExpect(status().isNoContent());

        // Verify soft delete - status should be OUT_OF_STOCK
        Optional<InventoryItem> deletedItem = inventoryItemRepository.findById(savedItem.getId());
        assertTrue(deletedItem.isPresent());
        assertEquals(InventoryStatus.OUT_OF_STOCK, deletedItem.get().getStatus());
    }

    @Test
    void getInventoryByProductId_Success() throws Exception {
        // Arrange - Save test item
        inventoryItemRepository.save(testInventoryItem);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/product/{productId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.productId").value(100L))
                .andExpect(jsonPath("$.totalQuantity").value(50))
                .andExpect(jsonPath("$.availableQuantity").value(40));
    }

    @Test
    void getInventoryByProductId_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/product/{productId}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInventoryByProductAndWarehouse_Success() throws Exception {
        // Arrange - Save test item
        inventoryItemRepository.save(testInventoryItem);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/product/{productId}/warehouse/{warehouseId}", 100L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.productId").value(100L))
                .andExpect(jsonPath("$.warehouse.id").value(1L));
    }

    @Test
    void getInventoryByProductAndWarehouse_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/product/{productId}/warehouse/{warehouseId}", 999L, 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInventoryByWarehouse_Success() throws Exception {
        // Arrange - Save test item
        inventoryItemRepository.save(testInventoryItem);

        InventoryItem item2 = new InventoryItem();
        item2.setProductId(101L);
        item2.setWarehouse(testWarehouse);
        item2.setTotalQuantity(25);
        inventoryItemRepository.save(item2);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/warehouse/{warehouseId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("[0].warehouse.id").value(1L))
                .andExpect(jsonPath("[1].warehouse.id").value(1L));
    }

    @Test
    void getLowStockItems_Success() throws Exception {
        // Arrange - Create low stock item
        InventoryItem lowStockItem = new InventoryItem();
        lowStockItem.setProductId(200L);
        lowStockItem.setWarehouse(testWarehouse);
        lowStockItem.setTotalQuantity(8);
        lowStockItem.setReservedQuantity(3);
        inventoryItemRepository.save(lowStockItem);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/low-stock")
                .param("threshold", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("[0].productId").value(200L))
                .andExpect(jsonPath("[0].availableQuantity").value(5));
    }

    @Test
    void updateStockQuantity_Success() throws Exception {
        // Arrange - Save test item
        inventoryItemRepository.save(testInventoryItem);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/inventory/stock/{productId}/warehouse/{warehouseId}")
                .param("newQuantity", "75")
                .param("warehouseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(75))
                .andExpect(jsonPath("$.availableQuantity").value(65)); // 75 - 10 reserved

        // Verify database update
        Optional<InventoryItem> updatedItem = inventoryItemRepository.findById(1L);
        assertTrue(updatedItem.isPresent());
        assertEquals(75, updatedItem.get().getTotalQuantity());
    }

    @Test
    void adjustReservedQuantity_Success() throws Exception {
        // Arrange - Save test item
        inventoryItemRepository.save(testInventoryItem);

        // Act & Assert - Reserve 5 more units
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/inventory/reserve/{productId}/warehouse/{warehouseId}")
                .param("quantityChange", "5")
                .param("warehouseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(15))
                .andExpect(jsonPath("$.availableQuantity").value(35));

        // Verify database
        Optional<InventoryItem> updatedItem = inventoryItemRepository.findById(1L);
        assertTrue(updatedItem.isPresent());
        assertEquals(15, updatedItem.get().getReservedQuantity());
    }

    @Test
    void adjustReservedQuantity_InsufficientStock_BadRequest() throws Exception {
        // Arrange - Set low stock
        testInventoryItem.setTotalQuantity(12);
        testInventoryItem.setReservedQuantity(10);
        inventoryItemRepository.save(testInventoryItem);

        // Act & Assert - Try to reserve 5 more (only 2 available)
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/inventory/reserve/{productId}/warehouse/{warehouseId}")
                .param("quantityChange", "5")
                .param("warehouseId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTotalStockForProduct_Success() throws Exception {
        // Arrange - Save test item
        inventoryItemRepository.save(testInventoryItem);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/product/{productId}/total-stock", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(100L))
                .andExpect(jsonPath("$.totalStock").value(50));
    }

    @Test
    void initializeInventoryForProduct_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/inventory/initialize/{productId}/warehouse/{warehouseId}")
                .param("initialQuantity", "100")
                .param("warehouseId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(100L))
                .andExpect(jsonPath("$.totalQuantity").value(100))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Verify database has new item
        List<InventoryItem> items = inventoryItemRepository.findAll();
        assertEquals(1, items.size());
        assertEquals(100L, items.get(0).getProductId());
        assertEquals(100, items.get(0).getTotalQuantity());
    }

    @Test
    void testSearchEndpoints() throws Exception {
        // Arrange - Save test items
        inventoryItemRepository.save(testInventoryItem);

        InventoryItem lowStockItem = new InventoryItem();
        lowStockItem.setProductId(200L);
        lowStockItem.setWarehouse(testWarehouse);
        lowStockItem.setTotalQuantity(8);
        lowStockItem.setReservedQuantity(3);
        inventoryItemRepository.save(lowStockItem);

        // Act & Assert - Search by warehouse
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/warehouse/{warehouseId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        // Act & Assert - Low stock search
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/low-stock")
                .param("threshold", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("[0].productId").value(200L));

        // Act & Assert - Out of stock search (should be empty)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/items")
                .param("status", "OUT_OF_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void testPaginationWithSorting() throws Exception {
        // Arrange - Create multiple items with different quantities
        InventoryItem item1 = new InventoryItem();
        item1.setProductId(101L);
        item1.setWarehouse(testWarehouse);
        item1.setTotalQuantity(100);
        item1.setReservedQuantity(10);
        inventoryItemRepository.save(item1);

        InventoryItem item2 = new InventoryItem();
        item2.setProductId(102L);
        item2.setWarehouse(testWarehouse);
        item2.setTotalQuantity(50);
        item2.setReservedQuantity(5);
        inventoryItemRepository.save(item2);

        InventoryItem item3 = new InventoryItem();
        item3.setProductId(103L);
        item3.setWarehouse(testWarehouse);
        item3.setTotalQuantity(75);
        item3.setReservedQuantity(15);
        inventoryItemRepository.save(item3);

        // Act & Assert - Paginated results with sorting by totalQuantity DESC
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/items")
                .param("page", "0")
                .param("size", "2")
                .param("sort", "totalQuantity,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].totalQuantity").value(100))
                .andExpect(jsonPath("$.content[1].totalQuantity").value(75))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}