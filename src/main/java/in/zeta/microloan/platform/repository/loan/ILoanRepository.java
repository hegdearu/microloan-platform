package in.zeta.microloan.platform.repository.loan;

import in.zeta.microloan.platform.model.Loan;

public interface ILoanRepository {
    int countActiveLoansByBorrower(String borrowerId);

    Loan findLastClosedLoanByBorrower(String borrowerId);
}
