package in.zeta.microloan.platform.repository.repayment;

import in.zeta.microloan.platform.model.enums.PaymentMethod;
import in.zeta.microloan.platform.model.enums.PaymentStatus;
import in.zeta.microloan.platform.model.Repayment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

@Repository
public class RepaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    public RepaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Repayment> rowMapper = (rs, rowNum) -> Repayment.builder()
            .id(rs.getObject("id", UUID.class))
            .receiptNumber(rs.getString("receipt_number"))
            .loanId(rs.getObject("loan_id", UUID.class))
            .borrowerId(rs.getObject("borrower_id", UUID.class))
            .householdId(rs.getObject("household_id", UUID.class))
            .amount(rs.getBigDecimal("amount"))
            .principalPaid(rs.getBigDecimal("principal_paid"))
            .interestPaid(rs.getBigDecimal("interest_paid"))
            .lateFeePaid(rs.getBigDecimal("late_fee_paid"))
            .advancePayment(rs.getBigDecimal("advance_payment"))
            .paymentDate(rs.getDate("payment_date").toLocalDate())
            .paymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")))
            .transactionRef(rs.getString(rs.getString("transaction_ref")))
            .notes(rs.getString("notes"))
            .status(PaymentStatus.valueOf("status"))
            .receiptUrl(rs.getString("receipt_url"))
            .createdBy(rs.getObject("created_by", Long.class))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    public UUID create(Repayment repayment) {
        String sql = "INSERT INTO public.repayments (receipt_number, loan_id, borrower_id, household_id, " +
                "amount, principal_paid, interest_paid, late_fee_paid, advance_payment, payment_date, " +
                "payment_method, transaction_ref, notes, status, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, repayment.getReceiptNumber());
            ps.setObject(2, repayment.getLoanId());
            ps.setObject(3, repayment.getBorrowerId());
            ps.setObject(4, repayment.getHouseholdId());
            ps.setBigDecimal(5, repayment.getAmount());
            ps.setBigDecimal(6, repayment.getPrincipalPaid());
            ps.setBigDecimal(7, repayment.getInterestPaid());
            ps.setBigDecimal(8, repayment.getLateFeePaid());
            ps.setBigDecimal(9, repayment.getAdvancePayment());
            ps.setDate(10, java.sql.Date.valueOf(repayment.getPaymentDate()));
            ps.setString(11, repayment.getPaymentMethod().name());
            ps.setString(12, repayment.getTransactionRef());
            ps.setString(13, repayment.getNotes());
            ps.setString(14, repayment.getStatus().name());
            ps.setObject(15, repayment.getCreatedBy());
            return ps;
        }, keyHolder);

        UUID key = (UUID) keyHolder.getKeys().get("id");
        return key;
    }

    public List<Repayment> findByLoanId(UUID loanId) {
        String sql = "SELECT * FROM public.repayments WHERE loan_id = ? ORDER BY payment_date DESC";
        return jdbcTemplate.query(sql, rowMapper, loanId);
    }
}
