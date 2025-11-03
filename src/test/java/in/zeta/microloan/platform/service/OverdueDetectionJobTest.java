package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.model.Loan;
import in.zeta.microloan.platform.model.RepaymentSchedule;
import in.zeta.microloan.platform.model.enums.InstallmentStatus;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import in.zeta.microloan.platform.repository.overduetracking.OverdueTrackingRepository;
import in.zeta.microloan.platform.repository.repaymentschedule.RepaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OverdueDetectionJobTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private OverdueTrackingRepository overdueRepository;

    @Mock
    private AtroposEventPublisherService atroposEventPublisher;

    @InjectMocks
    private OverdueDetectionJob overdueDetectionJob;

    private UUID loanId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loanId = UUID.randomUUID();
    }

    @Test
    void testDetectOverdueLoans_NoActiveLoans() {
        // Arrange
        when(loanRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList());

        // Act
        overdueDetectionJob.detectOverdueLoans();

        // Assert
        verify(loanRepository, times(1)).findByStatus("ACTIVE");
        verify(scheduleRepository, never()).findByLoanId(any());
    }

    @Test
    void testDetectOverdueLoans_NoOverdueSchedules() {
        // Arrange
        Loan loan = Loan.builder()
                .id(loanId)
                .loanNumber("LN-001")
                .gracePeriodDays(3)
                .lateFeePercent(new BigDecimal("0.5"))
                .emiAmount(new BigDecimal("5000"))
                .build();

        RepaymentSchedule schedule = RepaymentSchedule.builder()
                .id(UUID.randomUUID())
                .loanId(loanId)
                .dueDate(LocalDate.now().plusDays(10))
                .status(InstallmentStatus.PENDING)
                .build();

        when(loanRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(loan));
        when(scheduleRepository.findByLoanId(loanId)).thenReturn(Arrays.asList(schedule));

        // Act
        overdueDetectionJob.detectOverdueLoans();

        // Assert
        verify(loanRepository, times(1)).findByStatus("ACTIVE");
        verify(scheduleRepository, times(1)).findByLoanId(loanId);
        verify(overdueRepository, never()).create(any());
    }

    @Test
    void testDetectOverdueLoans_WithOverdueSchedule() {
        // Arrange
        UUID scheduleId = UUID.randomUUID();

        Loan loan = Loan.builder()
                .id(loanId)  // Uses the field loanId from setUp()
                .loanNumber("LN-001")
                .gracePeriodDays(3)
                .lateFeePercent(new BigDecimal("0.5"))
                .emiAmount(new BigDecimal("5000"))
                .build();

        RepaymentSchedule overdueSchedule = RepaymentSchedule.builder()
                .id(scheduleId)
                .loanId(loanId)  // Must match loan.getId()
                .dueDate(LocalDate.now().minusDays(10))
                .status(InstallmentStatus.PENDING)
                .principalDue(new BigDecimal("4000"))
                .principalPaid(BigDecimal.ZERO)
                .interestDue(new BigDecimal("1000"))
                .interestPaid(BigDecimal.ZERO)
                .build();

        when(loanRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(loan));
        when(scheduleRepository.findByLoanId(loanId)).thenReturn(Arrays.asList(overdueSchedule));
        when(overdueRepository.findByLoanId(loanId)).thenReturn(Optional.empty());
        doNothing().when(scheduleRepository).updateStatus(any(), any());
        doNothing().when(loanRepository).updateStatus(any(), any());
        doNothing().when(overdueRepository).create(any());
        doNothing().when(atroposEventPublisher).publishLoanOverdueEvent(any(), any());

        // Act
        overdueDetectionJob.detectOverdueLoans();

        // Assert
        verify(loanRepository, times(1)).findByStatus("ACTIVE");
        verify(scheduleRepository, times(1)).updateStatus(scheduleId, "OVERDUE");
        verify(loanRepository, times(1)).updateStatus(loanId, "OVERDUE");
        verify(overdueRepository, times(1)).create(any());
    }

    @Test
    void testDetectOverdueLoans_Exception() {
        // Arrange
        when(loanRepository.findByStatus("ACTIVE"))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        overdueDetectionJob.detectOverdueLoans();

        // Assert
        verify(loanRepository, times(1)).findByStatus("ACTIVE");
        // Job should handle exception gracefully
    }
}