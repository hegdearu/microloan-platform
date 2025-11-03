package in.zeta.microloan.platform.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
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

    private String incomeProofType;
    private String householdType;
}

