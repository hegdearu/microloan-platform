package in.zeta.microloan.platform.dto.request;

import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class LoanApplicationRequestDTO {

    @NotNull(message = "Borrower ID is required")
    private UUID borrowerId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "5000.00", message = "Minimum loan amount is ₹5,000")
    @DecimalMax(value = "100000.00", message = "Maximum loan amount is ₹1,00,000")
    private BigDecimal requestedAmount;

    @NotBlank(message = "Purpose is required")
    @Size(max = 1000, message = "Purpose must not exceed 1000 characters")
    private String purpose;

    @NotNull(message = "Preferred tenure is required")
    @Min(value = 1, message = "Minimum tenure is 1 month")
    @Max(value = 24, message = "Maximum tenure is 24 months")
    private Integer preferredTenure;
}
