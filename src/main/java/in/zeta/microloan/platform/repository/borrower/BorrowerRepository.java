package in.zeta.microloan.platform.repository.borrower;

import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.enums.UserStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BorrowerRepository {

    private final JdbcTemplate jdbcTemplate;

    public BorrowerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Borrower> rowMapper = (rs, rowNum) -> Borrower.builder()
            .id(rs.getObject("id", UUID.class))
            .name(rs.getString("name"))
            .phone(rs.getString("phone"))
            .email(rs.getString("email"))
            .dob(rs.getDate("dob").toLocalDate())
            .householdId(rs.getObject("household_id", UUID.class))
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
            .status(UserStatus.valueOf(rs.getString("status")))
            .isVerified(rs.getBoolean("is_verified"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public Borrower create(Borrower borrower) {
        String sql = """
        INSERT INTO public.borrowers (
            name, phone, email, dob, household_id,
            relationship_to_head, is_household_head, individual_annual_income, occupation,
            address, id_proof_type, id_proof_number, employment_details, income_details,
            status, is_verified
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id, created_at, updated_at
    """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                    borrower.setId(rs.getObject("id", UUID.class));
                    borrower.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    borrower.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return borrower;
                },
                borrower.getName(),
                borrower.getPhone(),
                borrower.getEmail(),
                java.sql.Date.valueOf(borrower.getDob()),
                borrower.getHouseholdId(),
                borrower.getRelationshipToHead(),
                borrower.getIsHouseholdHead(),
                borrower.getIndividualAnnualIncome(),
                borrower.getOccupation(),
                borrower.getAddress(),
                borrower.getIdProofType(),
                borrower.getIdProofNumber(),
                borrower.getEmploymentDetails(),
                borrower.getIncomeDetails(),
                borrower.getStatus().name(),
                borrower.getIsVerified());
    }

    public void update(Borrower borrower) {
        String sql = "UPDATE public.borrowers SET name = ?, email = ?, address = ?, " +
                "occupation = ?, individual_annual_income = ?, employment_details = ?, " +
                "income_details = ?, status = ?, is_verified = ?, " +
                "updated_at = ? WHERE id = ?";

        jdbcTemplate.update(sql,
                borrower.getName(),
                borrower.getEmail(),
                borrower.getAddress(),
                borrower.getOccupation(),
                borrower.getIndividualAnnualIncome(),
                borrower.getEmploymentDetails(),
                borrower.getIncomeDetails(),
                borrower.getStatus().name(),
                borrower.getIsVerified(),
                LocalDateTime.now(),
                borrower.getId()
        );
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM public.borrowers WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public Optional<Borrower> findById(UUID id) {
        String sql = "SELECT * FROM public.borrowers WHERE id = ?";
        List<Borrower> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Borrower> findByPhone(String phone) {
        String sql = "SELECT * FROM public.borrowers WHERE phone = ?";
        List<Borrower> results = jdbcTemplate.query(sql, rowMapper, phone);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Borrower> findByHouseholdId(UUID householdId) {
        String sql = "SELECT * FROM public.borrowers WHERE household_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, householdId);
    }

    public List<Borrower> findByStatus(UserStatus status) {
        String sql = "SELECT * FROM public.borrowers WHERE status = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }

    public List<Borrower> findAll() {
        String sql = "SELECT * FROM public.borrowers ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public int countActiveLoansByBorrower(UUID borrowerId) {
        String sql = "SELECT COUNT(*) FROM public.loans WHERE borrower_id = ? " +
                "AND status IN ('ACTIVE', 'OVERDUE')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, borrowerId);
        return count != null ? count : 0;
    }

    public int countAllLoansByBorrower(UUID borrowerId) {
        String sql = "SELECT COUNT(*) FROM public.loans WHERE borrower_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, borrowerId);
        return count != null ? count : 0;
    }

    public int countClosedLoansByBorrower(UUID borrowerId) {
        String sql = "SELECT COUNT(*) FROM public.loans WHERE borrower_id = ? AND status = 'CLOSED'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, borrowerId);
        return count != null ? count : 0;
    }

    public BigDecimal getTotalDisbursedAmount(UUID borrowerId) {
        String sql = "SELECT COALESCE(SUM(principal_amount), 0) FROM public.loans " +
                "WHERE borrower_id = ?";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, borrowerId);
    }

    public BigDecimal getTotalOutstandingAmount(UUID borrowerId) {
        String sql = "SELECT COALESCE(SUM(total_outstanding), 0) FROM public.loans " +
                "WHERE borrower_id = ? AND status IN ('ACTIVE', 'OVERDUE')";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, borrowerId);
    }

    public BigDecimal getTotalPaidAmount(UUID borrowerId) {
        String sql = "SELECT COALESCE(SUM(total_paid), 0) FROM public.loans " +
                "WHERE borrower_id = ?";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, borrowerId);
    }
}