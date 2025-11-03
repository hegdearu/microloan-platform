package in.zeta.microloan.platform.dto.response;

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
    private Long productId;
    private BigDecimal requestedAmount;
    private String purpose;
    private Integer preferredTenure;
    private String status;
    private BigDecimal approvedAmount;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}