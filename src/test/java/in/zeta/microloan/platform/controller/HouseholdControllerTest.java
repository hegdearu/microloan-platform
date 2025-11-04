package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.HouseholdRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.response.HouseholdResponseDTO;
import in.zeta.microloan.platform.service.HouseholdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HouseholdControllerTest {

    @Mock
    private HouseholdService householdService;

    @InjectMocks
    private HouseholdController householdController;

    private UUID householdId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        householdId = UUID.randomUUID();
    }

    @Test
    void testCreateHousehold_Success() {
        // Arrange
        HouseholdRegistrationRequestDTO requestDTO = HouseholdRegistrationRequestDTO.builder()
                .primaryAddress("123 Main St")
                .pincode("560001")
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(new BigDecimal("500000"))
                .incomeProofType("SALARY_SLIP")
                .householdType("NUCLEAR")
                .build();

        HouseholdResponseDTO responseDTO = HouseholdResponseDTO.builder()
                .id(householdId)
                .householdNumber("HH-20251104-001234")
                .primaryAddress("123 Main St")
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(new BigDecimal("500000"))
                .totalMembers(1)
                .isVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(householdService.createHousehold(any(HouseholdRegistrationRequestDTO.class)))
                .thenReturn(responseDTO);

        // Act
        ResponseEntity<HouseholdResponseDTO> response = householdController.createHousehold(requestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(householdId, response.getBody().getId());
        assertEquals("Bangalore", response.getBody().getCity());
        assertFalse(response.getBody().getIsVerified());
        verify(householdService, times(1)).createHousehold(any(HouseholdRegistrationRequestDTO.class));
    }

    @Test
    void testVerifyHousehold_Success() {
        // Arrange
        HouseholdResponseDTO responseDTO = HouseholdResponseDTO.builder()
                .id(householdId)
                .householdNumber("HH-20251104-001234")
                .primaryAddress("123 Main St")
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(new BigDecimal("500000"))
                .totalMembers(1)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(householdService.verifyHousehold(householdId)).thenReturn(responseDTO);

        // Act
        ResponseEntity<HouseholdResponseDTO> response = householdController.verifyHousehold(householdId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(householdId, response.getBody().getId());
        assertTrue(response.getBody().getIsVerified());
        verify(householdService, times(1)).verifyHousehold(householdId);
    }

    @Test
    void testGetHousehold_Success() {
        // Arrange
        HouseholdResponseDTO responseDTO = HouseholdResponseDTO.builder()
                .id(householdId)
                .householdNumber("HH-20251104-001234")
                .primaryAddress("123 Main St")
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(new BigDecimal("500000"))
                .totalMembers(3)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(householdService.getHouseholdById(householdId)).thenReturn(responseDTO);

        // Act
        ResponseEntity<HouseholdResponseDTO> response = householdController.getHousehold(householdId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(householdId, response.getBody().getId());
        assertEquals(3, response.getBody().getTotalMembers());
        assertEquals("Bangalore", response.getBody().getCity());
        verify(householdService, times(1)).getHouseholdById(householdId);
    }
}