package in.zeta.microloan.platform.model;

import in.zeta.microloan.platform.model.enums.LoanApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication {
    private UUID id;
    private String applicationNumber;
    private UUID borrowerId;
    private UUID householdId;
    private UUID productId;
    private BigDecimal requestedAmount;
    private String purpose;
    private Integer preferredTenure;
    private BigDecimal householdAnnualIncome;
    private BigDecimal existingHouseholdLoanTotal;
    private LoanApplicationStatus status;
    private LocalDateTime approvedAt;
    private BigDecimal approvedAmount;
    private String rejectionReason;
    private String notes;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
