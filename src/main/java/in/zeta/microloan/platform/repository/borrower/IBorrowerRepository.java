package in.zeta.microloan.platform.repository.borrower;

import in.zeta.microloan.platform.model.Borrower;

import java.util.Optional;

public interface IBorrowerRepository {
    Long create(Borrower borrower);
    Optional<Borrower> findById(Long id);
    Optional<Borrower> findByPhone(String phone);
    Optional<Borrower> findByEmail(String email);
    void update(Borrower borrower);
    int countActiveLoans(Long borrowerId);
}
