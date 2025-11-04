package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.BorrowerRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.request.BorrowerUpdateRequestDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.Error;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.Household;
import in.zeta.microloan.platform.model.enums.UserStatus;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowerValidatorTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private HouseholdRepository householdRepository;

    @InjectMocks
    private BorrowerValidator validator;

    private BorrowerRegistrationRequestDTO registrationDTO;
    private UUID householdId;

    @BeforeEach
    void setUp() {
        householdId = UUID.randomUUID();
        ReflectionTestUtils.setField(validator, "minAgeRequirement", 18);

        registrationDTO = BorrowerRegistrationRequestDTO.builder()
                .name("Test Borrower")
                .phone("9876543210")
                .email("test@example.com")
                .dob(LocalDate.of(1990, 1, 1))
                .householdId(householdId)
                .build();
    }

    @Test
    void validateRegistration_WithFullHousehold_ShouldThrowException() {
        Household household = Household.builder()
                .id(householdId)
                .isVerified(true)
                .totalMembers(2)
                .build();

        when(borrowerRepository.findByPhone(registrationDTO.getPhone()))
                .thenReturn(Optional.empty());
        when(householdRepository.findById(householdId))
                .thenReturn(Optional.of(household));
        when(borrowerRepository.findByHouseholdId(householdId))
                .thenReturn(Arrays.asList(new Borrower(), new Borrower())); // Already 2 members

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateRegistration(registrationDTO)
        );

        assertTrue(exception.getMessage().contains("Cannot add more members"));
    }

    @Test
    void validateRegistration_WithoutEmail_ShouldPass() {
        registrationDTO.setEmail(null);

        Household household = Household.builder()
                .id(householdId)
                .isVerified(true)
                .totalMembers(5)
                .build();

        when(borrowerRepository.findByPhone(registrationDTO.getPhone()))
                .thenReturn(Optional.empty());
        when(householdRepository.findById(householdId))
                .thenReturn(Optional.of(household));
        when(borrowerRepository.findByHouseholdId(householdId))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> validator.validateRegistration(registrationDTO));
    }

    @Test
    void validateRegistration_WithUnderageUser_ShouldThrowException() {
        registrationDTO.setDob(LocalDate.now().minusYears(17));

        ValidationException exception = assertThrows(ValidationException.class, () ->
                validator.validateRegistration(registrationDTO)
        );

        assertTrue(exception.getMessage().contains("at least"));
    }

    @Test
    void validateRegistration_WithExistingPhone_ShouldThrowException() {
        Borrower existingBorrower = new Borrower();
        when(borrowerRepository.findByPhone(registrationDTO.getPhone()))
                .thenReturn(Optional.of(existingBorrower));

        ValidationException exception = assertThrows(ValidationException.class, () ->
                validator.validateRegistration(registrationDTO)
        );

        assertTrue(exception.getMessage().contains("already registered"));
    }

    @Test
    void validateRegistration_WithNonExistentHousehold_ShouldThrowException() {
        when(borrowerRepository.findByPhone(registrationDTO.getPhone()))
                .thenReturn(Optional.empty());
        when(householdRepository.findById(householdId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                validator.validateRegistration(registrationDTO)
        );

        assertTrue(exception.getMessage().contains(Error.HOUSEHOLD_NOT_FOUND.name()));
    }

    @Test
    void registerBorrower_WithUnverifiedHousehold_ShouldThrowException() {
        Household household = Household.builder()
                .id(householdId)
                .isVerified(false)
                .totalMembers(5)
                .build();

        when(borrowerRepository.findByPhone(registrationDTO.getPhone()))
                .thenReturn(Optional.empty());
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

        assertThrows(ResourceNotFoundException.class, () ->
                validator.validateRegistration(registrationDTO)
        );
    }

    @Test
    void validateUpdate_WithInvalidEmail_ShouldThrowException() {
        BorrowerUpdateRequestDTO updateDTO = BorrowerUpdateRequestDTO.builder()
                .email("invalid-email")
                .build();

        ValidationException exception = assertThrows(ValidationException.class, () ->
                validator.validateUpdate(updateDTO)
        );

        assertTrue(exception.getMessage().contains("Invalid email"));
    }

    @Test
    void validateVerification_WithAlreadyVerified_ShouldThrowException() {
        Borrower borrower = Borrower.builder()
                .id(UUID.randomUUID())
                .isVerified(true)
                .build();

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateVerification(borrower)
        );

        assertTrue(exception.getMessage().contains("already verified"));
    }

    @Test
    void validateStatusChange_WithValidStatus_ShouldReturnStatus() {
        UUID borrowerId = UUID.randomUUID();
        when(borrowerRepository.countActiveLoansByBorrower(borrowerId)).thenReturn(0);

        UserStatus result = validator.validateStatusChange(borrowerId, "SUSPENDED");

        assertEquals(UserStatus.SUSPENDED, result);
    }

    @Test
    void validateStatusChange_WithInvalidStatus_ShouldThrowException() {
        UUID borrowerId = UUID.randomUUID();

        ValidationException exception = assertThrows(ValidationException.class, () ->
                validator.validateStatusChange(borrowerId, "INVALID_STATUS")
        );

        assertTrue(exception.getMessage().contains("Invalid status"));
    }

    @Test
    void validateStatusChange_WithActiveLoans_ShouldThrowException() {
        UUID borrowerId = UUID.randomUUID();
        when(borrowerRepository.countActiveLoansByBorrower(borrowerId)).thenReturn(2);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateStatusChange(borrowerId, "SUSPENDED")
        );

        assertTrue(exception.getMessage().contains("active loan"));
    }

    @Test
    void validateDeletion_WithActiveLoans_ShouldThrowException() {
        UUID borrowerId = UUID.randomUUID();
        when(borrowerRepository.countActiveLoansByBorrower(borrowerId)).thenReturn(1);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateDeletion(borrowerId)
        );

        assertTrue(exception.getMessage().contains("active loans"));
    }

    @Test
    void validateDeletion_WithoutActiveLoans_ShouldPass() {
        UUID borrowerId = UUID.randomUUID();
        when(borrowerRepository.countActiveLoansByBorrower(borrowerId)).thenReturn(0);

        assertDoesNotThrow(() -> validator.validateDeletion(borrowerId));
    }
}