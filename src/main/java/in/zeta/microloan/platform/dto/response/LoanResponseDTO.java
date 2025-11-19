package in.zeta.microloan.platform.dto.response;

import in.zeta.microloan.platform.model.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponseDTO {
    private UUID id;
    private String loanNumber;
    private UUID borrowerId;
    private UUID householdId;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal emiAmount;
    private BigDecimal totalPayable;
    private BigDecimal totalOutstanding;
    private BigDecimal totalPaid;
    private LocalDate disbursementDate;
    private LocalDate firstDueDate;
    private LoanStatus status;
    private LocalDateTime createdAt;
}
