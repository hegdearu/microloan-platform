package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.RepaymentRequestDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.model.Loan;
import in.zeta.microloan.platform.model.RepaymentSchedule;
import in.zeta.microloan.platform.model.enums.LoanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RepaymentValidatorTest {

    private RepaymentValidator validator;
    private RepaymentRequestDTO requestDTO;
    private Loan loan;
    private List<RepaymentSchedule> pendingSchedules;

    @BeforeEach
    void setUp() {
        validator = new RepaymentValidator();

        requestDTO = RepaymentRequestDTO.builder()
                .loanId(UUID.randomUUID())
                .amount(new BigDecimal("10000"))
                .paymentDate(LocalDate.now())
                .paymentMethod("CASH")
                .transactionRef("TXN123456")
                .build();

        loan = Loan.builder()
                .id(UUID.randomUUID())
                .status(LoanStatus.ACTIVE)
                .build();

        RepaymentSchedule schedule = RepaymentSchedule.builder()
                .id(UUID.randomUUID())
                .build();
        pendingSchedules = Arrays.asList(schedule);
    }

    @Test
    void validateRecord_WithValidData_ShouldPass() {
        assertDoesNotThrow(() ->
                validator.validateRecord(requestDTO, loan, pendingSchedules)
        );
    }

    @Test
    void validateRecord_WithInactiveLoan_ShouldThrowException() {
        loan.setStatus(LoanStatus.CANCELLED);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateRecord(requestDTO, loan, pendingSchedules)
        );

        assertTrue(exception.getMessage().contains("active or overdue"));
    }

    @Test
    void validateRecord_WithNoPendingSchedules_ShouldThrowException() {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateRecord(requestDTO, loan, Collections.emptyList())
        );

        assertTrue(exception.getMessage().contains("No pending installments"));
    }

    @Test
    void validateRecord_WithZeroAmount_ShouldThrowException() {
        requestDTO.setAmount(BigDecimal.ZERO);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateRecord(requestDTO, loan, pendingSchedules)
        );

        assertTrue(exception.getMessage().contains("must be positive"));
    }

    @Test
    void validateRecord_WithNegativeAmount_ShouldThrowException() {
        requestDTO.setAmount(new BigDecimal("-1000"));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateRecord(requestDTO, loan, pendingSchedules)
        );

        assertTrue(exception.getMessage().contains("must be positive"));
    }

    @Test
    void validateRecord_WithOverdueLoan_ShouldPass() {
        loan.setStatus(LoanStatus.OVERDUE);

        assertDoesNotThrow(() ->
                validator.validateRecord(requestDTO, loan, pendingSchedules)
        );
    }
}