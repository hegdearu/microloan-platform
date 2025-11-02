package in.zeta.microloan.platform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication {
    private Long id;
    private String applicationNumber;
    private Long borrowerId;
    private Long householdId;
    private Long productId;
    private BigDecimal requestedAmount;
    private String purpose;
    private Integer preferredTenure;
    private BigDecimal householdAnnualIncome;
    private BigDecimal existingHouseholdLoanTotal;
    private LoanApplicationStatus status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private BigDecimal approvedAmount;
    private String rejectionReason;
    private String notes;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
