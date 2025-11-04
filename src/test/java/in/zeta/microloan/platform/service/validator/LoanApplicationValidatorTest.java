package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.LoanApplicationRequestDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.LoanProduct;
import in.zeta.microloan.platform.model.enums.LoanProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoanApplicationValidatorTest {

    private LoanApplicationValidator validator;
    private LoanApplicationRequestDTO requestDTO;
    private Borrower borrower;
    private LoanProduct product;

    @BeforeEach
    void setUp() {
        validator = new LoanApplicationValidator();

        requestDTO = LoanApplicationRequestDTO.builder()
                .borrowerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .requestedAmount(new BigDecimal("50000"))
                .purpose("Business")
                .preferredTenure(12)
                .build();

        borrower = Borrower.builder()
                .id(UUID.randomUUID())
                .isVerified(true)
                .build();

        product = LoanProduct.builder()
                .id(UUID.randomUUID())
                .status(LoanProductStatus.ACTIVE)
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .tenureMonths(24)
                .build();
    }

    @Test
    void validateCreate_WithValidData_ShouldPass() {
        assertDoesNotThrow(() ->
                validator.validateCreate(requestDTO, borrower, product, 0, 3, false)
        );
    }

    @Test
    void validateCreate_WithUnverifiedBorrower_ShouldThrowException() {
        borrower.setIsVerified(false);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, borrower, product, 0, 3, false)
        );

        assertTrue(exception.getMessage().contains("verified"));
    }

    @Test
    void validateCreate_WithInactiveProduct_ShouldThrowException() {
        product.setStatus(LoanProductStatus.DELETED);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, borrower, product, 0, 3, false)
        );

        assertTrue(exception.getMessage().contains("not active"));
    }

    @Test
    void validateCreate_WithMaxActiveLoansReached_ShouldThrowException() {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, borrower, product, 3, 3, false)
        );

        assertTrue(exception.getMessage().contains("Maximum"));
    }

    @Test
    void validateCreate_WithPendingApplication_ShouldThrowException() {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, borrower, product, 0, 3, true)
        );

        assertTrue(exception.getMessage().contains("pending"));
    }

    @Test
    void validateCreate_WithAmountBelowMin_ShouldThrowException() {
        requestDTO.setRequestedAmount(new BigDecimal("5000"));

        ValidationException exception = assertThrows(ValidationException.class, () ->
                validator.validateCreate(requestDTO, borrower, product, 0, 3, false)
        );

        assertTrue(exception.getMessage().contains("between"));
    }

    @Test
    void validateCreate_WithAmountAboveMax_ShouldThrowException() {
        requestDTO.setRequestedAmount(new BigDecimal("150000"));

        ValidationException exception = assertThrows(ValidationException.class, () ->
                validator.validateCreate(requestDTO, borrower, product, 0, 3, false)
        );

        assertTrue(exception.getMessage().contains("between"));
    }

    @Test
    void validateCreate_WithTenureExceedingMax_ShouldThrowException() {
        requestDTO.setPreferredTenure(36);

        ValidationException exception = assertThrows(ValidationException.class, () ->
                validator.validateCreate(requestDTO, borrower, product, 0, 3, false)
        );

        assertTrue(exception.getMessage().contains("Maximum tenure"));
    }

    @Test
    void validateApproveAmount_WithValidAmount_ShouldPass() {
        BigDecimal approvedAmount = new BigDecimal("50000");

        assertDoesNotThrow(() ->
                validator.validateApproveAmount(approvedAmount, product)
        );
    }

    @Test
    void validateApproveAmount_WithAmountOutOfRange_ShouldThrowException() {
        BigDecimal approvedAmount = new BigDecimal("150000");

        ValidationException exception = assertThrows(ValidationException.class, () ->
                validator.validateApproveAmount(approvedAmount, product)
        );

        assertTrue(exception.getMessage().contains("between"));
    }
}