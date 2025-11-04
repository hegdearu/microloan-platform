package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.BorrowerRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.request.BorrowerUpdateRequestDTO;
import in.zeta.microloan.platform.dto.response.BorrowerCreditSummaryResponseDTO;
import in.zeta.microloan.platform.dto.response.BorrowerResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.Household;
import in.zeta.microloan.platform.model.enums.UserStatus;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.microloan.platform.service.mappers.BorrowerMapper;
import in.zeta.microloan.platform.service.validator.BorrowerValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowerServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private HouseholdRepository householdRepository;

    @Mock
    private BorrowerValidator validator;

    @Mock
    private BorrowerMapper mapper;

    @InjectMocks
    private BorrowerService borrowerService;

    private Borrower borrower;
    private BorrowerRegistrationRequestDTO registrationDTO;
    private BorrowerResponseDTO responseDTO;
    private UUID borrowerId;
    private UUID householdId;

    @BeforeEach
    void setUp() {
        borrowerId = UUID.randomUUID();
        householdId = UUID.randomUUID();

        borrower = Borrower.builder()
                .id(borrowerId)
                .name("Test Borrower")
                .phone("9876543210")
                .email("test@example.com")
                .dob(LocalDate.of(1990, 1, 1))
                .householdId(householdId)
                .relationshipToHead("Self")
                .isHouseholdHead(true)
                .individualAnnualIncome(new BigDecimal("500000"))
                .occupation("Engineer")
                .address("Test Address")
                .idProofType("AADHAAR")
                .idProofNumber("123456789012")
                .status(UserStatus.ACTIVE)
                .isVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        registrationDTO = BorrowerRegistrationRequestDTO.builder()
                .name("Test Borrower")
                .phone("9876543210")
                .email("test@example.com")
                .dob(LocalDate.of(1990, 1, 1))
                .householdId(householdId)
                .relationshipToHead("Self")
                .isHouseholdHead(true)
                .individualAnnualIncome(new BigDecimal("500000"))
                .occupation("Engineer")
                .address("Test Address")
                .idProofType("AADHAAR")
                .idProofNumber("123456789012")
                .build();

        responseDTO = BorrowerResponseDTO.builder()
                .id(borrowerId)
                .name("Test Borrower")
                .phone("9876543210")
                .status(UserStatus.ACTIVE)
                .isVerified(false)
                .build();
    }

    @Test
    void registerBorrower_WithValidData_ShouldSucceed() {
        Household household = Household.builder()
                .id(householdId)
                .isVerified(true)
                .build();

        doNothing().when(validator).validateRegistration(registrationDTO);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(borrowerRepository.create(any(Borrower.class))).thenReturn(borrower);
        when(mapper.toResponse(borrower)).thenReturn(responseDTO);

        BorrowerResponseDTO result = borrowerService.registerBorrower(registrationDTO);

        assertNotNull(result);
        assertEquals(borrowerId, result.getId());
        verify(validator).validateRegistration(registrationDTO);
        verify(borrowerRepository).create(any(Borrower.class));
    }

    @Test
    void registerBorrower_WithoutHousehold_ShouldSucceed() {
        registrationDTO.setHouseholdId(null);

        doNothing().when(validator).validateRegistration(registrationDTO);
        when(borrowerRepository.create(any(Borrower.class))).thenReturn(borrower);
        when(mapper.toResponse(borrower)).thenReturn(responseDTO);

        BorrowerResponseDTO result = borrowerService.registerBorrower(registrationDTO);

        assertNotNull(result);
        verify(validator).validateRegistration(registrationDTO);
    }

    @Test
    void registerBorrower_WithUnverifiedHousehold_ShouldThrowException() {
        Household household = Household.builder()
                .id(householdId)
                .isVerified(false)
                .build();

        doNothing().when(validator).validateRegistration(registrationDTO);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

        assertThrows(ResourceNotFoundException.class, () ->
                borrowerService.registerBorrower(registrationDTO)
        );
    }

    @Test
    void getBorrowerById_WhenExists_ShouldReturn() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(mapper.toResponse(borrower)).thenReturn(responseDTO);

        BorrowerResponseDTO result = borrowerService.getBorrowerById(borrowerId);

        assertNotNull(result);
        assertEquals(borrowerId, result.getId());
    }

    @Test
    void getBorrowerById_WhenNotExists_ShouldThrowException() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                borrowerService.getBorrowerById(borrowerId)
        );
    }

    @Test
    void getBorrowerByPhone_WhenExists_ShouldReturn() {
        String phone = "9876543210";
        when(borrowerRepository.findByPhone(phone)).thenReturn(Optional.of(borrower));
        when(mapper.toResponse(borrower)).thenReturn(responseDTO);

        BorrowerResponseDTO result = borrowerService.getBorrowerByPhone(phone);

        assertNotNull(result);
        assertEquals(phone, result.getPhone());
    }

    @Test
    void getBorrowersByHousehold_ShouldReturnList() {
        Household household = Household.builder().id(householdId).build();
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(borrowerRepository.findByHouseholdId(householdId)).thenReturn(Arrays.asList(borrower));
        when(mapper.toResponse(borrower)).thenReturn(responseDTO);

        List<BorrowerResponseDTO> result = borrowerService.getBorrowersByHousehold(householdId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAllBorrowers_WithStatus_ShouldReturnFiltered() {
        when(borrowerRepository.findByStatus(UserStatus.ACTIVE)).thenReturn(Arrays.asList(borrower));
        when(mapper.toResponse(borrower)).thenReturn(responseDTO);

        List<BorrowerResponseDTO> result = borrowerService.getAllBorrowers("ACTIVE", 1, 20);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAllBorrowers_WithoutStatus_ShouldReturnAll() {
        when(borrowerRepository.findAll()).thenReturn(Arrays.asList(borrower));
        when(mapper.toResponse(borrower)).thenReturn(responseDTO);

        List<BorrowerResponseDTO> result = borrowerService.getAllBorrowers(null, 1, 20);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void updateBorrower_ShouldUpdateFields() {
        BorrowerUpdateRequestDTO updateDTO = BorrowerUpdateRequestDTO.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        doNothing().when(validator).validateUpdate(updateDTO);
        doNothing().when(borrowerRepository).update(any(Borrower.class));
        when(mapper.toResponse(borrower)).thenReturn(responseDTO);

        BorrowerResponseDTO result = borrowerService.updateBorrower(borrowerId, updateDTO);

        assertNotNull(result);
        verify(borrowerRepository).update(any(Borrower.class));
    }

    @Test
    void verifyBorrower_ShouldSetVerifiedTrue() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        doNothing().when(validator).validateVerification(borrower);
        doNothing().when(borrowerRepository).update(borrower);
        when(mapper.toResponse(borrower)).thenReturn(responseDTO);

        BorrowerResponseDTO result = borrowerService.verifyBorrower(borrowerId);

        assertNotNull(result);
        assertTrue(borrower.getIsVerified());
        verify(borrowerRepository).update(borrower);
    }

    @Test
    void updateBorrowerStatus_ShouldUpdateStatus() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(validator.validateStatusChange(borrowerId, "SUSPENDED"))
                .thenReturn(UserStatus.SUSPENDED);
        doNothing().when(borrowerRepository).update(borrower);
        when(mapper.toResponse(borrower)).thenReturn(responseDTO);

        BorrowerResponseDTO result = borrowerService.updateBorrowerStatus(borrowerId, "SUSPENDED");

        assertNotNull(result);
        verify(borrowerRepository).update(borrower);
    }

    @Test
    void deleteBorrower_ShouldDelete() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        doNothing().when(validator).validateDeletion(borrowerId);
        doNothing().when(borrowerRepository).delete(borrowerId);

        borrowerService.deleteBorrower(borrowerId);

        verify(borrowerRepository).delete(borrowerId);
    }

    @Test
    void getBorrowerCreditSummary_ShouldReturnSummary() {
        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(borrowerRepository.countAllLoansByBorrower(borrowerId)).thenReturn(5);
        when(borrowerRepository.countActiveLoansByBorrower(borrowerId)).thenReturn(2);
        when(borrowerRepository.countClosedLoansByBorrower(borrowerId)).thenReturn(3);
        when(borrowerRepository.getTotalDisbursedAmount(borrowerId))
                .thenReturn(new BigDecimal("500000"));
        when(borrowerRepository.getTotalOutstandingAmount(borrowerId))
                .thenReturn(new BigDecimal("200000"));
        when(borrowerRepository.getTotalPaidAmount(borrowerId))
                .thenReturn(new BigDecimal("300000"));

        BorrowerCreditSummaryResponseDTO summaryDTO = BorrowerCreditSummaryResponseDTO.builder()
                .borrowerId(borrowerId)
                .borrowerName("Test Borrower")
                .totalLoans(5)
                .activeLoans(2)
                .closedLoans(3)
                .totalDisbursed(new BigDecimal("500000"))
                .totalOutstanding(new BigDecimal("200000"))
                .totalPaid(new BigDecimal("300000"))
                .isVerified(false)
                .status("ACTIVE")
                .build();

        when(mapper.toCreditSummary(any(), anyInt(), anyInt(), anyInt(),
                any(), any(), any())).thenReturn(summaryDTO);

        BorrowerCreditSummaryResponseDTO result = borrowerService.getBorrowerCreditSummary(borrowerId);

        assertNotNull(result);
        assertEquals(5, result.getTotalLoans());
        assertEquals(2, result.getActiveLoans());
    }
}