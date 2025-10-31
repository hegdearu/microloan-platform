package in.zeta.microloan.platform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanProductDTO {
    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Minimum amount is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal minAmount;

    @NotNull(message = "Maximum amount is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal maxAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal interestRate;

    @NotBlank(message = "Processing fee type is required")
    private String processingFeeType; // PERCENTAGE, FLAT

    @NotNull(message = "Processing fee value is required")
    private BigDecimal processingFeeValue;

    @NotNull(message = "Tenure is required")
    @Min(value = 1)
    private Integer tenureMonths;

    private Integer gracePeriodDays;

    @NotNull
    private BigDecimal lateFeePercent;

    private BigDecimal maxLateFeePercent;

    private String prepaymentChargesType; // PERCENTAGE, FLAT, NONE

    private BigDecimal prepaymentChargesValue;
}
