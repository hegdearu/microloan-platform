package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.model.RepaymentSchedule;
import in.zeta.microloan.platform.repository.repaymentschedule.RepaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RepaymentScheduleServiceTest {

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @InjectMocks
    private RepaymentScheduleService repaymentScheduleService;

    private UUID loanId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loanId = UUID.randomUUID();
    }

    @Test
    void testGenerateSchedule_Success() {
        BigDecimal principalAmount = new BigDecimal("100000");
        BigDecimal interestRate = new BigDecimal("12");
        int tenureMonths = 12;
        BigDecimal emiAmount = new BigDecimal("8884.88");
        LocalDate firstDueDate = LocalDate.now().plusMonths(1);

        // No stubbing needed if create() is void
        doNothing().when(scheduleRepository).create(any(RepaymentSchedule.class));

        repaymentScheduleService.generateSchedule(
                loanId, principalAmount, interestRate, tenureMonths, emiAmount, firstDueDate
        );

        ArgumentCaptor<RepaymentSchedule> captor = ArgumentCaptor.forClass(RepaymentSchedule.class);
        verify(scheduleRepository, times(tenureMonths)).create(captor.capture());
        List<RepaymentSchedule> created = captor.getAllValues();
        assertEquals(tenureMonths, created.size());

        // Check increasing due dates
        for (int i = 0; i < created.size(); i++) {
            assertEquals(i + 1, created.get(i).getInstallmentNumber());
            assertEquals(firstDueDate.plusMonths(i), created.get(i).getDueDate());
        }

        // Last installment principal should clear remaining principal
        BigDecimal sumPrincipal = created.stream()
                .map(RepaymentSchedule::getPrincipalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(principalAmount.setScale(2), sumPrincipal.setScale(2));
    }

    @Test
    void testGenerateSchedule_SingleMonth() {
        BigDecimal principalAmount = new BigDecimal("10000");
        BigDecimal interestRate = new BigDecimal("10");
        int tenureMonths = 1;
        BigDecimal emiAmount = new BigDecimal("10083.33");
        LocalDate firstDueDate = LocalDate.now().plusMonths(1);

        doNothing().when(scheduleRepository).create(any(RepaymentSchedule.class));

        repaymentScheduleService.generateSchedule(
                loanId, principalAmount, interestRate, tenureMonths, emiAmount, firstDueDate
        );

        ArgumentCaptor<RepaymentSchedule> captor = ArgumentCaptor.forClass(RepaymentSchedule.class);
        verify(scheduleRepository, times(1)).create(captor.capture());
        RepaymentSchedule only = captor.getValue();
        assertEquals(principalAmount.setScale(2), only.getPrincipalDue().setScale(2));
        assertEquals(firstDueDate, only.getDueDate());
    }

    @Test
    void testGenerateSchedule_MultipleMonths() {
        BigDecimal principalAmount = new BigDecimal("50000");
        BigDecimal interestRate = new BigDecimal("15");
        int tenureMonths = 6;
        BigDecimal emiAmount = new BigDecimal("8771.04");
        LocalDate firstDueDate = LocalDate.now().plusMonths(1);

        doNothing().when(scheduleRepository).create(any(RepaymentSchedule.class));

        repaymentScheduleService.generateSchedule(
                loanId, principalAmount, interestRate, tenureMonths, emiAmount, firstDueDate
        );

        ArgumentCaptor<RepaymentSchedule> captor = ArgumentCaptor.forClass(RepaymentSchedule.class);
        verify(scheduleRepository, times(6)).create(captor.capture());
        assertEquals(6, captor.getAllValues().size());
        assertEquals(firstDueDate.plusMonths(5), captor.getAllValues().get(5).getDueDate());
    }
}