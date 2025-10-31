package in.zeta.microloan.platform.dto;

import in.zeta.microloan.platform.model.LoanApplicationStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanApplicationResponseDTO {
    private Long id;
    private String applicationNumber;
    private Long borrowerId;
    private Long householdId;
    private BigDecimal requestedAmount;
    private String purpose;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}