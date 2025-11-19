package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.HouseholdRegistrationRequestDTO;
import in.zeta.microloan.platform.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class HouseholdValidatorTest {

    private HouseholdValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HouseholdValidator();
    }

    @Test
    void testValidateRegistration_Success() {
        HouseholdRegistrationRequestDTO dto = HouseholdRegistrationRequestDTO.builder()
                .primaryAddress("123 Main St")
                .pincode("560001")
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(new BigDecimal("500000"))
                .incomeProofType("SALARY_SLIP")
                .householdType("NUCLEAR")
                .build();

        assertDoesNotThrow(() -> validator.validateRegistration(dto));
    }

    @Test
    void testValidateRegistration_NegativeIncome() {
        HouseholdRegistrationRequestDTO dto = HouseholdRegistrationRequestDTO.builder()
                .primaryAddress("123 Main St")
                .pincode("560001")
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(new BigDecimal("-1000"))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validateRegistration(dto)
        );
        assertEquals("Total annual income must be positive", exception.getMessage());
    }

    @Test
    void testValidateRegistration_ZeroIncome() {
        HouseholdRegistrationRequestDTO dto = HouseholdRegistrationRequestDTO.builder()
                .primaryAddress("123 Main St")
                .pincode("560001")
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(BigDecimal.ZERO)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validateRegistration(dto)
        );
        assertEquals("Total annual income must be positive", exception.getMessage());
    }

    @Test
    void testValidateRegistration_NullPincode() {
        HouseholdRegistrationRequestDTO dto = HouseholdRegistrationRequestDTO.builder()
                .primaryAddress("123 Main St")
                .pincode(null)
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(new BigDecimal("500000"))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validateRegistration(dto)
        );
        assertEquals("Invalid pincode. Must be 6 digits", exception.getMessage());
    }

    @Test
    void testValidateRegistration_InvalidPincode_TooShort() {
        HouseholdRegistrationRequestDTO dto = HouseholdRegistrationRequestDTO.builder()
                .primaryAddress("123 Main St")
                .pincode("56001")
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(new BigDecimal("500000"))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validateRegistration(dto)
        );
        assertEquals("Invalid pincode. Must be 6 digits", exception.getMessage());
    }

    @Test
    void testValidateRegistration_InvalidPincode_WithLetters() {
        HouseholdRegistrationRequestDTO dto = HouseholdRegistrationRequestDTO.builder()
                .primaryAddress("123 Main St")
                .pincode("56000A")
                .city("Bangalore")
                .state("Karnataka")
                .totalAnnualIncome(new BigDecimal("500000"))
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validateRegistration(dto)
        );
        assertEquals("Invalid pincode. Must be 6 digits", exception.getMessage());
    }
}