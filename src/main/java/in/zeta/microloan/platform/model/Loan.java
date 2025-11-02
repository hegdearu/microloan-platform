package in.zeta.microloan.platform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    private Long id;
    private String loanNumber;
    private Long applicationId;
    private Long borrowerId;
    private Long householdId;
    private Long productId;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private BigDecimal processingFee;
    private Integer tenureMonths;
    private RepaymentFrequency repaymentFrequency;
    private BigDecimal emiAmount;
    private BigDecimal totalPayable;
    private BigDecimal outstandingPrincipal;
    private BigDecimal outstandingInterest;
    private BigDecimal totalOutstanding;
    private BigDecimal totalPaid;
    private LocalDate disbursementDate;
    private DisbursementMethod disbursementMethod;
    private LocalDate firstDueDate;
    private LocalDate lastPaymentDate;
    private LoanStatus status;
    private LocalDate closedDate;
    private Integer gracePeriodDays;
    private BigDecimal lateFeePercent;
    private String agreementUrl;
    private BigDecimal householdIncomeAtApproval;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
