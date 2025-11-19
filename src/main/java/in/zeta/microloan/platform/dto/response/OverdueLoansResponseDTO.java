package in.zeta.microloan.platform.dto.response;

import in.zeta.microloan.platform.model.enums.CollectionStage;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class OverdueLoansResponseDTO {
    private UUID loanId;
    private String loanNumber;
    private UUID borrowerId;
    private String borrowerName;
    private String borrowerPhone;
    private LocalDate overdueSince;
    private Integer overdueDays;
    private BigDecimal overdueAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalDue;
    private CollectionStage collectionStage;
}
