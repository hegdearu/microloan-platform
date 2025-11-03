package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.CollectionActivityRequestDTO;
import in.zeta.microloan.platform.dto.response.CollectionActivityResponseDTO;
import in.zeta.microloan.platform.dto.response.OverdueLoansResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.*;
import in.zeta.microloan.platform.model.enums.*;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.collectionactivity.CollectionActivityRepository;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import in.zeta.microloan.platform.repository.overduetracking.OverdueTrackingRepository;
import in.zeta.microloan.platform.service.mappers.CollectionMapper;
import in.zeta.microloan.platform.service.validator.CollectionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CollectionServiceTest {

    @Mock
    private CollectionActivityRepository activityRepository;

    @Mock
    private OverdueTrackingRepository overdueRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private CollectionValidator validator;

    @Mock
    private CollectionMapper mapper;

    @InjectMocks
    private CollectionService collectionService;

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
        CollectionActivityRequestDTO dto = CollectionActivityRequestDTO.builder()
                .loanId(loanId)
                .activityType("REMINDER")
                .contactMethod("PHONE")
                .borrowerResponse("Will pay soon")
                .promiseToPayDate(LocalDate.now().plusDays(5))
                .notes("Borrower agreed")
                .build();

        Loan loan = Loan.builder()
                .id(loanId)
                .loanNumber("LN-2025-001")
                .build();

        CollectionActivity activity = CollectionActivity.builder()
                .id(activityId)
                .loanId(loanId)
                .activityType("REMINDER")
                .contactMethod(ContactMethod.PHONE)
                .borrowerResponse("Will pay soon")
                .activityDate(LocalDateTime.now())
                .build();

        CollectionActivityResponseDTO responseDTO = CollectionActivityResponseDTO.builder()
                .id(activityId)
                .loanId(loanId)
                .activityType("REMINDER")
                .build();

        doNothing().when(validator).validateActivity(any());
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(activityRepository.create(any(CollectionActivity.class))).thenReturn(activityId);
        when(mapper.toActivityResponse(any())).thenReturn(responseDTO);

        // Act
        CollectionActivityResponseDTO result = collectionService.logActivity(dto);

        // Assert
        assertNotNull(result);
        assertEquals(activityId, result.getId());
        assertEquals(loanId, result.getLoanId());
        verify(validator, times(1)).validateActivity(dto);
        verify(loanRepository, times(1)).findById(loanId);
        verify(activityRepository, times(1)).create(any(CollectionActivity.class));
    }

    @Test
    void testLogActivity_LoanNotFound() {
        // Arrange
        CollectionActivityRequestDTO dto = CollectionActivityRequestDTO.builder()
                .loanId(loanId)
                .activityType("REMINDER")
                .contactMethod("PHONE")
                .build();

        doNothing().when(validator).validateActivity(any());
        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> collectionService.logActivity(dto));
        verify(loanRepository, times(1)).findById(loanId);
        verify(activityRepository, never()).create(any());
    }

    @Test
    void testGetAllOverdueLoans_Success() {
        // Arrange
        UUID loan1Id = UUID.randomUUID();
        UUID loan2Id = UUID.randomUUID();
        UUID borrower1Id = UUID.randomUUID();
        UUID borrower2Id = UUID.randomUUID();

        OverdueTracking overdue1 = OverdueTracking.builder()
                .id(UUID.randomUUID())
                .loanId(loan1Id)
                .overdueDays(10)
                .totalDue(new BigDecimal("5100"))
                .build();

        OverdueTracking overdue2 = OverdueTracking.builder()
                .id(UUID.randomUUID())
                .loanId(loan2Id)
                .overdueDays(5)
                .totalDue(new BigDecimal("3050"))
                .build();

        Loan loan1 = Loan.builder().id(loan1Id).borrowerId(borrower1Id).loanNumber("LN-001").build();
        Loan loan2 = Loan.builder().id(loan2Id).borrowerId(borrower2Id).loanNumber("LN-002").build();

        Borrower borrower1 = Borrower.builder().id(borrower1Id).name("John").phone("9876543210").build();
        Borrower borrower2 = Borrower.builder().id(borrower2Id).name("Jane").phone("9876543211").build();

        OverdueLoansResponseDTO response1 = OverdueLoansResponseDTO.builder()
                .loanId(loan1Id)
                .loanNumber("LN-001")
                .build();

        OverdueLoansResponseDTO response2 = OverdueLoansResponseDTO.builder()
                .loanId(loan2Id)
                .loanNumber("LN-002")
                .build();

        when(overdueRepository.findAll()).thenReturn(Arrays.asList(overdue1, overdue2));
        when(loanRepository.findById(loan1Id)).thenReturn(Optional.of(loan1));
        when(loanRepository.findById(loan2Id)).thenReturn(Optional.of(loan2));
        when(borrowerRepository.findById(borrower1Id)).thenReturn(Optional.of(borrower1));
        when(borrowerRepository.findById(borrower2Id)).thenReturn(Optional.of(borrower2));
        when(mapper.toOverdueResponse(overdue1, loan1, borrower1)).thenReturn(response1);
        when(mapper.toOverdueResponse(overdue2, loan2, borrower2)).thenReturn(response2);

        // Act
        List<OverdueLoansResponseDTO> result = collectionService.getAllOverdueLoans();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(overdueRepository, times(1)).findAll();
    }

    @Test
    void testGetActivitiesByLoanId_Success() {
        // Arrange
        CollectionActivity activity1 = CollectionActivity.builder()
                .id(UUID.randomUUID())
                .loanId(loanId)
                .activityType("REMINDER")
                .build();

        CollectionActivity activity2 = CollectionActivity.builder()
                .id(UUID.randomUUID())
                .loanId(loanId)
                .activityType("FOLLOW_UP")
                .build();

        CollectionActivityResponseDTO response1 = CollectionActivityResponseDTO.builder()
                .id(activity1.getId())
                .build();

        CollectionActivityResponseDTO response2 = CollectionActivityResponseDTO.builder()
                .id(activity2.getId())
                .build();

        when(activityRepository.findByLoanId(loanId)).thenReturn(Arrays.asList(activity1, activity2));
        when(mapper.toActivityResponse(activity1)).thenReturn(response1);
        when(mapper.toActivityResponse(activity2)).thenReturn(response2);

        // Act
        List<CollectionActivityResponseDTO> result = collectionService.getActivitiesByLoanId(loanId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(activityRepository, times(1)).findByLoanId(loanId);
    }

    @Test
    void testGetActivitiesByLoanId_EmptyList() {
        // Arrange
        when(activityRepository.findByLoanId(loanId)).thenReturn(Arrays.asList());

        // Act
        List<CollectionActivityResponseDTO> result = collectionService.getActivitiesByLoanId(loanId);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(activityRepository, times(1)).findByLoanId(loanId);
    }
}