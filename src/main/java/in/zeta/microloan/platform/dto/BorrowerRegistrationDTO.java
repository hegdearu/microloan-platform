package in.zeta.microloan.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BorrowerRegistrationDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    private Long householdId;
    private String relationshipToHead;
    private Boolean isHouseholdHead;

    @DecimalMin(value = "0.00", message = "Income must be positive")
    private BigDecimal individualAnnualIncome;

    private String occupation;
    private String address;

    @NotBlank(message = "ID proof type is required")
    private String idProofType;

    @NotBlank(message = "ID proof number is required")
    private String idProofNumber;

    private String employmentDetails;
    private String incomeDetails;
}
