package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.LoanProductRequestDTO;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LoanProductValidator {
    private static final SpectraLogger log = OlympusSpectra.getLogger(LoanProductValidator.class);

    public void validate(LoanProductRequestDTO dto) {
        if (dto.getMinAmount().compareTo(dto.getMaxAmount()) > 0) {
            log.warn("LOAN_PRODUCT_VALIDATE_MIN_GT_MAX")
                    .attr("minAmount", dto.getMinAmount())
                    .attr("maxAmount", dto.getMaxAmount())
                    .log();
            throw new ValidationException("Minimum amount cannot be greater than maximum amount");
        }
        if (dto.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("LOAN_PRODUCT_VALIDATE_INTEREST_NON_POSITIVE")
                    .attr("interestRate", dto.getInterestRate())
                    .log();
            throw new ValidationException("Interest rate must be positive");
        }
        if (dto.getTenureMonths() <= 0) {
            log.warn("LOAN_PRODUCT_VALIDATE_TENURE_NON_POSITIVE")
                    .attr("tenureMonths", dto.getTenureMonths())
                    .log();
            throw new ValidationException("Tenure must be positive");
        }
    }
}
