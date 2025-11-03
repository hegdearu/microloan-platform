package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.HouseholdRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.response.HouseholdResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.Household;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.microloan.platform.service.mappers.HouseholdMapper;
import in.zeta.microloan.platform.service.validator.HouseholdValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HouseholdServiceTest {

    @Mock
    private HouseholdRepository householdRepository;

    @Mock
    private HouseholdValidator validator;

    @Mock
    private HouseholdMapper mapper;

    @InjectMocks
    private HouseholdService householdService;

    private UUID householdId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        householdId = UUID.randomUUID();
    }

    @Test
    void testCreateHousehold_Success() {
        // Arrange
        HouseholdRegistrationRequestDTO dto = HouseholdRegistrationRequestDTO.builder()
                .primaryAddress("123 Main St")
                .pincode("560001")
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(new BigDecimal("500000"))
                .incomeProofType("SALARY_SLIP")
                .householdType("NUCLEAR")
                .build();

        Household household = Household.builder()
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

        HouseholdResponseDTO responseDTO = HouseholdResponseDTO.builder()
                .id(householdId)
                .householdNumber("HH-20251104-001234")
                .city("Bangalore")
                .build();

        doNothing().when(validator).validateRegistration(any());
        when(householdRepository.create(any(Household.class))).thenReturn(household);
        when(mapper.toResponse(any())).thenReturn(responseDTO);

        // Act
        HouseholdResponseDTO result = householdService.createHousehold(dto);

        // Assert
        assertNotNull(result);
        assertEquals(householdId, result.getId());
        verify(validator, times(1)).validateRegistration(dto);
        verify(householdRepository, times(1)).create(any(Household.class));
    }

    @Test
    void testGetHouseholdById_Success() {
        // Arrange
        Household household = Household.builder()
                .id(householdId)
                .householdNumber("HH-20251104-001234")
                .city("Bangalore")
                .build();

        HouseholdResponseDTO responseDTO = HouseholdResponseDTO.builder()
                .id(householdId)
                .householdNumber("HH-20251104-001234")
                .build();

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(mapper.toResponse(household)).thenReturn(responseDTO);

        // Act
        HouseholdResponseDTO result = householdService.getHouseholdById(householdId);

        // Assert
        assertNotNull(result);
        assertEquals(householdId, result.getId());
        verify(householdRepository, times(1)).findById(householdId);
    }

    @Test
    void testGetHouseholdById_NotFound() {
        // Arrange
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> householdService.getHouseholdById(householdId));
        verify(householdRepository, times(1)).findById(householdId);
    }

    @Test
    void testVerifyHousehold_Success() {
        // Arrange
        Household household = Household.builder()
                .id(householdId)
                .householdNumber("HH-20251104-001234")
                .isVerified(false)
                .build();

        HouseholdResponseDTO responseDTO = HouseholdResponseDTO.builder()
                .id(householdId)
                .isVerified(true)
                .build();

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        doNothing().when(householdRepository).update(any());
        when(mapper.toResponse(any())).thenReturn(responseDTO);

        // Act
        HouseholdResponseDTO result = householdService.verifyHousehold(householdId);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsVerified());
        verify(householdRepository, times(1)).update(any());
    }

    @Test
    void testVerifyHousehold_AlreadyVerified() {
        // Arrange
        Household household = Household.builder()
                .id(householdId)
                .householdNumber("HH-20251104-001234")
                .isVerified(true)
                .build();

        HouseholdResponseDTO responseDTO = HouseholdResponseDTO.builder()
                .id(householdId)
                .isVerified(true)
                .build();

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(mapper.toResponse(household)).thenReturn(responseDTO);

        // Act
        HouseholdResponseDTO result = householdService.verifyHousehold(householdId);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsVerified());
        verify(householdRepository, never()).update(any());
    }
}
