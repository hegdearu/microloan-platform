package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.BorrowerCreditSummaryResponseDTO;
import in.zeta.microloan.platform.dto.response.BorrowerResponseDTO;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BorrowerMapperTest {

    private BorrowerMapper borrowerMapper;
    private Borrower borrower;

    @BeforeEach
    void setUp() {
        borrowerMapper = new BorrowerMapper();
        borrower = Borrower.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .phone("9876543210")
                .email("john@example.com")
                .dob(LocalDate.of(1990, 1, 1))
                .householdId(UUID.randomUUID())
                .relationshipToHead("SELF")
                .isHouseholdHead(true)
                .individualAnnualIncome(new BigDecimal("500000"))
                .occupation("Software Engineer")
                .address("123 Main St")
                .idProofType("AADHAAR")
                .idProofNumber("1234-5678-9012")
                .status(UserStatus.ACTIVE)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testToResponse_Success() {
        BorrowerResponseDTO response = borrowerMapper.toResponse(borrower);

        assertNotNull(response);
        assertEquals(borrower.getId(), response.getId());
        assertEquals(borrower.getName(), response.getName());
        assertEquals(borrower.getPhone(), response.getPhone());
        assertEquals(borrower.getEmail(), response.getEmail());
        assertEquals(borrower.getDob(), response.getDob());
        assertTrue(response.getIsHouseholdHead());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
        assertTrue(response.getIsVerified());
    }

    @Test
    void testToCreditSummary_Success() {
        int totalLoans = 5;
        int activeLoans = 2;
        int closedLoans = 3;
        BigDecimal totalDisbursed = new BigDecimal("500000");
        BigDecimal totalOutstanding = new BigDecimal("100000");
        BigDecimal totalPaid = new BigDecimal("400000");

        BorrowerCreditSummaryResponseDTO summary = borrowerMapper.toCreditSummary(
                borrower, totalLoans, activeLoans, closedLoans,
                totalDisbursed, totalOutstanding, totalPaid
        );

        assertNotNull(summary);
        assertEquals(borrower.getId(), summary.getBorrowerId());
        assertEquals(borrower.getName(), summary.getBorrowerName());
        assertEquals(totalLoans, summary.getTotalLoans());
        assertEquals(activeLoans, summary.getActiveLoans());
        assertEquals(closedLoans, summary.getClosedLoans());
        assertEquals(totalDisbursed, summary.getTotalDisbursed());
        assertEquals(totalOutstanding, summary.getTotalOutstanding());
        assertEquals(totalPaid, summary.getTotalPaid());
        assertTrue(summary.getIsVerified());
        assertEquals("ACTIVE", summary.getStatus());
    }

    @Test
    void testToCreditSummary_NullValues() {
        BorrowerCreditSummaryResponseDTO summary = borrowerMapper.toCreditSummary(
                borrower, 0, 0, 0, null, null, null
        );

        assertNotNull(summary);
        assertEquals(BigDecimal.ZERO, summary.getTotalDisbursed());
        assertEquals(BigDecimal.ZERO, summary.getTotalOutstanding());
        assertEquals(BigDecimal.ZERO, summary.getTotalPaid());
    }
}