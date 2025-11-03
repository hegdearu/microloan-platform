package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.LoanProductRequestDTO;
import in.zeta.microloan.platform.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LoanProductValidatorTest {

    private LoanProductValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LoanProductValidator();
    }

    @Test
    void testValidate_Success() {
        LoanProductRequestDTO dto = LoanProductRequestDTO.builder()
                .name("Personal Loan")
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12.5"))
                .tenureMonths(12)
                .build();

        assertDoesNotThrow(() -> validator.validate(dto));
    }

    @Test
    void testValidate_MinGreaterThanMax() {
        LoanProductRequestDTO dto = LoanProductRequestDTO.builder()
                .name("Personal Loan")
                .minAmount(new BigDecimal("100000"))
                .maxAmount(new BigDecimal("10000"))
                .interestRate(new BigDecimal("12.5"))
                .tenureMonths(12)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(dto)
        );
        assertEquals("Minimum amount cannot be greater than maximum amount", exception.getMessage());
    }

    @Test
    void testValidate_ZeroInterestRate() {
        LoanProductRequestDTO dto = LoanProductRequestDTO.builder()
                .name("Personal Loan")
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .interestRate(BigDecimal.ZERO)
                .tenureMonths(12)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(dto)
        );
        assertEquals("Interest rate must be positive", exception.getMessage());
    }

    @Test
    void testValidate_NegativeInterestRate() {
        LoanProductRequestDTO dto = LoanProductRequestDTO.builder()
                .name("Personal Loan")
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("-5"))
                .tenureMonths(12)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(dto)
        );
        assertEquals("Interest rate must be positive", exception.getMessage());
    }

    @Test
    void testValidate_ZeroTenure() {
        LoanProductRequestDTO dto = LoanProductRequestDTO.builder()
                .name("Personal Loan")
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12.5"))
                .tenureMonths(0)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(dto)
        );
        assertEquals("Tenure must be positive", exception.getMessage());
    }

    @Test
    void testValidate_NegativeTenure() {
        LoanProductRequestDTO dto = LoanProductRequestDTO.builder()
                .name("Personal Loan")
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12.5"))
                .tenureMonths(-6)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validate(dto)
        );
        assertEquals("Tenure must be positive", exception.getMessage());
    }
}