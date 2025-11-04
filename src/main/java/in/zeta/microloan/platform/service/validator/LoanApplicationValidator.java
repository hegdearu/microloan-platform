package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.LoanApplicationRequestDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.LoanProduct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LoanApplicationValidator {

    public void validateCreate(LoanApplicationRequestDTO dto,
                               Borrower borrower,
                               LoanProduct product,
                               int activeLoansCount,
                               int maxActiveLoans,
                               boolean hasPendingApplication) {

        if (!borrower.getIsVerified()) {
            throw new BusinessRuleException("Borrower must be verified before applying for loan");
        }
        if (!"ACTIVE".equals(product.getStatus().name())) {
            throw new BusinessRuleException("Selected loan product is not active");
        }
        if (activeLoansCount >= maxActiveLoans) {
            throw new BusinessRuleException("Maximum " + maxActiveLoans + " active loans allowed per borrower");
        }
        if (hasPendingApplication) {
            throw new BusinessRuleException("You already have a pending loan application");
        }
        BigDecimal req = dto.getRequestedAmount();
        if (req.compareTo(product.getMinAmount()) < 0 || req.compareTo(product.getMaxAmount()) > 0) {
            throw new ValidationException(String.format("Loan amount must be between ₹%s and ₹%s",
                    product.getMinAmount(), product.getMaxAmount()));
        }
        if (dto.getPreferredTenure() != null && dto.getPreferredTenure() > product.getTenureMonths()) {
            throw new ValidationException(String.format("Maximum tenure for this product is %d months",
                    product.getTenureMonths()));
        }
    }

    public void validateApproveAmount(BigDecimal approvedAmount, LoanProduct product) {
        if (approvedAmount.compareTo(product.getMinAmount()) < 0 ||
                approvedAmount.compareTo(product.getMaxAmount()) > 0) {
            throw new ValidationException(String.format("Approved amount must be between ₹%s and ₹%s",
                    product.getMinAmount(), product.getMaxAmount()));
        }
    }
}
