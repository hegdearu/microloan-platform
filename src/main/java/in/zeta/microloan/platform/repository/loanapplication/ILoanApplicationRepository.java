package in.zeta.microloan.platform.repository.loanapplication;

import in.zeta.microloan.platform.model.LoanApplication;

public interface ILoanApplicationRepository {
    LoanApplication create(in.zeta.microloan.platform.model.LoanApplication application);
    java.util.Optional<in.zeta.microloan.platform.model.LoanApplication> findById(Long id);
    java.util.List<in.zeta.microloan.platform.model.LoanApplication> findByBorrowerId(Long borrowerId);
    java.util.List<in.zeta.microloan.platform.model.LoanApplication> findByStatus(in.zeta.microloan.platform.model.LoanApplicationStatus status);
    void updateStatus(Long id, in.zeta.microloan.platform.model.LoanApplicationStatus status);
    void approve(Long id, Long approvedBy, java.math.BigDecimal approvedAmount);
    void reject(Long id, String rejectionReason);
    boolean hasPendingApplication(Long borrowerId);
}
