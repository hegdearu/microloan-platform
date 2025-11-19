package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.CollectionActivityRequestDTO;
import in.zeta.microloan.platform.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class CollectionValidator {

    public void validateActivity(CollectionActivityRequestDTO dto) {
        if (dto.getLoanId() == null) {
            throw new ValidationException("Loan ID is required");
        }

        if (dto.getActivityType() == null || dto.getActivityType().trim().isEmpty()) {
            throw new ValidationException("Activity type is required");
        }

        if (dto.getContactMethod() == null || dto.getContactMethod().trim().isEmpty()) {
            throw new ValidationException("Contact method is required");
        }
    }
}
