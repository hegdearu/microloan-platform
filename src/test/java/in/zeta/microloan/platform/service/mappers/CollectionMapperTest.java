package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.CollectionActivityResponseDTO;
import in.zeta.microloan.platform.dto.response.OverdueLoansResponseDTO;
import in.zeta.microloan.platform.model.*;
import in.zeta.microloan.platform.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CollectionMapperTest {

    private CollectionMapper collectionMapper;

    @BeforeEach
    void setUp() {
        collectionMapper = new CollectionMapper();
    }

    @Test
    void testToActivityResponse_Success() {
        CollectionActivity activity = CollectionActivity.builder()
                .id(UUID.randomUUID())
                .loanId(UUID.randomUUID())
                .activityType("REMINDER")
                .contactMethod(ContactMethod.PHONE)
                .borrowerResponse("Will pay soon")
                .promiseToPayDate(LocalDate.now().plusDays(5))
                .notes("Borrower agreed to pay")
                .activityDate(LocalDateTime.now())
                .nextFollowUpDate(LocalDate.now().plusDays(7))
                .build();

        CollectionActivityResponseDTO response = collectionMapper.toActivityResponse(activity);

        assertNotNull(response);
        assertEquals(activity.getId(), response.getId());
        assertEquals(activity.getLoanId(), response.getLoanId());
        assertEquals(activity.getActivityType(), response.getActivityType());
        assertEquals(ContactMethod.PHONE, response.getContactMethod());
        assertEquals("Will pay soon", response.getBorrowerResponse());
    }

    @Test
    void testToOverdueResponse_Success() {
        UUID loanId = UUID.randomUUID();
        UUID borrowerId = UUID.randomUUID();

        OverdueTracking overdue = OverdueTracking.builder()
                .id(UUID.randomUUID())
                .loanId(loanId)
                .overdueSince(LocalDate.now().minusDays(10))
                .overdueDays(10)
                .overdueAmount(new BigDecimal("5000"))
                .penaltyAmount(new BigDecimal("100"))
                .totalDue(new BigDecimal("5100"))
                .collectionStage(CollectionStage.REGULAR_FOLLOWUP)
                .build();

        Loan loan = Loan.builder()
                .id(loanId)
                .loanNumber("LN-2025-001")
                .borrowerId(borrowerId)
                .build();

        Borrower borrower = Borrower.builder()
                .id(borrowerId)
                .name("John Doe")
                .phone("9876543210")
                .build();

        OverdueLoansResponseDTO response = collectionMapper.toOverdueResponse(overdue, loan, borrower);

        assertNotNull(response);
        assertEquals(loanId, response.getLoanId());
        assertEquals("LN-2025-001", response.getLoanNumber());
        assertEquals(borrowerId, response.getBorrowerId());
        assertEquals("John Doe", response.getBorrowerName());
        assertEquals("9876543210", response.getBorrowerPhone());
        assertEquals(10, response.getOverdueDays());
        assertEquals(new BigDecimal("5100"), response.getTotalDue());
        assertEquals(CollectionStage.REGULAR_FOLLOWUP, response.getCollectionStage());
    }
}