package in.zeta.microloan.platform.repository.loanproduct;

import in.zeta.microloan.platform.model.LoanProduct;
import in.zeta.microloan.platform.model.enums.LoanProductStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class LoanProductRepository {

    private final JdbcTemplate jdbcTemplate;

    public LoanProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<LoanProduct> rowMapper = (rs, rowNum) -> LoanProduct.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .description(rs.getString("description"))
            .minAmount(rs.getBigDecimal("min_amount"))
            .maxAmount(rs.getBigDecimal("max_amount"))
            .interestRate(rs.getBigDecimal("interest_rate"))
            .processingFeeType(rs.getString("processing_fee_type"))
            .processingFeeValue(rs.getBigDecimal("processing_fee_value"))
            .tenureMonths(rs.getInt("tenure_months"))
            .gracePeriodDays(rs.getInt("grace_period_days"))
            .lateFeePercent(rs.getBigDecimal("late_fee_percent"))
            .maxLateFeePercent(rs.getBigDecimal("max_late_fee_percent"))
            .prepaymentChargesType(rs.getString("prepayment_charges_type"))
            .prepaymentChargesValue(rs.getBigDecimal("prepayment_charges_value"))
            .status(LoanProductStatus.valueOf(rs.getString("status")))
            .version(rs.getInt("version"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public List<LoanProduct> findAllActive() {
        String sql = "SELECT * FROM public.loan_products WHERE status = 'ACTIVE' ORDER BY name";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<LoanProduct> findAll() {
        String sql = "SELECT * FROM public.loan_products ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<LoanProduct> findById(Long id) {
        String sql = "SELECT * FROM public.loan_products WHERE id = ?";
        List<LoanProduct> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Long create(LoanProduct product) {
        String sql = "INSERT INTO public.loan_products (name, description, min_amount, max_amount, " +
                "interest_rate, processing_fee_type, processing_fee_value, tenure_months, " +
                "grace_period_days, late_fee_percent, max_late_fee_percent, " +
                "prepayment_charges_type, prepayment_charges_value, status, version, " +
                "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setBigDecimal(3, product.getMinAmount());
            ps.setBigDecimal(4, product.getMaxAmount());
            ps.setBigDecimal(5, product.getInterestRate());
            ps.setString(6, product.getProcessingFeeType());
            ps.setBigDecimal(7, product.getProcessingFeeValue());
            ps.setInt(8, product.getTenureMonths());
            ps.setInt(9, product.getGracePeriodDays());
            ps.setBigDecimal(10, product.getLateFeePercent());
            ps.setBigDecimal(11, product.getMaxLateFeePercent());
            ps.setString(12, product.getPrepaymentChargesType());
            ps.setBigDecimal(13, product.getPrepaymentChargesValue());
            ps.setString(14, product.getStatus().name());
            ps.setInt(15, 1);
            ps.setTimestamp(16, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(17, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        Object idObj = keyHolder.getKeys().get("id");
        if (idObj instanceof Number) {
            return ((Number) idObj).longValue();
        } else {
            throw new IllegalStateException("Failed to retrieve generated Product id");
        }
    }

    public void update(LoanProduct product) {
        String sql = "UPDATE public.loan_products SET name = ?, description = ?, min_amount = ?, " +
                "max_amount = ?, interest_rate = ?, processing_fee_type = ?, processing_fee_value = ?, " +
                "tenure_months = ?, grace_period_days = ?, late_fee_percent = ?, max_late_fee_percent = ?, " +
                "prepayment_charges_type = ?, prepayment_charges_value = ?, status = ?, updated_at = ? " +
                "WHERE id = ?";

        jdbcTemplate.update(sql,
                product.getName(),
                product.getDescription(),
                product.getMinAmount(),
                product.getMaxAmount(),
                product.getInterestRate(),
                product.getProcessingFeeType(),
                product.getProcessingFeeValue(),
                product.getTenureMonths(),
                product.getGracePeriodDays(),
                product.getLateFeePercent(),
                product.getMaxLateFeePercent(),
                product.getPrepaymentChargesType(),
                product.getPrepaymentChargesValue(),
                product.getStatus().name(),
                LocalDateTime.now(),
                product.getId()
        );
    }

    public void delete(Long id) {
        String sql = "UPDATE public.loan_products SET status = 'ARCHIVED', updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), id);
    }
}
