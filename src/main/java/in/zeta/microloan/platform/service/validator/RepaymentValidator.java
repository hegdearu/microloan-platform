package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.RepaymentRequestDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.model.Loan;
import in.zeta.microloan.platform.model.RepaymentSchedule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RepaymentValidator {

    public void validateRecord(RepaymentRequestDTO dto, Loan loan, List<RepaymentSchedule> pendingSchedules) {
        if (!(loan.getStatus().name().equals("ACTIVE") || loan.getStatus().name().equals("OVERDUE"))) {
            throw new BusinessRuleException("Repayment can only be made for active or overdue loans");
        }
        if (pendingSchedules.isEmpty()) {
            throw new BusinessRuleException("No pending installments found");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Repayment amount must be positive");
        }
    }
}
