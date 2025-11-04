package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.LoanApplicationResponseDTO;
import in.zeta.microloan.platform.model.LoanApplication;
import in.zeta.microloan.platform.model.enums.LoanApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoanApplicationMapperTest {

    private LoanApplicationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LoanApplicationMapper();
    }

    @Test
    void testToResponse_Success() {
        LoanApplication application = LoanApplication.builder()
                .id(UUID.randomUUID())
                .applicationNumber("APP-20251104-001234")
                .borrowerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .requestedAmount(new BigDecimal("50000"))
                .purpose("Business expansion")
                .preferredTenure(12)
                .status(LoanApplicationStatus.APPROVED)
                .approvedAmount(new BigDecimal("45000"))
                .approvedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        LoanApplicationResponseDTO response = mapper.toResponse(application);

        assertNotNull(response);
        assertEquals(application.getId(), response.getId());
        assertEquals("APP-20251104-001234", response.getApplicationNumber());
        assertEquals(application.getBorrowerId(), response.getBorrowerId());
        assertEquals(new BigDecimal("50000"), response.getRequestedAmount());
        assertEquals("APPROVED", response.getStatus());
        assertEquals(new BigDecimal("45000"), response.getApprovedAmount());
        assertEquals(12, response.getPreferredTenure());
    }
}