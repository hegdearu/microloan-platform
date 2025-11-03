package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.LoanIssuanceRequestDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.LoanApplication;
import in.zeta.microloan.platform.model.LoanProduct;
import in.zeta.microloan.platform.model.enums.UserStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LoanValidator {

    public void validateCreate(LoanIssuanceRequestDTO dto,
                               LoanApplication application,
                               LoanProduct product,
                               Borrower borrower) {
        if (!"ACTIVE".equals(product.getStatus().name())) {
            throw new BusinessRuleException("Cannot create loan for inactive product");
        }
        if (!borrower.getIsVerified()) {
            throw new BusinessRuleException("Borrower must be verified before loan disbursement");
        }
        if (borrower.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Borrower status must be ACTIVE. Current status: " + borrower.getStatus());
        }
        if (dto.getPrincipalAmount().compareTo(product.getMinAmount()) < 0 ||
                dto.getPrincipalAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessRuleException(String.format("Loan amount must be between ₹%s and ₹%s",
                    product.getMinAmount(), product.getMaxAmount()));
        }
        if (dto.getTenureMonths() > product.getTenureMonths()) {
            throw new BusinessRuleException(String.format("Tenure cannot exceed %d months for this product",
                    product.getTenureMonths()));
        }
        if (application != null) {
            if (application.getStatus() == null || !application.getStatus().name().equals("APPROVED")) {
                throw new BusinessRuleException("Loan can only be created for APPROVED applications. Current status: " +
                        application.getStatus());
            }
            if (LocalDateTime.now().isAfter(application.getExpiresAt())) {
                throw new BusinessRuleException("Loan application has expired");
            }
            if (application.getApprovedAmount() != null &&
                    dto.getPrincipalAmount().compareTo(application.getApprovedAmount()) != 0) {
                throw new BusinessRuleException(String.format("Principal amount ₹%s does not match approved amount ₹%s",
                        dto.getPrincipalAmount(), application.getApprovedAmount()));
            }
            if (!application.getBorrowerId().equals(dto.getBorrowerId())) {
                throw new BusinessRuleException("Borrower ID does not match the loan application");
            }
            if (!application.getProductId().equals(dto.getProductId())) {
                throw new BusinessRuleException("Product ID does not match the loan application");
            }
        }
    }
}
