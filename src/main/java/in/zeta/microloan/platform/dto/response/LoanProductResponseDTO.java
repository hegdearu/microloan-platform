package in.zeta.microloan.platform.dto.response;

import in.zeta.microloan.platform.model.enums.LoanProductStatus;
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
public class LoanProductResponseDTO {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal interestRate;
    private String processingFeeType;
    private BigDecimal processingFeeValue;
    private Integer tenureMonths;
    private Integer gracePeriodDays;
    private BigDecimal lateFeePercent;
    private BigDecimal maxLateFeePercent;
    private String prepaymentChargesType;
    private BigDecimal prepaymentChargesValue;
    private LoanProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
