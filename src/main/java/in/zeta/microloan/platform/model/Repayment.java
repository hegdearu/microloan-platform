package in.zeta.microloan.platform.model;

import in.zeta.microloan.platform.model.enums.PaymentMethod;
import in.zeta.microloan.platform.model.enums.PaymentStatus;
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
public class Repayment {
    private Long id;
    private String receiptNumber;
    private Long loanId;
    private Long borrowerId;
    private Long householdId;
    private BigDecimal amount;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal lateFeePaid;
    private BigDecimal advancePayment;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private String transactionRef;
    private String notes;
    private PaymentStatus status;
    private String receiptUrl;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime voidedAt;
    private Long voidedBy;
    private String voidReason;
}
