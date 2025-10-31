package in.zeta.microloan.platform.repository;

import in.zeta.microloan.platform.model.CollectionStage;
import in.zeta.microloan.platform.model.OverdueTracking;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OverdueTrackingRepository {

    private final JdbcTemplate jdbcTemplate;

    public OverdueTrackingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<OverdueTracking> rowMapper = (rs, rowNum) -> OverdueTracking.builder()
            .id(rs.getLong("id"))
            .loanId(rs.getLong("loan_id"))
            .overdueSince(rs.getDate("overdue_since").toLocalDate())
            .overdueDays(rs.getInt("overdue_days"))
            .overduePrincipal(rs.getBigDecimal("overdue_principal"))
            .overdueInterest(rs.getBigDecimal("overdue_interest"))
            .overdueAmount(rs.getBigDecimal("overdue_amount"))
            .penaltyAmount(rs.getBigDecimal("penalty_amount"))
            .totalDue(rs.getBigDecimal("total_due"))
            .lastCheckedAt(rs.getTimestamp("last_checked_at").toLocalDateTime())
            .collectionStage(CollectionStage.valueOf("collection_stage"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public void create(OverdueTracking tracking) {
        String sql = "INSERT INTO public.overdue_tracking (loan_id, overdue_since, overdue_days, " +
                "overdue_principal, overdue_interest, overdue_amount, penalty_amount, total_due, " +
                "last_checked_at, collection_stage, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                tracking.getLoanId(),
                tracking.getOverdueSince(),
                tracking.getOverdueDays(),
                tracking.getOverduePrincipal(),
                tracking.getOverdueInterest(),
                tracking.getOverdueAmount(),
                tracking.getPenaltyAmount(),
                tracking.getTotalDue(),
                tracking.getLastCheckedAt(),
                tracking.getCollectionStage(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public void update(OverdueTracking tracking) {
        String sql = "UPDATE public.overdue_tracking SET overdue_days = ?, overdue_principal = ?, " +
                "overdue_interest = ?, overdue_amount = ?, penalty_amount = ?, total_due = ?, " +
                "last_checked_at = ?, collection_stage = ?, updated_at = ? WHERE loan_id = ?";

        jdbcTemplate.update(sql,
                tracking.getOverdueDays(),
                tracking.getOverduePrincipal(),
                tracking.getOverdueInterest(),
                tracking.getOverdueAmount(),
                tracking.getPenaltyAmount(),
                tracking.getTotalDue(),
                tracking.getLastCheckedAt(),
                tracking.getCollectionStage(),
                LocalDateTime.now(),
                tracking.getLoanId()
        );
    }

    public Optional<OverdueTracking> findByLoanId(Long loanId) {
        String sql = "SELECT * FROM public.overdue_tracking WHERE loan_id = ?";
        List<OverdueTracking> results = jdbcTemplate.query(sql, rowMapper, loanId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<OverdueTracking> findAll() {
        String sql = "SELECT * FROM public.overdue_tracking ORDER BY overdue_days DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public void deleteByLoanId(Long loanId) {
        String sql = "DELETE FROM public.overdue_tracking WHERE loan_id = ?";
        jdbcTemplate.update(sql, loanId);
    }
}
