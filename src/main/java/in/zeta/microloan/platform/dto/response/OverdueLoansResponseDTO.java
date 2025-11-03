package in.zeta.microloan.platform.dto.response;

import in.zeta.microloan.platform.model.enums.CollectionStage;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class OverdueLoansResponseDTO {
    private Long loanId;
    private String loanNumber;
    private Long borrowerId;
    private String borrowerName;
    private String borrowerPhone;
    private LocalDate overdueSince;
    private Integer overdueDays;
    private BigDecimal overdueAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalDue;
    private CollectionStage collectionStage;
}
