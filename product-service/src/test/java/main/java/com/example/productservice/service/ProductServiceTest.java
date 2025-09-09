package test.java.main.java.com.example.productservice.service;

import main.java.main.java.com.example.productservice.model.Product;
import main.java.main.java.com.example.productservice.model.Category;
import main.java.main.java.com.example.productservice.repository.ProductRepository;
import main.java.main.java.com.example.productservice.repository.CategoryRepository;
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
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SearchService searchService;

    @Mock
    private FilterService filterService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private Category testCategory;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Electronics");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Laptop");
        testProduct.setDescription("A test product description");
        testProduct.setPrice(BigDecimal.valueOf(999.99));
        testProduct.setSku("TEST123");
        testProduct.setStockQuantity(10);
        testProduct.setStatus(Product.ProductStatus.ACTIVE);
        testProduct.setTags(Arrays.asList("laptop", "test"));
        testProduct.setCategory(testCategory);

        pageable = PageRequest.of(0, 20);
    }

    @Test
    void createProduct_Success() {
        // Arrange
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsBySku("TEST123")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        Product result = productService.createProduct(testProduct);

        // Assert
        assertNotNull(result);
        assertEquals("Test Laptop", result.getName());
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    void createProduct_SkuAlreadyExists_ThrowsException() {
        // Arrange
        when(productRepository.existsBySku("TEST123")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(testProduct);
        });
        assertEquals("SKU already exists: TEST123", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_CategoryNotFound_ThrowsException() {
        // Arrange
        testProduct.setCategory(testCategory);
        when(categoryRepository.existsById(1L)).thenReturn(false);
        when(productRepository.existsBySku("TEST123")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(testProduct);
        });
        assertEquals("Category not found: 1", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_Success() {
        // Arrange
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("Old Name");
        existingProduct.setSku("OLD123");

        Product updatedProductDetails = new Product();
        updatedProductDetails.setName("New Name");
        updatedProductDetails.setSku("NEW123");
        updatedProductDetails.setCategory(testCategory);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsBySku("NEW123")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        // Act
        Product result = productService.updateProduct(1L, updatedProductDetails);

        // Assert
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("NEW123", result.getSku());
        verify(productRepository, times(1)).save(existingProduct);
    }

    @Test
    void updateProduct_ProductNotFound_ThrowsException() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        Product updatedProductDetails = new Product();
        updatedProductDetails.setName("New Name");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.updateProduct(1L, updatedProductDetails);
        });
        assertEquals("Product not found: 1", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void partialUpdateProduct_Success() {
        // Arrange
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("Old Name");
        existingProduct.setPrice(BigDecimal.valueOf(500.00));

        Product partialUpdate = new Product();
        partialUpdate.setName("New Name");

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsBySku(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        // Act
        Product result = productService.partialUpdateProduct(1L, partialUpdate);

        // Assert
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals(BigDecimal.valueOf(500.00), result.getPrice()); // Unchanged field
        verify(productRepository, times(1)).save(existingProduct);
    }

    @Test
    void getProductById_Success() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        Optional<Product> result = productService.getProductById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Laptop", result.get().getName());
    }

    @Test
    void getProductById_NotFound() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<Product> result = productService.getProductById(1L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void getAllProducts_Success() {
        // Arrange
        List<Product> productList = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(productList, pageable, 1);
        when(productRepository.findAll(pageable)).thenReturn(productPage);

        // Act
        Page<Product> result = productService.getAllProducts(pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Laptop", result.getContent().get(0).getName());
    }

    @Test
    void getProductBySku_Success() {
        // Arrange
        when(productRepository.findBySku("TEST123")).thenReturn(Optional.of(testProduct));

        // Act
        Optional<Product> result = productService.getProductBySku("TEST123");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Laptop", result.get().getName());
    }

    @Test
    void deleteProduct_Success() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository, times(1)).save(argThat(product -> 
            product.getStatus() == Product.ProductStatus.DISCONTINUED));
    }

    @Test
    void deleteProduct_NotFound_ThrowsException() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.deleteProduct(1L);
        });
        assertEquals("Product not found: 1", exception.getMessage());
    }

    @Test
    void searchProducts_Success() {
        // Arrange
        List<Product> productList = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(productList, pageable, 1);
        when(searchService.searchProducts("laptop", pageable)).thenReturn(productPage);

        // Act
        Page<Product> result = productService.searchProducts("laptop", pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        verify(searchService, times(1)).searchProducts("laptop", pageable);
    }

    @Test
    void filterProducts_Success() {
        // Arrange
        List<Product> productList = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(productList, pageable, 1);
        when(filterService.filterProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(pageable)))
            .thenReturn(productPage);

        // Act
        Page<Product> result = productService.filterProducts(
            BigDecimal.valueOf(500), BigDecimal.valueOf(1500), 1L, "Electronics",
            "ACTIVE", true, 5, 100, LocalDateTime.now().minusDays(30), LocalDateTime.now(),
            LocalDateTime.now().minusDays(7), LocalDateTime.now(), Arrays.asList("laptop"),
            "color", "silver", pageable
        );

        // Assert
        assertEquals(1, result.getTotalElements());
        verify(filterService, times(1)).filterProducts(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(pageable));
    }
}