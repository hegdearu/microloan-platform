package in.zeta.microloan.platform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerUpdateDTO {

    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    private String address;

    private String occupation;

    @DecimalMin(value = "0.0", inclusive = false, message = "Income must be greater than 0")
    private BigDecimal individualAnnualIncome;

    private String employmentDetails;

    private String incomeDetails;
}
