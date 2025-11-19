package in.zeta.microloan.platform.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LoanApplicationResponseDTO {
    private UUID id;
    private String applicationNumber;
    private UUID borrowerId;
    private UUID productId;
    private BigDecimal requestedAmount;
    private String purpose;
    private Integer preferredTenure;
    private String status;
    private BigDecimal approvedAmount;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}