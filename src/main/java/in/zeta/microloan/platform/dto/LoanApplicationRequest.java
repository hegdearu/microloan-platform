package in.zeta.microloan.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoanApplicationRequest {
    @NotNull(message = "Borrower ID is required")
    private Long borrowerId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "5000.00", message = "Minimum loan amount is ₹5,000")
    @DecimalMax(value = "50000.00", message = "Maximum loan amount is ₹50,000")
    private BigDecimal requestedAmount;

    @NotBlank(message = "Purpose is required")
    @Size(max = 1000, message = "Purpose must not exceed 1000 characters")
    private String purpose;

    @NotNull(message = "Preferred tenure is required")
    @Min(value = 1, message = "Minimum tenure is 1 month")
    @Max(value = 24, message = "Maximum tenure is 24 months")
    private Integer preferredTenure;
}
