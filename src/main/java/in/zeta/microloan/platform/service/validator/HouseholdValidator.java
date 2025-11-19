package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.HouseholdRegistrationRequestDTO;
import in.zeta.microloan.platform.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HouseholdValidator {

    public void validateRegistration(HouseholdRegistrationRequestDTO dto) {
        if (dto.getTotalAnnualIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Total annual income must be positive");
        }

        if (dto.getPincode() == null || !dto.getPincode().matches("^[0-9]{6}$")) {
            throw new ValidationException("Invalid pincode. Must be 6 digits");
        }
    }
}
