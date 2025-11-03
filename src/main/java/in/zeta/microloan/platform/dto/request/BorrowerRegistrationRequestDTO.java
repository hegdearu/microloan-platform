package in.zeta.microloan.platform.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class BorrowerRegistrationRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    private UUID householdId;

    private String relationshipToHead;

    private Boolean isHouseholdHead;

    @NotNull(message = "Individual annual income is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Income must be greater than 0")
    private BigDecimal individualAnnualIncome;

    @NotBlank(message = "Occupation is required")
    private String occupation;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "ID proof type is required")
    private String idProofType;

    @NotBlank(message = "ID proof number is required")
    private String idProofNumber;

    private String employmentDetails;

    private String incomeDetails;
}
