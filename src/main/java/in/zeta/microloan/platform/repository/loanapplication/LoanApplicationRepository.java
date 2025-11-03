package in.zeta.microloan.platform.repository.loanapplication;

import in.zeta.microloan.platform.model.LoanApplication;
import in.zeta.microloan.platform.model.enums.LoanApplicationStatus;
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
import java.util.UUID;

@Repository
public class LoanApplicationRepository {

    private final JdbcTemplate jdbcTemplate;

    public LoanApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<LoanApplication> rowMapper = (rs, rowNum) -> LoanApplication.builder()
            .id(rs.getObject("id", UUID.class))
            .applicationNumber(rs.getString("application_number"))
            .borrowerId(rs.getObject("borrower_id", UUID.class))
            .productId(rs.getObject("product_id", UUID.class))
            .requestedAmount(rs.getBigDecimal("requested_amount"))
            .purpose(rs.getString("purpose"))
            .preferredTenure(rs.getInt("preferred_tenure"))
            .status(LoanApplicationStatus.valueOf(rs.getString("status")))
            .approvedAmount(rs.getBigDecimal("approved_amount"))
            .approvedAt(rs.getTimestamp("approved_at") != null ?
                    rs.getTimestamp("approved_at").toLocalDateTime() : null)
            .rejectionReason(rs.getString("rejection_reason"))
            .expiresAt(rs.getTimestamp("expires_at").toLocalDateTime())
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public LoanApplication create(LoanApplication application) {
        String sql = "INSERT INTO public.loan_applications (application_number, borrower_id, " +
                "product_id, requested_amount, purpose, preferred_tenure, status, expires_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, application.getApplicationNumber());
            ps.setObject(2, application.getBorrowerId());
            ps.setObject(3, application.getProductId());
            ps.setBigDecimal(4, application.getRequestedAmount());
            ps.setString(5, application.getPurpose());
            ps.setInt(6, application.getPreferredTenure());
            ps.setString(7, application.getStatus().name());
            ps.setTimestamp(8, java.sql.Timestamp.valueOf(application.getExpiresAt()));
            return ps;
        }, keyHolder);

        application.setId(UUID.fromString(keyHolder.getKeys().get("id").toString()));
        return application;
    }

    public Optional<LoanApplication> findById(UUID id) {
        String sql = "SELECT * FROM public.loan_applications WHERE id = ?";
        List<LoanApplication> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<LoanApplication> findByBorrowerId(UUID borrowerId) {
        String sql = "SELECT * FROM public.loan_applications WHERE borrower_id = ? " +
                "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, borrowerId);
    }

    public Optional<LoanApplication> findLatestByBorrowerId(UUID borrowerId) {
        String sql = "SELECT * FROM public.loan_applications WHERE borrower_id = ? " +
                "ORDER BY created_at DESC LIMIT 1";
        List<LoanApplication> results = jdbcTemplate.query(sql, rowMapper, borrowerId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<LoanApplication> findByStatus(LoanApplicationStatus status) {
        String sql = "SELECT * FROM public.loan_applications WHERE status = ? " +
                "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }

    public List<LoanApplication> findAll() {
        String sql = "SELECT * FROM public.loan_applications ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<LoanApplication> findPendingApplications() {
        String sql = "SELECT * FROM public.loan_applications " +
                "WHERE status IN ('PENDING_REVIEW', 'UNDER_VERIFICATION') " +
                "AND expires_at > NOW() " +
                "ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<LoanApplication> findExpiredApplications() {
        String sql = "SELECT * FROM public.loan_applications " +
                "WHERE status IN ('PENDING_REVIEW', 'UNDER_VERIFICATION') " +
                "AND expires_at < NOW() " +
                "ORDER BY expires_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public boolean hasPendingApplication(UUID borrowerId) {
        String sql = "SELECT COUNT(*) FROM public.loan_applications WHERE borrower_id = ? " +
                "AND status IN ('PENDING_REVIEW', 'UNDER_VERIFICATION')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, borrowerId);
        return count != null && count > 0;
    }

    public void approve(UUID id, BigDecimal approvedAmount) {
        String sql = "UPDATE public.loan_applications SET status = ?, approved_amount = ?, " +
                 "approved_at = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                LoanApplicationStatus.APPROVED.name(),
                approvedAmount,
                LocalDateTime.now(),
                id
        );
    }

    public void reject(UUID id, String rejectionReason) {
        String sql = "UPDATE public.loan_applications SET status = ?, rejection_reason = ? " +
                "WHERE id = ?";
        jdbcTemplate.update(sql,
                LoanApplicationStatus.REJECTED.name(),
                rejectionReason,
                id
        );
    }

    public void cancel(UUID id) {
        String sql = "UPDATE public.loan_applications SET status = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                LoanApplicationStatus.CANCELLED.name(),
                LocalDateTime.now(),
                id
        );
    }

    public void updateStatus(UUID id, LoanApplicationStatus status) {
        String sql = "UPDATE public.loan_applications SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), id);
    }

    public int expirePendingApplications() {
        String sql = "UPDATE public.loan_applications " +
                "SET status = ?, updated_at = NOW() " +
                "WHERE status =  " +
                "AND expires_at < NOW()";
        return jdbcTemplate.update(sql,
                LoanApplicationStatus.EXPIRED.name(),
                LoanApplicationStatus.PENDING_REVIEW.name());
    }

    public int countByBorrowerAndStatus(UUID borrowerId, LoanApplicationStatus status) {
        String sql = "SELECT COUNT(*) FROM public.loan_applications " +
                "WHERE borrower_id = ? AND status = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, borrowerId, status.name());
        return count != null ? count : 0;
    }

    public int countByStatus(LoanApplicationStatus status) {
        String sql = "SELECT COUNT(*) FROM public.loan_applications WHERE status = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, status.name());
        return count != null ? count : 0;
    }

    public List<LoanApplication> findExpiringApplications(int daysBeforeExpiry) {
        String sql = "SELECT * FROM public.loan_applications " +
                "WHERE status IN ('PENDING_REVIEW', 'UNDER_VERIFICATION') " +
                "AND expires_at BETWEEN NOW() AND NOW() + INTERVAL '" + daysBeforeExpiry + " days' " +
                "ORDER BY expires_at ASC";
        return jdbcTemplate.query(sql, rowMapper);
    }
}
