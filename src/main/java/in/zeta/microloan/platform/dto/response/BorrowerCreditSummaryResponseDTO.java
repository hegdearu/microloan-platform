package in.zeta.microloan.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerCreditSummaryResponseDTO {
    private UUID borrowerId;
    private String borrowerName;
    private Integer totalLoans;
    private Integer activeLoans;
    private Integer closedLoans;
    private BigDecimal totalDisbursed;
    private BigDecimal totalOutstanding;
    private BigDecimal totalPaid;
    private Boolean isVerified;
    private String status;
}
