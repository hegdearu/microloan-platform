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
public class LoanRestructuring {
    private Long id;
    private Long loanId;
    private String requestReason;
    private Integer oldTenureMonths;
    private Integer newTenureMonths;
    private BigDecimal oldInterestRate;
    private BigDecimal newInterestRate;
    private BigDecimal oldEmi;
    private BigDecimal newEmi;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDate effectiveDate;
    private String notes;
    private LocalDateTime createdAt;
}
