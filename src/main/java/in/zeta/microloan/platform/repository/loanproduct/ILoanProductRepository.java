package in.zeta.microloan.platform.repository.loanproduct;

import in.zeta.microloan.platform.model.LoanProduct;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

public interface ILoanProductRepository {
    List<LoanProduct> findAllActive();
    Optional<LoanProduct> findById(Long id);
    Long create(LoanProduct product);
}
