package test.java.test.java.main.java.com.example.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.java.main.java.com.example.productservice.model.Product;
import main.java.main.java.com.example.productservice.model.Category;
import main.java.main.java.com.example.productservice.ProductServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ProductServiceApplication.class)
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProductControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Create test category if needed
        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setDescription("Test category description");
    }

    @Test
    void createProduct_Success() throws Exception {
        // Arrange
        Product newProduct = new Product();
        newProduct.setName("Integration Test Product");
        newProduct.setDescription("Test product description");
        newProduct.setPrice(BigDecimal.valueOf(999.99));
        newProduct.setSku("INTEGRATION-001");
        newProduct.setStockQuantity(50);
        newProduct.setStatus(Product.ProductStatus.ACTIVE);
        newProduct.setTags(Arrays.asList("test", "integration"));
        newProduct.setCategory(testCategory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Product> request = new HttpEntity<>(newProduct, headers);

        // Act
        ResponseEntity<Product> response = restTemplate.postForEntity(
            createURLWithPort("/api/products"), request, Product.class);

        // Assert
        assertEquals(HttpStatus.CREATED.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Integration Test Product", response.getBody().getName());
        assertNotNull(response.getBody().getId());
        assertNotNull(response.getBody().getCreatedAt());
        assertNotNull(response.getBody().getUpdatedAt());
    }

    @Test
    void createProduct_InvalidData_BadRequest() throws Exception {
        // Arrange
        Product invalidProduct = new Product();
        // Missing required fields

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Product> request = new HttpEntity<>(invalidProduct, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
            createURLWithPort("/api/products"), request, String.class);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());
    }

    @Test
    void getProductById_Success() {
        // Assuming test data includes a product with ID 1
        // Act
        ResponseEntity<Product> response = restTemplate.getForEntity(
            createURLWithPort("/api/products/1"), Product.class);

        // Assert
        assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    void getProductById_NotFound() {
        // Act
        ResponseEntity<Product> response = restTemplate.getForEntity(
            createURLWithPort("/api/products/999"), Product.class);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCodeValue());
    }

    @Test
    void getAllProducts_Pagination() {
        // Act
        ResponseEntity<Map> response = restTemplate.getForEntity(
            createURLWithPort("/api/products?page=1&limit=5"), Map.class);

        // Assert
        assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("data"));
        assertTrue(body.containsKey("total_count"));
        assertTrue(body.containsKey("total_pages"));
        assertTrue(body.containsKey("current_page"));
        assertEquals(1, body.get("current_page"));
        assertEquals(5, ((Map) body.get("data")).size() <= 5); // Check size constraint
    }

    @Test
    void searchProducts_Success() {
        // Act
        ResponseEntity<Map> response = restTemplate.getForEntity(
            createURLWithPort("/api/products/search?q=laptop"), Map.class);

        // Assert
        assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("data"));
    }

    @Test
    void filterProducts_PriceRange() {
        // Act
        ResponseEntity<Map> response = restTemplate.getForEntity(
            createURLWithPort("/api/products/filter?min_price=500&max_price=1500"), Map.class);

        // Assert
        assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("data"));
        // Verify products are within price range
        List<Map<String, Object>> products = (List<Map<String, Object>>) body.get("data");
        for (Map<String, Object> product : products) {
            BigDecimal price = new BigDecimal(product.get("price").toString());
            assertTrue(price.compareTo(BigDecimal.valueOf(500)) >= 0);
            assertTrue(price.compareTo(BigDecimal.valueOf(1500)) <= 0);
        }
    }

    @Test
    void filterProducts_CategoryAndStatus() {
        // Act
        ResponseEntity<Map> response = restTemplate.getForEntity(
            createURLWithPort("/api/products/filter?category_id=1&status=ACTIVE"), Map.class);

        // Assert
        assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("data"));
        // Verify category and status filtering
        List<Map<String, Object>> products = (List<Map<String, Object>>) body.get("data");
        for (Map<String, Object> product : products) {
            assertEquals(1L, product.get("category.id"));
            assertEquals("ACTIVE", product.get("status"));
        }
    }

    @Test
    void updateProduct_Success() throws Exception {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setName("Updated Product Name");
        updatedProduct.setPrice(BigDecimal.valueOf(1200.00));
        updatedProduct.setSku("UPDATED-001");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Product> request = new HttpEntity<>(updatedProduct, headers);

        // Act
        ResponseEntity<Product> response = restTemplate.exchange(
            createURLWithPort("/api/products/1"), HttpMethod.PUT, request, Product.class);

        // Assert
        assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Updated Product Name", response.getBody().getName());
        assertEquals(BigDecimal.valueOf(1200.00), response.getBody().getPrice());
    }

    @Test
    void partialUpdateProduct_Success() throws Exception {
        // Arrange
        Map<String, Object> partialUpdate = new HashMap<>();
        partialUpdate.put("name", "Partially Updated Product");
        partialUpdate.put("price", 1100.00);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map> request = new HttpEntity<>(partialUpdate, headers);

        // Act
        ResponseEntity<Product> response = restTemplate.exchange(
            createURLWithPort("/api/products/1"), HttpMethod.PATCH, request, Product.class);

        // Assert
        assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("Partially Updated Product", response.getBody().getName());
        assertEquals(BigDecimal.valueOf(1100.00), response.getBody().getPrice());
    }

    @Test
    void deleteProduct_Success() {
        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
            createURLWithPort("/api/products/1"), HttpMethod.DELETE, null, Void.class);

        // Assert
        assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
        
        // Verify soft delete by checking status
        ResponseEntity<Product> productResponse = restTemplate.getForEntity(
            createURLWithPort("/api/products/1"), Product.class);
        assertEquals(Product.ProductStatus.DISCONTINUED, productResponse.getBody().getStatus());
    }

    @Test
    void deleteProduct_NotFound() {
        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
            createURLWithPort("/api/products/999"), HttpMethod.DELETE, null, Void.class);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCodeValue());
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }
}