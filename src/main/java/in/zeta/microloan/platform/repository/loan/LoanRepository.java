package in.zeta.microloan.platform.repository.loan;

import in.zeta.microloan.platform.model.enums.DisbursementMethod;
import in.zeta.microloan.platform.model.Loan;
import in.zeta.microloan.platform.model.enums.LoanStatus;
import in.zeta.microloan.platform.model.enums.RepaymentFrequency;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class LoanRepository {

    private final JdbcTemplate jdbcTemplate;

    public LoanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Loan> rowMapper = (rs, rowNum) -> Loan.builder()
            .id(rs.getLong("id"))
            .loanNumber(rs.getString("loan_number"))
            .applicationId(rs.getObject("application_id", Long.class))
            .borrowerId(rs.getLong("borrower_id"))
            .householdId(rs.getObject("household_id", Long.class))
            .productId(rs.getLong("product_id"))
            .principalAmount(rs.getBigDecimal("principal_amount"))
            .interestRate(rs.getBigDecimal("interest_rate"))
            .processingFee(rs.getBigDecimal("processing_fee"))
            .tenureMonths(rs.getInt("tenure_months"))
            .repaymentFrequency(RepaymentFrequency.valueOf(rs.getString("repayment_frequency")))
            .emiAmount(rs.getBigDecimal("emi_amount"))
            .totalPayable(rs.getBigDecimal("total_payable"))
            .outstandingPrincipal(rs.getBigDecimal("outstanding_principal"))
            .outstandingInterest(rs.getBigDecimal("outstanding_interest"))
            .totalOutstanding(rs.getBigDecimal("total_outstanding"))
            .totalPaid(rs.getBigDecimal("total_paid"))
            .disbursementDate(rs.getDate("disbursement_date").toLocalDate())
            .disbursementMethod(DisbursementMethod.valueOf(rs.getString("disbursement_method")))
            .firstDueDate(rs.getDate("first_due_date").toLocalDate())
            .lastPaymentDate(rs.getDate("last_payment_date") != null ?
                    rs.getDate("last_payment_date").toLocalDate() : null)
            .status(LoanStatus.valueOf(rs.getString("status")))
            .closedDate(rs.getDate("closed_date") != null ?
                    rs.getDate("closed_date").toLocalDate() : null)
            .gracePeriodDays(rs.getInt("grace_period_days"))
            .lateFeePercent(rs.getBigDecimal("late_fee_percent"))
            .agreementUrl(rs.getString("agreement_url"))
            .householdIncomeAtApproval(rs.getBigDecimal("household_income_at_approval"))
            .createdBy(rs.getLong("created_by"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public Long create(Loan loan) {
        String sql = "INSERT INTO public.loans (loan_number, application_id, borrower_id, household_id, " +
                "product_id, principal_amount, interest_rate, processing_fee, tenure_months, " +
                "repayment_frequency, emi_amount, total_payable, outstanding_principal, " +
                "outstanding_interest, total_outstanding, total_paid, disbursement_date, " +
                "disbursement_method, first_due_date, status, grace_period_days, late_fee_percent, " +
                "household_income_at_approval, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, loan.getLoanNumber());
            ps.setObject(2, loan.getApplicationId());
            ps.setLong(3, loan.getBorrowerId());
            ps.setObject(4, loan.getHouseholdId());
            ps.setLong(5, loan.getProductId());
            ps.setBigDecimal(6, loan.getPrincipalAmount());
            ps.setBigDecimal(7, loan.getInterestRate());
            ps.setBigDecimal(8, loan.getProcessingFee());
            ps.setInt(9, loan.getTenureMonths());
            ps.setString(10, loan.getRepaymentFrequency().name());
            ps.setBigDecimal(11, loan.getEmiAmount());
            ps.setBigDecimal(12, loan.getTotalPayable());
            ps.setBigDecimal(13, loan.getOutstandingPrincipal());
            ps.setBigDecimal(14, loan.getOutstandingInterest());
            ps.setBigDecimal(15, loan.getTotalOutstanding());
            ps.setBigDecimal(16, loan.getTotalPaid());
            ps.setDate(17, java.sql.Date.valueOf(loan.getDisbursementDate()));
            ps.setString(18, loan.getDisbursementMethod().name());
            ps.setDate(19, java.sql.Date.valueOf(loan.getFirstDueDate()));
            ps.setString(20, loan.getStatus().name());
            ps.setInt(21, loan.getGracePeriodDays());
            ps.setBigDecimal(22, loan.getLateFeePercent());
            ps.setBigDecimal(23, loan.getHouseholdIncomeAtApproval());
            ps.setLong(24, loan.getCreatedBy());
            return ps;
        }, keyHolder);

        // Fix: Get the ID from the keys map instead of using getKey()
        Number key = (Number) keyHolder.getKeys().get("id");
        return key.longValue();
    }

    public Optional<Loan> findById(Long id) {
        String sql = "SELECT * FROM public.loans WHERE id = ?";
        List<Loan> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Loan> findByBorrowerId(Long borrowerId) {
        String sql = "SELECT * FROM public.loans WHERE borrower_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, borrowerId);
    }

    public List<Loan> findByHouseholdId(Long householdId) {
        String sql = "SELECT * FROM public.loans WHERE household_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, householdId);
    }

    public BigDecimal getTotalHouseholdExposure(Long householdId) {
        String sql = "SELECT COALESCE(SUM(total_outstanding), 0) FROM public.loans " +
                "WHERE household_id = ? AND status IN ('ACTIVE', 'OVERDUE')";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, householdId);
    }

    public List<Loan> findByStatus(String status) {
        String sql = "SELECT * FROM public.loans WHERE status = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, status);
    }

    public void updateStatus(Long loanId, String status) {
        String sql = "UPDATE public.loans SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, loanId);
    }

    public void updateTotalPaid(Long loanId, BigDecimal amount) {
        String sql = "UPDATE public.loans SET total_paid = total_paid + ? WHERE id = ?";
        jdbcTemplate.update(sql, amount, loanId);
    }

    public void updateOutstanding(Long loanId, BigDecimal principal, BigDecimal interest) {
        String sql = "UPDATE public.loans SET outstanding_principal = outstanding_principal - ?, " +
                "outstanding_interest = outstanding_interest - ?, " +
                "total_outstanding = outstanding_principal + outstanding_interest, " +
                "last_payment_date = ? WHERE id = ?";
        jdbcTemplate.update(sql, principal, interest, java.time.LocalDate.now(), loanId);
    }

    public boolean existsByApplicationId(Long applicationId) {
        String sql = "SELECT COUNT(*) FROM public.loans WHERE application_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, applicationId);
        return count != null && count > 0;
    }
}