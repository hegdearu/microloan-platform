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
import java.util.UUID;

@Repository
public class HouseholdRepository {

    private final JdbcTemplate jdbcTemplate;

    public HouseholdRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Household> rowMapper = (rs, rowNum) -> Household.builder()
            .id(rs.getObject("id", UUID.class))
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

    public Household create(Household household) {
        String sql = """
        INSERT INTO public.households (
            household_number, primary_address, pincode, city, state,
            total_annual_income, income_proof_type, total_members, household_type, is_verified
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id, created_at, updated_at
    """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                    household.setId(rs.getObject("id", UUID.class));
                    household.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    household.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return household;
                },
                household.getHouseholdNumber(),
                household.getPrimaryAddress(),
                household.getPincode(),
                household.getCity(),
                household.getState(),
                household.getTotalAnnualIncome(),
                household.getIncomeProofType(),
                household.getTotalMembers(),
                household.getHouseholdType(),
                household.getIsVerified());
    }

    public Optional<Household> findById(UUID id) {
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
