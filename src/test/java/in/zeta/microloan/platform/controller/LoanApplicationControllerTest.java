package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.LoanApplicationRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanApplicationResponseDTO;
import in.zeta.microloan.platform.service.LoanApplicationService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoanApplicationControllerTest {

    @Mock
    private LoanApplicationService applicationService;

    @InjectMocks
    private LoanApplicationController loanApplicationController;

    private UUID applicationId;
    private UUID borrowerId;
    private UUID productId;
    private LoanApplicationResponseDTO responseDTO;
    private LoanApplicationRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        applicationId = UUID.randomUUID();
        borrowerId = UUID.randomUUID();
        productId = UUID.randomUUID();

        responseDTO = LoanApplicationResponseDTO.builder()
                .id(applicationId)
                .applicationNumber("APP-20240101-123456")
                .borrowerId(borrowerId)
                .productId(productId)
                .requestedAmount(new BigDecimal("100000"))
                .purpose("Business")
                .preferredTenure(12)
                .status("PENDING_REVIEW")
                .approvedAmount(null)
                .approvedAt(null)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestDTO = LoanApplicationRequestDTO.builder()
                .borrowerId(borrowerId)
                .productId(productId)
                .requestedAmount(new BigDecimal("100000"))
                .purpose("Business")
                .preferredTenure(12)
                .build();
    }

    @Test
    void testCreateApplication_Success() {
        // Arrange
        when(applicationService.createApplication(any(LoanApplicationRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<LoanApplicationResponseDTO> response =
                loanApplicationController.createApplication(requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(applicationId, response.getBody().getId());
        assertEquals("APP-20240101-123456", response.getBody().getApplicationNumber());
        verify(applicationService, times(1)).createApplication(any(LoanApplicationRequestDTO.class));
    }

    @Test
    void testGetApplications_WithBorrowerId_Success() {
        // Arrange
        List<LoanApplicationResponseDTO> applications = Arrays.asList(responseDTO);
        when(applicationService.getApplicationsByBorrower(borrowerId)).thenReturn(applications);

        // Act
        ResponseEntity<List<LoanApplicationResponseDTO>> response =
                loanApplicationController.getApplications(borrowerId, null, 1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(borrowerId, response.getBody().get(0).getBorrowerId());
        verify(applicationService, times(1)).getApplicationsByBorrower(borrowerId);
    }

    @Test
    void testGetApplications_WithStatus_Success() {
        // Arrange
        List<LoanApplicationResponseDTO> applications = Arrays.asList(responseDTO);
        when(applicationService.getApplicationsByStatus("PENDING_REVIEW", 1, 20))
                .thenReturn(applications);

        // Act
        ResponseEntity<List<LoanApplicationResponseDTO>> response =
                loanApplicationController.getApplications(null, "PENDING_REVIEW", 1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("PENDING_REVIEW", response.getBody().get(0).getStatus());
        verify(applicationService, times(1)).getApplicationsByStatus("PENDING_REVIEW", 1, 20);
    }

    @Test
    void testGetApplications_WithoutFilters_Success() {
        // Arrange
        List<LoanApplicationResponseDTO> applications = Arrays.asList(responseDTO);
        when(applicationService.getAllApplications(1, 20)).thenReturn(applications);

        // Act
        ResponseEntity<List<LoanApplicationResponseDTO>> response =
                loanApplicationController.getApplications(null, null, 1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(applicationService, times(1)).getAllApplications(1, 20);
    }

    @Test
    void testGetApplications_EmptyList() {
        // Arrange
        when(applicationService.getAllApplications(1, 20)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<LoanApplicationResponseDTO>> response =
                loanApplicationController.getApplications(null, null, 1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(applicationService, times(1)).getAllApplications(1, 20);
    }

    @Test
    void testGetApplication_Success() {
        // Arrange
        when(applicationService.getApplicationById(applicationId)).thenReturn(responseDTO);

        // Act
        ResponseEntity<LoanApplicationResponseDTO> response =
                loanApplicationController.getApplication(applicationId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(applicationId, response.getBody().getId());
        verify(applicationService, times(1)).getApplicationById(applicationId);
    }

    @Test
    void testApproveApplication_Success() {
        // Arrange
        BigDecimal approvedAmount = new BigDecimal("95000");
        responseDTO.setStatus("APPROVED");
        responseDTO.setApprovedAmount(approvedAmount);
        responseDTO.setApprovedAt(LocalDateTime.now());

        when(applicationService.approveApplication(eq(applicationId), eq(approvedAmount)))
                .thenReturn(responseDTO);

        Map<String, Object> request = new HashMap<>();
        request.put("approvedAmount", "95000");

        // Act
        ResponseEntity<LoanApplicationResponseDTO> response =
                loanApplicationController.approveApplication(applicationId, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("APPROVED", response.getBody().getStatus());
        assertEquals(approvedAmount, response.getBody().getApprovedAmount());
        verify(applicationService, times(1)).approveApplication(eq(applicationId), any(BigDecimal.class));
    }

    @Test
    void testRejectApplication_Success() {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("rejectionReason", "Insufficient documentation");

        doNothing().when(applicationService).rejectApplication(applicationId, "Insufficient documentation");

        // Act
        ResponseEntity<Void> response =
                loanApplicationController.rejectApplication(applicationId, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(applicationService, times(1)).rejectApplication(applicationId, "Insufficient documentation");
    }

    @Test
    void testCancelApplication_Success() {
        // Arrange
        doNothing().when(applicationService).cancelApplication(applicationId);

        // Act
        ResponseEntity<Void> response =
                loanApplicationController.cancelApplication(applicationId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(applicationService, times(1)).cancelApplication(applicationId);
    }

    @Test
    void testGetPendingApplications_Success() {
        // Arrange
        List<LoanApplicationResponseDTO> applications = Arrays.asList(responseDTO);
        when(applicationService.getPendingApplications(1, 20)).thenReturn(applications);

        // Act
        ResponseEntity<List<LoanApplicationResponseDTO>> response =
                loanApplicationController.getPendingApplications(1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("PENDING_REVIEW", response.getBody().get(0).getStatus());
        verify(applicationService, times(1)).getPendingApplications(1, 20);
    }

    @Test
    void testGetPendingApplications_EmptyList() {
        // Arrange
        when(applicationService.getPendingApplications(1, 20)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<LoanApplicationResponseDTO>> response =
                loanApplicationController.getPendingApplications(1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(applicationService, times(1)).getPendingApplications(1, 20);
    }

    @Test
    void testGetExpiredApplications_Success() {
        // Arrange
        List<LoanApplicationResponseDTO> applications = Arrays.asList(responseDTO);
        when(applicationService.getExpiredApplications(1, 20)).thenReturn(applications);

        // Act
        ResponseEntity<List<LoanApplicationResponseDTO>> response =
                loanApplicationController.getExpiredApplications(1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(applicationService, times(1)).getExpiredApplications(1, 20);
    }

    @Test
    void testGetExpiredApplications_EmptyList() {
        // Arrange
        when(applicationService.getExpiredApplications(1, 20)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<LoanApplicationResponseDTO>> response =
                loanApplicationController.getExpiredApplications(1, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(applicationService, times(1)).getExpiredApplications(1, 20);
    }
}