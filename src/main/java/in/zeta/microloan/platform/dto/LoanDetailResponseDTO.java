package in.zeta.microloan.platform.dto;

import in.zeta.microloan.platform.model.LoanStatus;
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
public class LoanDetailResponseDTO {
    private Long id;
    private String loanNumber;
    private Long borrowerId;
    private String borrowerName;
    private String borrowerPhone;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal emiAmount;
    private BigDecimal totalPayable;
    private BigDecimal outstandingPrincipal;
    private BigDecimal outstandingInterest;
    private BigDecimal totalOutstanding;
    private BigDecimal totalPaid;
    private LocalDate disbursementDate;
    private LocalDate firstDueDate;
    private LocalDate lastPaymentDate;
    private LoanStatus status;
    private LocalDateTime createdAt;
}
