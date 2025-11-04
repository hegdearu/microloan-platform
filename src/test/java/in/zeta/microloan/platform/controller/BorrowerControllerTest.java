package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.BorrowerRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.request.BorrowerUpdateRequestDTO;
import in.zeta.microloan.platform.dto.response.BorrowerCreditSummaryResponseDTO;
import in.zeta.microloan.platform.dto.response.BorrowerResponseDTO;
import in.zeta.microloan.platform.model.enums.UserStatus;
import in.zeta.microloan.platform.service.BorrowerService;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BorrowerControllerTest {

    @Mock
    private BorrowerService borrowerService;

    @InjectMocks
    private BorrowerController borrowerController;

    private UUID borrowerId;
    private UUID householdId;
    private BorrowerResponseDTO borrowerResponseDTO;
    private BorrowerRegistrationRequestDTO registrationRequestDTO;
    private BorrowerUpdateRequestDTO updateRequestDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        borrowerId = UUID.randomUUID();
        householdId = UUID.randomUUID();

        borrowerResponseDTO = BorrowerResponseDTO.builder()
                .id(borrowerId)
                .name("Test Borrower")
                .phone("9876543210")
                .email("test@example.com")
                .dob(LocalDate.of(1990, 1, 1))
                .householdId(householdId)
                .relationshipToHead("Self")
                .isHouseholdHead(true)
                .individualAnnualIncome(new BigDecimal("500000"))
                .occupation("Software Engineer")
                .address("Test Address")
                .idProofType("AADHAAR")
                .idProofNumber("123456789012")
                .status(UserStatus.ACTIVE)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        registrationRequestDTO = BorrowerRegistrationRequestDTO.builder()
                .name("Test Borrower")
                .phone("9876543210")
                .email("test@example.com")
                .dob(LocalDate.of(1990, 1, 1))
                .householdId(householdId)
                .relationshipToHead("Self")
                .isHouseholdHead(true)
                .individualAnnualIncome(new BigDecimal("500000"))
                .occupation("Software Engineer")
                .address("Test Address")
                .idProofType("AADHAAR")
                .idProofNumber("123456789012")
                .build();

        updateRequestDTO = BorrowerUpdateRequestDTO.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .address("Updated Address")
                .occupation("Senior Engineer")
                .individualAnnualIncome(new BigDecimal("600000"))
                .build();
    }

    @Test
    void testRegisterBorrower_Success() {
        // Arrange
        when(borrowerService.registerBorrower(any(BorrowerRegistrationRequestDTO.class)))
                .thenReturn(borrowerResponseDTO);

        // Act
        ResponseEntity<BorrowerResponseDTO> response =
                borrowerController.registerBorrower(registrationRequestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(borrowerId, response.getBody().getId());
        assertEquals("Test Borrower", response.getBody().getName());
        assertEquals("9876543210", response.getBody().getPhone());
        verify(borrowerService, times(1)).registerBorrower(any(BorrowerRegistrationRequestDTO.class));
    }

    @Test
    void testGetBorrowerDetails_Success() {
        // Arrange
        when(borrowerService.getBorrowerById(borrowerId)).thenReturn(borrowerResponseDTO);

        // Act
        ResponseEntity<BorrowerResponseDTO> response =
                borrowerController.getBorrowerDetails(borrowerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(borrowerId, response.getBody().getId());
        assertEquals("Test Borrower", response.getBody().getName());
        verify(borrowerService, times(1)).getBorrowerById(borrowerId);
    }

    @Test
    void testGetBorrowerByPhone_Success() {
        // Arrange
        String phone = "9876543210";
        when(borrowerService.getBorrowerByPhone(phone)).thenReturn(borrowerResponseDTO);

        // Act
        ResponseEntity<BorrowerResponseDTO> response =
                borrowerController.getBorrowerByPhone(phone);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(phone, response.getBody().getPhone());
        verify(borrowerService, times(1)).getBorrowerByPhone(phone);
    }

    @Test
    void testGetBorrowersByHousehold_Success() {
        // Arrange
        List<BorrowerResponseDTO> borrowers = Arrays.asList(borrowerResponseDTO);
        when(borrowerService.getBorrowersByHousehold(householdId)).thenReturn(borrowers);

        // Act
        ResponseEntity<List<BorrowerResponseDTO>> response =
                borrowerController.getBorrowersByHousehold(householdId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(householdId, response.getBody().get(0).getHouseholdId());
        verify(borrowerService, times(1)).getBorrowersByHousehold(householdId);
    }

    @Test
    void testGetAllBorrowers_WithoutStatus_Success() {
        // Arrange
        List<BorrowerResponseDTO> borrowers = Arrays.asList(borrowerResponseDTO);
        when(borrowerService.getAllBorrowers(null, 1, 20)).thenReturn(borrowers);

        // Act
        ResponseEntity<List<BorrowerResponseDTO>> response =
                borrowerController.getAllBorrowers(null, 1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(borrowerService, times(1)).getAllBorrowers(null, 1, 20);
    }

    @Test
    void testGetAllBorrowers_WithStatus_Success() {
        // Arrange
        List<BorrowerResponseDTO> borrowers = Arrays.asList(borrowerResponseDTO);
        when(borrowerService.getAllBorrowers("ACTIVE", 1, 20)).thenReturn(borrowers);

        // Act
        ResponseEntity<List<BorrowerResponseDTO>> response =
                borrowerController.getAllBorrowers("ACTIVE", 1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(UserStatus.ACTIVE, response.getBody().get(0).getStatus());
        verify(borrowerService, times(1)).getAllBorrowers("ACTIVE", 1, 20);
    }

    @Test
    void testGetAllBorrowers_EmptyList() {
        // Arrange
        when(borrowerService.getAllBorrowers(null, 1, 20)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<BorrowerResponseDTO>> response =
                borrowerController.getAllBorrowers(null, 1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(borrowerService, times(1)).getAllBorrowers(null, 1, 20);
    }

    @Test
    void testUpdateBorrowerDetails_Success() {
        // Arrange
        when(borrowerService.updateBorrower(eq(borrowerId), any(BorrowerUpdateRequestDTO.class)))
                .thenReturn(borrowerResponseDTO);

        // Act
        ResponseEntity<BorrowerResponseDTO> response =
                borrowerController.updateBorrowerDetails(borrowerId, updateRequestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(borrowerId, response.getBody().getId());
        verify(borrowerService, times(1)).updateBorrower(eq(borrowerId), any(BorrowerUpdateRequestDTO.class));
    }

    @Test
    void testVerifyBorrower_Success() {
        // Arrange
        borrowerResponseDTO.setIsVerified(true);
        when(borrowerService.verifyBorrower(borrowerId)).thenReturn(borrowerResponseDTO);

        // Act
        ResponseEntity<BorrowerResponseDTO> response =
                borrowerController.verifyBorrower(borrowerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getIsVerified());
        verify(borrowerService, times(1)).verifyBorrower(borrowerId);
    }

    @Test
    void testUpdateBorrowerStatus_Success() {
        // Arrange
        when(borrowerService.updateBorrowerStatus(borrowerId, "SUSPENDED"))
                .thenReturn(borrowerResponseDTO);

        // Act
        ResponseEntity<BorrowerResponseDTO> response =
                borrowerController.updateBorrowerStatus(borrowerId, "SUSPENDED");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(borrowerId, response.getBody().getId());
        verify(borrowerService, times(1)).updateBorrowerStatus(borrowerId, "SUSPENDED");
    }

    @Test
    void testGetBorrowerCreditSummary_Success() {
        // Arrange
        BorrowerCreditSummaryResponseDTO summaryDTO = BorrowerCreditSummaryResponseDTO.builder()
                .borrowerId(borrowerId)
                .borrowerName("Test Borrower")
                .totalLoans(5)
                .activeLoans(2)
                .closedLoans(3)
                .totalDisbursed(new BigDecimal("500000"))
                .totalOutstanding(new BigDecimal("200000"))
                .totalPaid(new BigDecimal("300000"))
                .isVerified(true)
                .status("ACTIVE")
                .build();

        when(borrowerService.getBorrowerCreditSummary(borrowerId)).thenReturn(summaryDTO);

        // Act
        ResponseEntity<BorrowerCreditSummaryResponseDTO> response =
                borrowerController.getBorrowerCreditSummary(borrowerId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(borrowerId, response.getBody().getBorrowerId());
        assertEquals(5, response.getBody().getTotalLoans());
        assertEquals(2, response.getBody().getActiveLoans());
        verify(borrowerService, times(1)).getBorrowerCreditSummary(borrowerId);
    }
}