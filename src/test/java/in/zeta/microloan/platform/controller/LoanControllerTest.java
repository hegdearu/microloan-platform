package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.LoanIssuanceRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanDetailResponseDTO;
import in.zeta.microloan.platform.dto.response.LoanResponseDTO;
import in.zeta.microloan.platform.model.enums.LoanStatus;
import in.zeta.microloan.platform.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoanControllerTest {

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanController loanController;

    private UUID loanId;
    private UUID borrowerId;
    private LoanResponseDTO loanResponseDTO;
    private LoanIssuanceRequestDTO issuanceRequestDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loanId = UUID.randomUUID();
        borrowerId = UUID.randomUUID();

        loanResponseDTO = LoanResponseDTO.builder()
                .id(loanId)
                .loanNumber("LN-2024-123456")
                .borrowerId(borrowerId)
                .householdId(UUID.randomUUID())
                .principalAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12.5"))
                .tenureMonths(12)
                .emiAmount(new BigDecimal("8938"))
                .totalPayable(new BigDecimal("107256"))
                .totalOutstanding(new BigDecimal("107256"))
                .totalPaid(BigDecimal.ZERO)
                .disbursementDate(LocalDate.now())
                .firstDueDate(LocalDate.now().plusMonths(1))
                .status(LoanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        issuanceRequestDTO = LoanIssuanceRequestDTO.builder()
                .borrowerId(borrowerId)
                .productId(UUID.randomUUID())
                .principalAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12.5"))
                .tenureMonths(12)
                .repaymentFrequency("MONTHLY")
                .disbursementMethod("BANK_TRANSFER")
                .disbursementDate(LocalDate.now())
                .firstDueDate(LocalDate.now().plusMonths(1))
                .build();
    }

    @Test
    void testCreateLoan_Success() {
        // Arrange
        when(loanService.createLoan(any(LoanIssuanceRequestDTO.class), anyLong()))
                .thenReturn(loanResponseDTO);

        // Act
        ResponseEntity<LoanResponseDTO> response =
                loanController.createLoan(issuanceRequestDTO, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(loanId, response.getBody().getId());
        assertEquals("LN-2024-123456", response.getBody().getLoanNumber());
        verify(loanService, times(1)).createLoan(any(LoanIssuanceRequestDTO.class), anyLong());
    }

    @Test
    void testGetLoanById_Success() {
        // Arrange
        when(loanService.getLoanById(loanId)).thenReturn(loanResponseDTO);

        // Act
        ResponseEntity<LoanResponseDTO> response =
                loanController.getLoanById(loanId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(loanId, response.getBody().getId());
        verify(loanService, times(1)).getLoanById(loanId);
    }

    @Test
    void testGetLoanDetails_Success() {
        // Arrange
        LoanDetailResponseDTO detailDTO = LoanDetailResponseDTO.builder()
                .id(loanId)
                .loanNumber("LN-2024-123456")
                .borrowerId(borrowerId)
                .borrowerName("Test Borrower")
                .borrowerPhone("9876543210")
                .principalAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12.5"))
                .tenureMonths(12)
                .emiAmount(new BigDecimal("8938"))
                .totalPayable(new BigDecimal("107256"))
                .outstandingPrincipal(new BigDecimal("100000"))
                .outstandingInterest(new BigDecimal("7256"))
                .totalOutstanding(new BigDecimal("107256"))
                .totalPaid(BigDecimal.ZERO)
                .disbursementDate(LocalDate.now())
                .firstDueDate(LocalDate.now().plusMonths(1))
                .status(LoanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(loanService.getLoanDetails(loanId)).thenReturn(detailDTO);

        // Act
        ResponseEntity<LoanDetailResponseDTO> response =
                loanController.getLoanDetails(loanId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Borrower", response.getBody().getBorrowerName());
        verify(loanService, times(1)).getLoanDetails(loanId);
    }

    @Test
    void testGetLoansByBorrower_Success() {
        // Arrange
        List<LoanResponseDTO> loans = Arrays.asList(loanResponseDTO);
        when(loanService.getLoansByBorrower(borrowerId)).thenReturn(loans);

        // Act
        ResponseEntity<List<LoanResponseDTO>> response =
                loanController.getLoansByBorrower(borrowerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(borrowerId, response.getBody().get(0).getBorrowerId());
        verify(loanService, times(1)).getLoansByBorrower(borrowerId);
    }

    @Test
    void testGetLoansByBorrower_EmptyList() {
        // Arrange
        when(loanService.getLoansByBorrower(borrowerId)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<LoanResponseDTO>> response =
                loanController.getLoansByBorrower(borrowerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(loanService, times(1)).getLoansByBorrower(borrowerId);
    }

    @Test
    void testGetLoansByHousehold_Success() {
        // Arrange
        UUID householdId = UUID.randomUUID();
        List<LoanResponseDTO> loans = Arrays.asList(loanResponseDTO);
        when(loanService.getLoansByHousehold(householdId)).thenReturn(loans);

        // Act
        ResponseEntity<List<LoanResponseDTO>> response =
                loanController.getLoansByHousehold(householdId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(loanService, times(1)).getLoansByHousehold(householdId);
    }

    @Test
    void testGetLoansByStatus_Success() {
        // Arrange
        List<LoanResponseDTO> loans = Arrays.asList(loanResponseDTO);
        when(loanService.getLoansByStatus("ACTIVE", 1, 20)).thenReturn(loans);

        // Act
        ResponseEntity<List<LoanResponseDTO>> response =
                loanController.getLoansByStatus("ACTIVE", 1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(LoanStatus.ACTIVE, response.getBody().get(0).getStatus());
        verify(loanService, times(1)).getLoansByStatus("ACTIVE", 1, 20);
    }

    @Test
    void testGetLoansByStatus_EmptyList() {
        // Arrange
        when(loanService.getLoansByStatus("ACTIVE", 1, 20)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<LoanResponseDTO>> response =
                loanController.getLoansByStatus("ACTIVE", 1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(loanService, times(1)).getLoansByStatus("ACTIVE", 1, 20);
    }

    @Test
    void testCancelLoan_Success() {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("reason", "Customer request");

        doNothing().when(loanService).cancelLoan(loanId, "Customer request");

        // Act
        ResponseEntity<Void> response =
                loanController.cancelLoan(loanId, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(loanService, times(1)).cancelLoan(loanId, "Customer request");
    }
}