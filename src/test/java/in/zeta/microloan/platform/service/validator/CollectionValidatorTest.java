package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.CollectionActivityRequestDTO;
import in.zeta.microloan.platform.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CollectionValidatorTest {

    private CollectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CollectionValidator();
    }

    @Test
    void testValidateActivity_Success() {
        CollectionActivityRequestDTO dto = CollectionActivityRequestDTO.builder()
                .loanId(UUID.randomUUID())
                .activityType("REMINDER")
                .contactMethod("PHONE")
                .borrowerResponse("Will pay soon")
                .promiseToPayDate(LocalDate.now().plusDays(5))
                .notes("Borrower agreed")
                .build();

        assertDoesNotThrow(() -> validator.validateActivity(dto));
    }

    @Test
    void testValidateActivity_NullLoanId() {
        CollectionActivityRequestDTO dto = CollectionActivityRequestDTO.builder()
                .loanId(null)
                .activityType("REMINDER")
                .contactMethod("PHONE")
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validateActivity(dto)
        );
        assertEquals("Loan ID is required", exception.getMessage());
    }

    @Test
    void testValidateActivity_NullActivityType() {
        CollectionActivityRequestDTO dto = CollectionActivityRequestDTO.builder()
                .loanId(UUID.randomUUID())
                .activityType(null)
                .contactMethod("PHONE")
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validateActivity(dto)
        );
        assertEquals("Activity type is required", exception.getMessage());
    }

    @Test
    void testValidateActivity_EmptyActivityType() {
        CollectionActivityRequestDTO dto = CollectionActivityRequestDTO.builder()
                .loanId(UUID.randomUUID())
                .activityType("   ")
                .contactMethod("PHONE")
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validateActivity(dto)
        );
        assertEquals("Activity type is required", exception.getMessage());
    }

    @Test
    void testValidateActivity_NullContactMethod() {
        CollectionActivityRequestDTO dto = CollectionActivityRequestDTO.builder()
                .loanId(UUID.randomUUID())
                .activityType("REMINDER")
                .contactMethod(null)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validateActivity(dto)
        );
        assertEquals("Contact method is required", exception.getMessage());
    }

    @Test
    void testValidateActivity_EmptyContactMethod() {
        CollectionActivityRequestDTO dto = CollectionActivityRequestDTO.builder()
                .loanId(UUID.randomUUID())
                .activityType("REMINDER")
                .contactMethod("  ")
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> validator.validateActivity(dto)
        );
        assertEquals("Contact method is required", exception.getMessage());
    }
}
