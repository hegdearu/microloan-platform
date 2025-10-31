package in.zeta.microloan.platform.repository.loanapplication;

import in.zeta.microloan.platform.model.LoanApplication;
import in.zeta.microloan.platform.model.LoanApplicationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class LoanApplicationRepository {

    private final JdbcTemplate jdbcTemplate;

    public LoanApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<LoanApplication> rowMapper = (rs, rowNum) -> LoanApplication.builder()
            .id(rs.getLong("id"))
            .applicationNumber(rs.getString("application_number"))
            .borrowerId(rs.getLong("borrower_id"))
            .productId(rs.getLong("product_id"))
            .requestedAmount(rs.getBigDecimal("requested_amount"))
            .purpose(rs.getString("purpose"))
            .preferredTenure(rs.getInt("preferred_tenure"))
            .status(LoanApplicationStatus.valueOf(rs.getString("status")))
            .approvedAmount(rs.getBigDecimal("approved_amount"))
            .approvedBy(rs.getObject("approved_by", Long.class))
            .approvedAt(rs.getTimestamp("approved_at") != null ?
                    rs.getTimestamp("approved_at").toLocalDateTime() : null)
            .rejectionReason(rs.getString("rejection_reason"))
            .expiresAt(rs.getTimestamp("expires_at").toLocalDateTime())
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public LoanApplication create(LoanApplication application) {
        String sql = "INSERT INTO public.loan_applications (application_number, borrower_id, " +
                "product_id, requested_amount, purpose, preferred_tenure, status, expires_at, " +
                "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, application.getApplicationNumber());
            ps.setLong(2, application.getBorrowerId());
            ps.setLong(3, application.getProductId());
            ps.setBigDecimal(4, application.getRequestedAmount());
            ps.setString(5, application.getPurpose());
            ps.setInt(6, application.getPreferredTenure());
            ps.setString(7, application.getStatus().name());
            ps.setTimestamp(8, java.sql.Timestamp.valueOf(application.getExpiresAt()));
            ps.setTimestamp(9, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(10, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        application.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        return application;
    }

    public Optional<LoanApplication> findById(Long id) {
        String sql = "SELECT * FROM public.loan_applications WHERE id = ?";
        List<LoanApplication> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<LoanApplication> findByBorrowerId(Long borrowerId) {
        String sql = "SELECT * FROM public.loan_applications WHERE borrower_id = ? " +
                "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, borrowerId);
    }

    public boolean hasPendingApplication(Long borrowerId) {
        String sql = "SELECT COUNT(*) FROM public.loan_applications WHERE borrower_id = ? " +
                "AND status IN ('PENDING_REVIEW', 'UNDER_VERIFICATION')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, borrowerId);
        return count != null && count > 0;
    }

    public void approve(Long id, Long approvedBy, BigDecimal approvedAmount) {
        String sql = "UPDATE public.loan_applications SET status = ?, approved_amount = ?, " +
                "approved_by = ?, approved_at = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                LoanApplicationStatus.APPROVED.name(),
                approvedAmount,
                approvedBy,
                LocalDateTime.now(),
                LocalDateTime.now(),
                id
        );
    }

    public void reject(Long id, String rejectionReason) {
        String sql = "UPDATE public.loan_applications SET status = ?, rejection_reason = ?, " +
                "updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                LoanApplicationStatus.REJECTED.name(),
                rejectionReason,
                LocalDateTime.now(),
                id
        );
    }
}
