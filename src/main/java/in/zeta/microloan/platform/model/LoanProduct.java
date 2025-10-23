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
public class LoanProduct {
    private Long id;
    private String name;
    private String description;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal interestRate;
    private String processingFeeType; // 'percentage' or 'flat'
    private BigDecimal processingFeeValue;
    private Integer tenureMonths;
    private Integer gracePeriodDays;
    private BigDecimal lateFeePercent;
    private BigDecimal maxLateFeePercent;
    private String prepaymentChargesType; // 'percentage' or 'flat' or null
    private BigDecimal prepaymentChargesValue;
    private LoanProductStatus status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
