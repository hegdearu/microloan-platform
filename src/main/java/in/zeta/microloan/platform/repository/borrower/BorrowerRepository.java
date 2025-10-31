package in.zeta.microloan.platform.repository.borrower;

import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.UserStatus;
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
public class BorrowerRepository {

    private final JdbcTemplate jdbcTemplate;

    public BorrowerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Borrower> rowMapper = (rs, rowNum) -> Borrower.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .phone(rs.getString("phone"))
            .email(rs.getString("email"))
            .dob(rs.getDate("dob").toLocalDate())
            .householdId(rs.getObject("household_id", Long.class))
            .relationshipToHead(rs.getString("relationship_to_head"))
            .isHouseholdHead(rs.getBoolean("is_household_head"))
            .individualAnnualIncome(rs.getBigDecimal("individual_annual_income"))
            .occupation(rs.getString("occupation"))
            .address(rs.getString("address"))
            .idProofType(rs.getString("id_proof_type"))
            .idProofNumber(rs.getString("id_proof_number"))
            .employmentDetails(rs.getString("employment_details"))
            .incomeDetails(rs.getString("income_details"))
            .profilePhotoUrl(rs.getString("profile_photo_url"))
            .creditScore(rs.getObject("credit_score", Integer.class))
            .status(UserStatus.valueOf(rs.getString("status")))
            .isVerified(rs.getBoolean("is_verified"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public Long create(Borrower borrower) {
        String sql = "INSERT INTO public.borrowers (name, phone, email, dob, household_id, " +
                "relationship_to_head, is_household_head, individual_annual_income, occupation, " +
                "address, id_proof_type, id_proof_number, employment_details, income_details, " +
                "status, is_verified, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, borrower.getName());
            ps.setString(2, borrower.getPhone());
            ps.setString(3, borrower.getEmail());
            ps.setDate(4, java.sql.Date.valueOf(borrower.getDob()));
            ps.setObject(5, borrower.getHouseholdId());
            ps.setString(6, borrower.getRelationshipToHead());
            ps.setBoolean(7, borrower.getIsHouseholdHead());
            ps.setBigDecimal(8, borrower.getIndividualAnnualIncome());
            ps.setString(9, borrower.getOccupation());
            ps.setString(10, borrower.getAddress());
            ps.setString(11, borrower.getIdProofType());
            ps.setString(12, borrower.getIdProofNumber());
            ps.setString(13, borrower.getEmploymentDetails());
            ps.setString(14, borrower.getIncomeDetails());
            ps.setString(15, borrower.getStatus().name());
            ps.setBoolean(16, borrower.getIsVerified());
            ps.setTimestamp(17, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(18, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        Object idObj = keyHolder.getKeys().get("id");
        if (idObj instanceof Number) {
            return ((Number) idObj).longValue();
        } else {
            throw new IllegalStateException("Failed to retrieve generated borrower id");
        }
    }

    public Optional<Borrower> findById(Long id) {
        String sql = "SELECT * FROM public.borrowers WHERE id = ?";
        List<Borrower> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Borrower> findByPhone(String phone) {
        String sql = "SELECT * FROM public.borrowers WHERE phone = ?";
        List<Borrower> results = jdbcTemplate.query(sql, rowMapper, phone);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Borrower> findByHouseholdId(Long householdId) {
        String sql = "SELECT * FROM public.borrowers WHERE household_id = ?";
        return jdbcTemplate.query(sql, rowMapper, householdId);
    }

    public int countActiveLoansByBorrower(Long borrowerId) {
        String sql = "SELECT COUNT(*) FROM public.loans WHERE borrower_id = ? AND status IN ('ACTIVE', 'OVERDUE')";
        return jdbcTemplate.queryForObject(sql, Integer.class, borrowerId);
    }

    public int countActiveLoansByHousehold(Long householdId) {
        String sql = "SELECT COUNT(*) FROM public.loans WHERE household_id = ? AND status IN ('ACTIVE', 'OVERDUE')";
        return jdbcTemplate.queryForObject(sql, Integer.class, householdId);
    }
}