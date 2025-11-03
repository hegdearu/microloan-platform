package in.zeta.microloan.platform.dto.response;

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
public class RepaymentResponseDTO {
    private UUID id;
    private String receiptNumber;
    private UUID loanId;
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
    private String message;
}
