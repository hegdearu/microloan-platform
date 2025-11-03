package in.zeta.microloan.platform.service.mappers;

import in.zeta.microloan.platform.dto.response.HouseholdResponseDTO;
import in.zeta.microloan.platform.model.Household;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HouseholdMapperTest {

    private HouseholdMapper householdMapper;

    @BeforeEach
    void setUp() {
        householdMapper = new HouseholdMapper();
    }

    @Test
    void testToResponse_Success() {
        Household household = Household.builder()
                .id(UUID.randomUUID())
                .householdNumber("HH-20251104-001234")
                .primaryAddress("123 Main St")
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(new BigDecimal("500000"))
                .totalMembers(4)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        HouseholdResponseDTO response = householdMapper.toResponse(household);

        assertNotNull(response);
        assertEquals(household.getId(), response.getId());
        assertEquals(household.getHouseholdNumber(), response.getHouseholdNumber());
        assertEquals(household.getPrimaryAddress(), response.getPrimaryAddress());
        assertEquals("Bangalore", response.getCity());
        assertEquals("Karnataka", response.getState());
        assertEquals(new BigDecimal("500000"), response.getTotalAnnualIncome());
        assertEquals(4, response.getTotalMembers());
        assertTrue(response.getIsVerified());
    }
}