package in.zeta.microloan.platform.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApproveLoanApplicationRequestDTO {
    private Long approvedBy;
    private BigDecimal approvedAmount;
}
