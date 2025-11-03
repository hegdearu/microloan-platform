package in.zeta.microloan.platform.repository.repaymentschedule;

import in.zeta.microloan.platform.model.enums.InstallmentStatus;
import in.zeta.microloan.platform.model.RepaymentSchedule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public class RepaymentScheduleRepository {

    private final JdbcTemplate jdbcTemplate;

    public RepaymentScheduleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<RepaymentSchedule> rowMapper = (rs, rowNum) -> RepaymentSchedule.builder()
            .id(rs.getLong("id"))
            .loanId(rs.getLong("loan_id"))
            .installmentNumber(rs.getInt("installment_number"))
            .dueDate(rs.getDate("due_date").toLocalDate())
            .principalDue(rs.getBigDecimal("principal_due"))
            .interestDue(rs.getBigDecimal("interest_due"))
            .totalDue(rs.getBigDecimal("total_due"))
            .principalPaid(rs.getBigDecimal("principal_paid"))
            .interestPaid(rs.getBigDecimal("interest_paid"))
            .lateFeePaid(rs.getBigDecimal("late_fee_paid"))
            .totalPaid(rs.getBigDecimal("total_paid"))
            .status(InstallmentStatus.valueOf(rs.getString("status")))
            .paidDate(rs.getDate("paid_date") != null ? rs.getDate("paid_date").toLocalDate() : null)
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public void create(RepaymentSchedule schedule) {
        // Removed created_at and updated_at - database handles these with DEFAULT NOW()
        String sql = "INSERT INTO public.repayment_schedule (loan_id, installment_number, due_date, " +
                "principal_due, interest_due, total_due, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                schedule.getLoanId(),
                schedule.getInstallmentNumber(),
                schedule.getDueDate(),
                schedule.getPrincipalDue(),
                schedule.getInterestDue(),
                schedule.getTotalDue(),
                schedule.getStatus().name()
        );
    }

    public List<RepaymentSchedule> findByLoanId(Long loanId) {
        String sql = "SELECT * FROM public.repayment_schedule WHERE loan_id = ? ORDER BY installment_number";
        return jdbcTemplate.query(sql, rowMapper, loanId);
    }

    public List<RepaymentSchedule> findPendingByLoanId(Long loanId) {
        String sql = "SELECT * FROM public.repayment_schedule WHERE loan_id = ? " +
                "AND status IN ('PENDING', 'PARTIALLY_PAID', 'OVERDUE') ORDER BY installment_number";
        return jdbcTemplate.query(sql, rowMapper, loanId);
    }

    public void updatePayment(Long id, BigDecimal principalPaid, BigDecimal interestPaid,
                              BigDecimal lateFeePaid, String status, LocalDate paidDate) {
        String sql = "UPDATE public.repayment_schedule SET " +
                "principal_paid = principal_paid + ?, " +
                "interest_paid = interest_paid + ?, " +
                "late_fee_paid = late_fee_paid + ?, " +
                "total_paid = principal_paid + interest_paid + late_fee_paid, " +
                "status = ?, " +
                "paid_date = ? " +
                "WHERE id = ?";

        jdbcTemplate.update(sql,
                principalPaid,
                interestPaid,
                lateFeePaid,
                status,
                paidDate != null ? java.sql.Date.valueOf(paidDate) : null,
                id
        );
    }

    public void updateStatus(Long id, String status) {
        String sql = "UPDATE public.repayment_schedule SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, id);
    }
}
