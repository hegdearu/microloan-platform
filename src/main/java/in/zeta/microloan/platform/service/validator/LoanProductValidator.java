package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.LoanProductRequestDTO;
import in.zeta.microloan.platform.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LoanProductValidator {
    public void validate(LoanProductRequestDTO dto) {
        if (dto.getMinAmount().compareTo(dto.getMaxAmount()) > 0) {
            throw new ValidationException("Minimum amount cannot be greater than maximum amount");
        }
        if (dto.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Interest rate must be positive");
        }
        if (dto.getTenureMonths() <= 0) {
            throw new ValidationException("Tenure must be positive");
        }
    }
}
