package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.LoanProductRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanProductResponseDTO;
import in.zeta.microloan.platform.model.enums.LoanProductStatus;
import in.zeta.microloan.platform.service.LoanProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoanProductControllerTest {

    @Mock
    private LoanProductService productService;

    @InjectMocks
    private LoanProductController loanProductController;

    private UUID productId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productId = UUID.randomUUID();
    }

    @Test
    void testCreateProduct_Success() {
        // Arrange
        LoanProductRequestDTO requestDTO = LoanProductRequestDTO.builder()
                .name("Personal Loan")
                .description("Quick personal loan")
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12.5"))
                .processingFeeType("PERCENTAGE")
                .processingFeeValue(new BigDecimal("2"))
                .tenureMonths(12)
                .gracePeriodDays(3)
                .lateFeePercent(new BigDecimal("0.5"))
                .maxLateFeePercent(new BigDecimal("5"))
                .prepaymentChargesType("PERCENTAGE")
                .prepaymentChargesValue(new BigDecimal("2"))
                .build();

        LoanProductResponseDTO responseDTO = LoanProductResponseDTO.builder()
                .id(productId)
                .name("Personal Loan")
                .description("Quick personal loan")
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12.5"))
                .status(LoanProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(productService.createProduct(any(LoanProductRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<LoanProductResponseDTO> response = loanProductController.createProduct(requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(productId, response.getBody().getId());
        assertEquals("Personal Loan", response.getBody().getName());
        verify(productService, times(1)).createProduct(any(LoanProductRequestDTO.class));
    }

    @Test
    void testGetAllProducts_ActiveOnly() {
        // Arrange
        LoanProductResponseDTO product1 = LoanProductResponseDTO.builder()
                .id(UUID.randomUUID())
                .name("Product 1")
                .status(LoanProductStatus.ACTIVE)
                .build();

        LoanProductResponseDTO product2 = LoanProductResponseDTO.builder()
                .id(UUID.randomUUID())
                .name("Product 2")
                .status(LoanProductStatus.ACTIVE)
                .build();

        List<LoanProductResponseDTO> products = Arrays.asList(product1, product2);

        when(productService.getAllActiveProducts()).thenReturn(products);

        // Act
        ResponseEntity<List<LoanProductResponseDTO>> response =
                loanProductController.getAllProducts(true);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(productService, times(1)).getAllActiveProducts();
        verify(productService, never()).getAllProducts();
    }

    @Test
    void testGetAllProducts_AllProducts() {
        // Arrange
        LoanProductResponseDTO product1 = LoanProductResponseDTO.builder()
                .id(UUID.randomUUID())
                .name("Product 1")
                .status(LoanProductStatus.ACTIVE)
                .build();

        LoanProductResponseDTO product2 = LoanProductResponseDTO.builder()
                .id(UUID.randomUUID())
                .name("Product 2")
                .status(LoanProductStatus.DELETED)
                .build();

        List<LoanProductResponseDTO> products = Arrays.asList(product1, product2);

        when(productService.getAllProducts()).thenReturn(products);

        // Act
        ResponseEntity<List<LoanProductResponseDTO>> response =
                loanProductController.getAllProducts(false);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(productService, times(1)).getAllProducts();
        verify(productService, never()).getAllActiveProducts();
    }

    @Test
    void testGetProductById_Success() {
        // Arrange
        LoanProductResponseDTO responseDTO = LoanProductResponseDTO.builder()
                .id(productId)
                .name("Personal Loan")
                .status(LoanProductStatus.ACTIVE)
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .build();

        when(productService.getProductById(productId)).thenReturn(responseDTO);

        // Act
        ResponseEntity<LoanProductResponseDTO> response =
                loanProductController.getProductById(productId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(productId, response.getBody().getId());
        assertEquals("Personal Loan", response.getBody().getName());
        verify(productService, times(1)).getProductById(productId);
    }

    @Test
    void testUpdateProduct_Success() {
        // Arrange
        LoanProductRequestDTO requestDTO = LoanProductRequestDTO.builder()
                .name("Updated Personal Loan")
                .description("Updated description")
                .minAmount(new BigDecimal("15000"))
                .maxAmount(new BigDecimal("150000"))
                .interestRate(new BigDecimal("11.5"))
                .processingFeeType("FLAT")
                .processingFeeValue(new BigDecimal("500"))
                .tenureMonths(18)
                .gracePeriodDays(5)
                .lateFeePercent(new BigDecimal("0.75"))
                .maxLateFeePercent(new BigDecimal("6"))
                .prepaymentChargesType("FLAT")
                .prepaymentChargesValue(new BigDecimal("1000"))
                .build();

        LoanProductResponseDTO responseDTO = LoanProductResponseDTO.builder()
                .id(productId)
                .name("Updated Personal Loan")
                .description("Updated description")
                .minAmount(new BigDecimal("15000"))
                .maxAmount(new BigDecimal("150000"))
                .status(LoanProductStatus.ACTIVE)
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.updateProduct(eq(productId), any(LoanProductRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<LoanProductResponseDTO> response =
                loanProductController.updateProduct(productId, requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(productId, response.getBody().getId());
        assertEquals("Updated Personal Loan", response.getBody().getName());
        verify(productService, times(1)).updateProduct(eq(productId), any(LoanProductRequestDTO.class));
    }

    @Test
    void testDeleteProduct_Success() {
        // Arrange
        doNothing().when(productService).deleteProduct(productId);

        // Act
        ResponseEntity<Void> response = loanProductController.deleteProduct(productId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productService, times(1)).deleteProduct(productId);
    }
}