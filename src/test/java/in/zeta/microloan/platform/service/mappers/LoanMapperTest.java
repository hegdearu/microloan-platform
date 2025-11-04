package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.LoanDetailResponseDTO;
import in.zeta.microloan.platform.dto.response.LoanResponseDTO;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.Loan;
import in.zeta.microloan.platform.model.enums.LoanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoanMapperTest {

    private LoanMapper mapper;
    private Loan loan;
    private Borrower borrower;

    @BeforeEach
    void setUp() {
        mapper = new LoanMapper();

        loan = Loan.builder()
                .id(UUID.randomUUID())
                .loanNumber("LN-2024-123456")
                .borrowerId(UUID.randomUUID())
                .householdId(UUID.randomUUID())
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
                .lastPaymentDate(null)
                .status(LoanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        borrower = Borrower.builder()
                .id(loan.getBorrowerId())
                .name("Test Borrower")
                .phone("9876543210")
                .build();
    }

    @Test
    void toResponse_ShouldMapAllFields() {
        LoanResponseDTO result = mapper.toResponse(loan);

        assertNotNull(result);
        assertEquals(loan.getId(), result.getId());
        assertEquals(loan.getLoanNumber(), result.getLoanNumber());
        assertEquals(loan.getBorrowerId(), result.getBorrowerId());
        assertEquals(loan.getHouseholdId(), result.getHouseholdId());
        assertEquals(loan.getPrincipalAmount(), result.getPrincipalAmount());
        assertEquals(loan.getInterestRate(), result.getInterestRate());
        assertEquals(loan.getTenureMonths(), result.getTenureMonths());
        assertEquals(loan.getEmiAmount(), result.getEmiAmount());
        assertEquals(loan.getTotalPayable(), result.getTotalPayable());
        assertEquals(loan.getTotalOutstanding(), result.getTotalOutstanding());
        assertEquals(loan.getTotalPaid(), result.getTotalPaid());
        assertEquals(loan.getDisbursementDate(), result.getDisbursementDate());
        assertEquals(loan.getFirstDueDate(), result.getFirstDueDate());
        assertEquals(loan.getStatus(), result.getStatus());
        assertEquals(loan.getCreatedAt(), result.getCreatedAt());
    }

    @Test
    void toDetail_ShouldMapAllFields() {
        LoanDetailResponseDTO result = mapper.toDetail(loan, borrower);

        assertNotNull(result);
        assertEquals(loan.getId(), result.getId());
        assertEquals(loan.getLoanNumber(), result.getLoanNumber());
        assertEquals(borrower.getId(), result.getBorrowerId());
        assertEquals(borrower.getName(), result.getBorrowerName());
        assertEquals(borrower.getPhone(), result.getBorrowerPhone());
        assertEquals(loan.getPrincipalAmount(), result.getPrincipalAmount());
        assertEquals(loan.getInterestRate(), result.getInterestRate());
        assertEquals(loan.getTenureMonths(), result.getTenureMonths());
        assertEquals(loan.getEmiAmount(), result.getEmiAmount());
        assertEquals(loan.getTotalPayable(), result.getTotalPayable());
        assertEquals(loan.getOutstandingPrincipal(), result.getOutstandingPrincipal());
        assertEquals(loan.getOutstandingInterest(), result.getOutstandingInterest());
        assertEquals(loan.getTotalOutstanding(), result.getTotalOutstanding());
        assertEquals(loan.getTotalPaid(), result.getTotalPaid());
        assertEquals(loan.getDisbursementDate(), result.getDisbursementDate());
        assertEquals(loan.getFirstDueDate(), result.getFirstDueDate());
        assertEquals(loan.getLastPaymentDate(), result.getLastPaymentDate());
        assertEquals(loan.getStatus(), result.getStatus());
        assertEquals(loan.getCreatedAt(), result.getCreatedAt());
    }
}
