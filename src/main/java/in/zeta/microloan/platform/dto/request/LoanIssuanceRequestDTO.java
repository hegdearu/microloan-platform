package in.zeta.microloan.platform.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanIssuanceRequestDTO {
    private UUID applicationId;

    @NotNull(message = "Borrower ID is required")
    private UUID borrowerId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal principalAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Interest rate must be greater than 0")
    private BigDecimal interestRate;

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Tenure must be at least 1 month")
    private Integer tenureMonths;

    @NotNull(message = "Repayment frequency is required")
    private String repaymentFrequency; // MONTHLY, WEEKLY

    @NotNull(message = "Disbursement date is required")
    private LocalDate disbursementDate;

    @NotNull(message = "Disbursement method is required")
    private String disbursementMethod; // BANK_TRANSFER, CASH, CHEQUE

    @NotNull(message = "First due date is required")
    private LocalDate firstDueDate;
}
