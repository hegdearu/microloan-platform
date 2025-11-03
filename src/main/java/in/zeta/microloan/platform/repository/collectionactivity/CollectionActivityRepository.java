package in.zeta.microloan.platform.repository.collectionactivity;

import in.zeta.microloan.platform.model.CollectionActivity;
import in.zeta.microloan.platform.model.enums.ContactMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class CollectionActivityRepository {

    private final JdbcTemplate jdbcTemplate;

    public CollectionActivityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<CollectionActivity> rowMapper = (rs, rowNum) -> CollectionActivity.builder()
            .id(rs.getObject("id", UUID.class))
            .loanId(rs.getObject("loan_id", UUID.class))
            .activityType(rs.getString("activity_type"))
            .contactMethod(ContactMethod.valueOf("contact_method"))
            .borrowerResponse(rs.getString("borrower_response"))
            .promiseToPayDate(rs.getDate("promise_to_pay_date") != null ?
                    rs.getDate("promise_to_pay_date").toLocalDate() : null)
            .paymentArrangement(rs.getString("payment_arrangement"))
            .notes(rs.getString("notes"))
            .assignedTo(rs.getObject("assigned_to", UUID.class))
            .activityDate(rs.getTimestamp("activity_date").toLocalDateTime())
            .nextFollowUpDate(rs.getDate("next_follow_up_date") != null ?
                    rs.getDate("next_follow_up_date").toLocalDate() : null)
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public UUID create(CollectionActivity activity) {
        String sql = "INSERT INTO public.collection_activities (loan_id, activity_type, contact_method, " +
                "borrower_response, promise_to_pay_date, payment_arrangement, notes, assigned_to, " +
                "activity_date, next_follow_up_date, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, activity.getLoanId());
            ps.setString(2, activity.getActivityType());
            ps.setString(3, activity.getContactMethod().toString());
            ps.setString(4, activity.getBorrowerResponse());
            ps.setDate(5, activity.getPromiseToPayDate() != null ?
                    java.sql.Date.valueOf(activity.getPromiseToPayDate()) : null);
            ps.setString(6, activity.getPaymentArrangement());
            ps.setString(7, activity.getNotes());
            ps.setObject(8, activity.getAssignedTo());
            ps.setTimestamp(9, java.sql.Timestamp.valueOf(activity.getActivityDate()));
            ps.setDate(10, activity.getNextFollowUpDate() != null ?
                    java.sql.Date.valueOf(activity.getNextFollowUpDate()) : null);
            ps.setTimestamp(11, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(12, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        return UUID.fromString(keyHolder.getKey().toString());
    }

    public List<CollectionActivity> findByLoanId(UUID loanId) {
        String sql = "SELECT * FROM public.collection_activities WHERE loan_id = ? ORDER BY activity_date DESC";
        return jdbcTemplate.query(sql, rowMapper, loanId);
    }
}
