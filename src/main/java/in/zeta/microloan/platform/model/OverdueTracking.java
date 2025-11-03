package in.zeta.microloan.platform.model;

import in.zeta.microloan.platform.model.enums.CollectionStage;
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
public class OverdueTracking {
    private Long id;
    private Long loanId;
    private LocalDate overdueSince;
    private Integer overdueDays;
    private BigDecimal overduePrincipal;
    private BigDecimal overdueInterest;
    private BigDecimal overdueAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalDue;
    private LocalDateTime lastCheckedAt;
    private CollectionStage collectionStage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}