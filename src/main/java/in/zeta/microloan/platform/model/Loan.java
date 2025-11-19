package in.zeta.microloan.platform.model;

import in.zeta.microloan.platform.model.enums.DisbursementMethod;
import in.zeta.microloan.platform.model.enums.LoanStatus;
import in.zeta.microloan.platform.model.enums.RepaymentFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    private UUID id;
    private String loanNumber;
    private UUID applicationId;
    private UUID borrowerId;
    private UUID householdId;
    private UUID productId;
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
