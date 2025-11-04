package in.zeta.microloan.platform.model.enums;

public enum LoanApplicationStatus {
    PENDING_REVIEW,      // Initial state when application is submitted
    APPROVED,            // When application is approved
    REJECTED,            // When application is rejected
    DISBURSED,          // When loan has been disbursed
    CANCELLED,          // When application is cancelled by borrower
    EXPIRED             // When application has expired
}
