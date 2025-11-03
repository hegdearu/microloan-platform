package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.CollectionActivityRequestDTO;
import in.zeta.microloan.platform.dto.response.CollectionActivityResponseDTO;
import in.zeta.microloan.platform.dto.response.OverdueLoansResponseDTO;
import in.zeta.microloan.platform.model.enums.ContactMethod;
import in.zeta.microloan.platform.model.enums.CollectionStage;
import in.zeta.microloan.platform.service.CollectionService;
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
import static org.mockito.Mockito.*;

class CollectionControllerTest {

    @Mock
    private CollectionService collectionService;

    @InjectMocks
    private CollectionController collectionController;

    private UUID loanId;
    private UUID activityId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loanId = UUID.randomUUID();
        activityId = UUID.randomUUID();
    }

    @Test
    void testLogActivity_Success() {
        // Arrange
        CollectionActivityRequestDTO requestDTO = CollectionActivityRequestDTO.builder()
                .loanId(loanId)
                .activityType("REMINDER")
                .contactMethod("PHONE")
                .borrowerResponse("Will pay soon")
                .promiseToPayDate(LocalDate.now().plusDays(5))
                .notes("Borrower agreed to pay")
                .build();

        CollectionActivityResponseDTO responseDTO = CollectionActivityResponseDTO.builder()
                .id(activityId)
                .loanId(loanId)
                .activityType("REMINDER")
                .contactMethod(ContactMethod.PHONE)
                .borrowerResponse("Will pay soon")
                .activityDate(LocalDateTime.now())
                .build();

        when(collectionService.logActivity(any(CollectionActivityRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<CollectionActivityResponseDTO> response = collectionController.logActivity(requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(activityId, response.getBody().getId());
        assertEquals(loanId, response.getBody().getLoanId());
        verify(collectionService, times(1)).logActivity(any(CollectionActivityRequestDTO.class));
    }

    @Test
    void testGetActivities_Success() {
        // Arrange
        CollectionActivityResponseDTO activity1 = CollectionActivityResponseDTO.builder()
                .id(UUID.randomUUID())
                .loanId(loanId)
                .activityType("REMINDER")
                .contactMethod(ContactMethod.PHONE)
                .activityDate(LocalDateTime.now())
                .build();

        CollectionActivityResponseDTO activity2 = CollectionActivityResponseDTO.builder()
                .id(UUID.randomUUID())
                .loanId(loanId)
                .activityType("FOLLOW_UP")
                .contactMethod(ContactMethod.SMS)
                .activityDate(LocalDateTime.now())
                .build();

        List<CollectionActivityResponseDTO> activities = Arrays.asList(activity1, activity2);

        when(collectionService.getActivitiesByLoanId(loanId)).thenReturn(activities);

        // Act
        ResponseEntity<List<CollectionActivityResponseDTO>> response =
                collectionController.getActivities(loanId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(collectionService, times(1)).getActivitiesByLoanId(loanId);
    }

    @Test
    void testGetActivities_EmptyList() {
        // Arrange
        when(collectionService.getActivitiesByLoanId(loanId)).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<CollectionActivityResponseDTO>> response =
                collectionController.getActivities(loanId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(collectionService, times(1)).getActivitiesByLoanId(loanId);
    }

    @Test
    void testGetOverdueLoans_Success() {
        // Arrange
        OverdueLoansResponseDTO overdue1 = OverdueLoansResponseDTO.builder()
                .loanId(UUID.randomUUID())
                .loanNumber("LN-2025-001")
                .borrowerId(UUID.randomUUID())
                .borrowerName("John Doe")
                .borrowerPhone("9876543210")
                .overdueSince(LocalDate.now().minusDays(10))
                .overdueDays(10)
                .overdueAmount(new BigDecimal("5000"))
                .penaltyAmount(new BigDecimal("100"))
                .totalDue(new BigDecimal("5100"))
                .collectionStage(CollectionStage.REGULAR_FOLLOWUP)
                .build();

        OverdueLoansResponseDTO overdue2 = OverdueLoansResponseDTO.builder()
                .loanId(UUID.randomUUID())
                .loanNumber("LN-2025-002")
                .borrowerId(UUID.randomUUID())
                .borrowerName("Jane Smith")
                .borrowerPhone("9876543211")
                .overdueSince(LocalDate.now().minusDays(5))
                .overdueDays(5)
                .overdueAmount(new BigDecimal("3000"))
                .penaltyAmount(new BigDecimal("50"))
                .totalDue(new BigDecimal("3050"))
                .collectionStage(CollectionStage.SOFT_REMINDER)
                .build();

        List<OverdueLoansResponseDTO> overdueLoans = Arrays.asList(overdue1, overdue2);

        when(collectionService.getAllOverdueLoans()).thenReturn(overdueLoans);

        // Act
        ResponseEntity<List<OverdueLoansResponseDTO>> response =
                collectionController.getOverdueLoans();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("LN-2025-001", response.getBody().get(0).getLoanNumber());
        assertEquals(10, response.getBody().get(0).getOverdueDays());
        verify(collectionService, times(1)).getAllOverdueLoans();
    }

    @Test
    void testGetOverdueLoans_EmptyList() {
        // Arrange
        when(collectionService.getAllOverdueLoans()).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<OverdueLoansResponseDTO>> response =
                collectionController.getOverdueLoans();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(collectionService, times(1)).getAllOverdueLoans();
    }
}