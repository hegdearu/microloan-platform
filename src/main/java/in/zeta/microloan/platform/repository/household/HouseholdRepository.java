package in.zeta.microloan.platform.repository.household;

import in.zeta.microloan.platform.model.Household;
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
public class HouseholdRepository {

    private final JdbcTemplate jdbcTemplate;

    public HouseholdRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Household> rowMapper = (rs, rowNum) -> Household.builder()
            .id(rs.getLong("id"))
            .householdNumber(rs.getString("household_number"))
            .primaryAddress(rs.getString("primary_address"))
            .pincode(rs.getString("pincode"))
            .city(rs.getString("city"))
            .state(rs.getString("state"))
            .totalAnnualIncome(rs.getBigDecimal("total_annual_income"))
            .incomeProofType(rs.getString("income_proof_type"))
            .incomeProofUrl(rs.getString("income_proof_url"))
            .incomeVerifiedDate(rs.getDate("income_verified_date") != null ?
                    rs.getDate("income_verified_date").toLocalDate() : null)
            .totalMembers(rs.getInt("total_members"))
            .householdType(rs.getString("household_type"))
            .isVerified(rs.getBoolean("is_verified"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public Long create(Household household) {
        String sql = "INSERT INTO public.households (household_number, primary_address, pincode, city, state, " +
                "total_annual_income, income_proof_type, total_members, household_type, is_verified, " +
                "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, household.getHouseholdNumber());
            ps.setString(2, household.getPrimaryAddress());
            ps.setString(3, household.getPincode());
            ps.setString(4, household.getCity());
            ps.setString(5, household.getState());
            ps.setBigDecimal(6, household.getTotalAnnualIncome());
            ps.setString(7, household.getIncomeProofType());
            ps.setInt(8, household.getTotalMembers());
            ps.setString(9, household.getHouseholdType());
            ps.setBoolean(10, household.getIsVerified());
            ps.setTimestamp(11, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(12, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        return ((Number) keyHolder.getKeys().get("id")).longValue();
    }

    public Optional<Household> findById(Long id) {
        String sql = "SELECT * FROM public.households WHERE id = ?";
        List<Household> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void update(Household household) {
        String sql = "UPDATE public.households SET total_annual_income = ?, income_proof_type = ?, " +
                "income_proof_url = ?, income_verified_date = ?, total_members = ?, " +
                "is_verified = ?, updated_at = ? WHERE id = ?";

        jdbcTemplate.update(sql,
                household.getTotalAnnualIncome(),
                household.getIncomeProofType(),
                household.getIncomeProofUrl(),
                household.getIncomeVerifiedDate(),
                household.getTotalMembers(),
                household.getIsVerified(),
                LocalDateTime.now(),
                household.getId()
        );
    }
}
