package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.RepaymentRequestDTO;
import in.zeta.microloan.platform.dto.response.RepaymentResponseDTO;
import in.zeta.microloan.platform.dto.response.RepaymentScheduleResponseDTO;
import in.zeta.microloan.platform.service.RepaymentService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RepaymentControllerTest {

    @Mock
    private RepaymentService repaymentService;

    @InjectMocks
    private RepaymentController repaymentController;

    private UUID loanId;
    private UUID repaymentId;
    private Long createdBy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loanId = UUID.randomUUID();
        repaymentId = UUID.randomUUID();
        createdBy = 1001L;
    }

    @Test
    void testRecordRepayment_Success() {
        // Arrange
        RepaymentRequestDTO requestDTO = RepaymentRequestDTO.builder()
                .loanId(loanId)
                .amount(new BigDecimal("5000"))
                .paymentDate(LocalDate.now())
                .paymentMethod("CASH")
                .transactionRef("TXN123456")
                .notes("Monthly EMI payment")
                .build();

        RepaymentResponseDTO responseDTO = RepaymentResponseDTO.builder()
                .id(repaymentId)
                .receiptNumber("RCP-20251104-001234")
                .loanId(loanId)
                .amount(new BigDecimal("5000"))
                .principalPaid(new BigDecimal("4000"))
                .interestPaid(new BigDecimal("1000"))
                .lateFeePaid(BigDecimal.ZERO)
                .advancePayment(BigDecimal.ZERO)
                .paymentDate(LocalDate.now())
                .paymentMethod("CASH")
                .transactionRef("TXN123456")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .message(null)
                .build();

        when(repaymentService.recordRepayment(any(RepaymentRequestDTO.class), eq(createdBy)))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<RepaymentResponseDTO> response =
                repaymentController.recordRepayment(requestDTO, createdBy);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(repaymentId, response.getBody().getId());
        assertEquals(loanId, response.getBody().getLoanId());
        assertEquals(new BigDecimal("5000"), response.getBody().getAmount());
        verify(repaymentService, times(1)).recordRepayment(any(RepaymentRequestDTO.class), eq(createdBy));
    }

    @Test
    void testGetRepaymentsByLoan_Success() {
        // Arrange
        RepaymentResponseDTO repayment1 = RepaymentResponseDTO.builder()
                .id(UUID.randomUUID())
                .receiptNumber("RCP-001")
                .loanId(loanId)
                .amount(new BigDecimal("5000"))
                .paymentDate(LocalDate.now().minusMonths(2))
                .status("COMPLETED")
                .build();

        RepaymentResponseDTO repayment2 = RepaymentResponseDTO.builder()
                .id(UUID.randomUUID())
                .receiptNumber("RCP-002")
                .loanId(loanId)
                .amount(new BigDecimal("5000"))
                .paymentDate(LocalDate.now().minusMonths(1))
                .status("COMPLETED")
                .build();

        List<RepaymentResponseDTO> repayments = Arrays.asList(repayment1, repayment2);

        when(repaymentService.getRepaymentsByLoan(loanId)).thenReturn(repayments);

        // Act
        ResponseEntity<List<RepaymentResponseDTO>> response =
                repaymentController.getRepaymentsByLoan(loanId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("RCP-001", response.getBody().get(0).getReceiptNumber());
        verify(repaymentService, times(1)).getRepaymentsByLoan(loanId);
    }

    @Test
    void testGetRepaymentsByLoan_EmptyList() {
        // Arrange
        when(repaymentService.getRepaymentsByLoan(loanId)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<RepaymentResponseDTO>> response =
                repaymentController.getRepaymentsByLoan(loanId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(repaymentService, times(1)).getRepaymentsByLoan(loanId);
    }

    @Test
    void testGetRepaymentSchedule_Success() {
        // Arrange
        RepaymentScheduleResponseDTO schedule1 = RepaymentScheduleResponseDTO.builder()
                .id(UUID.randomUUID())
                .installmentNumber(1)
                .dueDate(LocalDate.now().plusMonths(1))
                .principalDue(new BigDecimal("4000"))
                .interestDue(new BigDecimal("1000"))
                .totalDue(new BigDecimal("5000"))
                .status("PENDING")
                .build();

        RepaymentScheduleResponseDTO schedule2 = RepaymentScheduleResponseDTO.builder()
                .id(UUID.randomUUID())
                .installmentNumber(2)
                .dueDate(LocalDate.now().plusMonths(2))
                .principalDue(new BigDecimal("4100"))
                .interestDue(new BigDecimal("900"))
                .totalDue(new BigDecimal("5000"))
                .status("PENDING")
                .build();

        List<RepaymentScheduleResponseDTO> schedules = Arrays.asList(schedule1, schedule2);

        when(repaymentService.getRepaymentSchedule(loanId)).thenReturn(schedules);

        // Act
        ResponseEntity<List<RepaymentScheduleResponseDTO>> response =
                repaymentController.getRepaymentSchedule(loanId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(1, response.getBody().get(0).getInstallmentNumber());
        assertEquals(2, response.getBody().get(1).getInstallmentNumber());
        verify(repaymentService, times(1)).getRepaymentSchedule(loanId);
    }

    @Test
    void testGetPendingSchedule_Success() {
        // Arrange
        RepaymentScheduleResponseDTO pending1 = RepaymentScheduleResponseDTO.builder()
                .id(UUID.randomUUID())
                .installmentNumber(3)
                .dueDate(LocalDate.now().plusMonths(1))
                .principalDue(new BigDecimal("4200"))
                .interestDue(new BigDecimal("800"))
                .totalDue(new BigDecimal("5000"))
                .status("PENDING")
                .build();

        List<RepaymentScheduleResponseDTO> pendingSchedules = Arrays.asList(pending1);

        when(repaymentService.getPendingSchedule(loanId)).thenReturn(pendingSchedules);

        // Act
        ResponseEntity<List<RepaymentScheduleResponseDTO>> response =
                repaymentController.getPendingSchedule(loanId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("PENDING", response.getBody().get(0).getStatus());
        verify(repaymentService, times(1)).getPendingSchedule(loanId);
    }

    @Test
    void testGetPendingSchedule_EmptyList() {
        // Arrange
        when(repaymentService.getPendingSchedule(loanId)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<RepaymentScheduleResponseDTO>> response =
                repaymentController.getPendingSchedule(loanId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(repaymentService, times(1)).getPendingSchedule(loanId);
    }
}