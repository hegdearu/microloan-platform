package in.zeta.microloan.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerCreditSummaryDTO {
    private Long borrowerId;
    private String borrowerName;
    private Integer totalLoans;
    private Integer activeLoans;
    private Integer closedLoans;
    private BigDecimal totalDisbursed;
    private BigDecimal totalOutstanding;
    private BigDecimal totalPaid;
    private Integer creditScore;
    private Boolean isVerified;
    private String status;
}
