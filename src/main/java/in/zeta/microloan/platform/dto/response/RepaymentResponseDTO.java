package in.zeta.microloan.platform.dto.response;

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
public class RepaymentResponseDTO {
    private Long id;
    private String receiptNumber;
    private Long loanId;
    private BigDecimal amount;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal lateFeePaid;
    private BigDecimal advancePayment;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String transactionRef;
    private String status;
    private LocalDateTime createdAt;
}
