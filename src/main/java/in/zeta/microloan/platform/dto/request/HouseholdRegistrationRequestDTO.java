package in.zeta.microloan.platform.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class HouseholdRegistrationRequestDTO {

    @NotBlank(message = "Primary address is required")
    private String primaryAddress;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotNull(message = "Total annual income is required")
    @DecimalMin(value = "0.00", message = "Income must be positive")
    private BigDecimal totalAnnualIncome;

    @NotNull(message = "Number of members is required")
    @Min(value = 1, message = "There must be at least one member in the household")
    private Integer totalMembers;
    private String incomeProofType;
    private String householdType;
}

