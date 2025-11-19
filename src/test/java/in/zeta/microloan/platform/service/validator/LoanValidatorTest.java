package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.LoanIssuanceRequestDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.LoanApplication;
import in.zeta.microloan.platform.model.LoanProduct;
import in.zeta.microloan.platform.model.enums.LoanApplicationStatus;
import in.zeta.microloan.platform.model.enums.LoanProductStatus;
import in.zeta.microloan.platform.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoanValidatorTest {

    private LoanValidator validator;
    private LoanIssuanceRequestDTO requestDTO;
    private LoanApplication application;
    private LoanProduct product;
    private Borrower borrower;

    @BeforeEach
    void setUp() {
        validator = new LoanValidator();

        UUID borrowerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        requestDTO = LoanIssuanceRequestDTO.builder()
                .borrowerId(borrowerId)
                .productId(productId)
                .principalAmount(new BigDecimal("50000"))
                .interestRate(new BigDecimal("12"))
                .tenureMonths(12)
                .repaymentFrequency("MONTHLY")
                .disbursementMethod("BANK_TRANSFER")
                .disbursementDate(LocalDate.now())
                .firstDueDate(LocalDate.now().plusMonths(1))
                .build();

        application = LoanApplication.builder()
                .id(UUID.randomUUID())
                .borrowerId(borrowerId)
                .productId(productId)
                .status(LoanApplicationStatus.APPROVED)
                .approvedAmount(new BigDecimal("50000"))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        product = LoanProduct.builder()
                .id(productId)
                .status(LoanProductStatus.ACTIVE)
                .minAmount(new BigDecimal("10000"))
                .maxAmount(new BigDecimal("100000"))
                .tenureMonths(24)
                .build();

        borrower = Borrower.builder()
                .id(borrowerId)
                .isVerified(true)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void validateCreate_WithValidData_ShouldPass() {
        assertDoesNotThrow(() ->
                validator.validateCreate(requestDTO, application, product, borrower)
        );
    }

    @Test
    void validateCreate_WithInactiveProduct_ShouldThrowException() {
        product.setStatus(LoanProductStatus.DELETED);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, application, product, borrower)
        );

        assertTrue(exception.getMessage().contains("inactive product"));
    }

    @Test
    void validateCreate_WithUnverifiedBorrower_ShouldThrowException() {
        borrower.setIsVerified(false);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, application, product, borrower)
        );

        assertTrue(exception.getMessage().contains("verified"));
    }

    @Test
    void validateCreate_WithInactiveBorrower_ShouldThrowException() {
        borrower.setStatus(UserStatus.SUSPENDED);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, application, product, borrower)
        );

        assertTrue(exception.getMessage().contains("ACTIVE"));
    }

    @Test
    void validateCreate_WithAmountBelowMin_ShouldThrowException() {
        requestDTO.setPrincipalAmount(new BigDecimal("5000"));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, application, product, borrower)
        );

        assertTrue(exception.getMessage().contains("between"));
    }

    @Test
    void validateCreate_WithTenureExceedingMax_ShouldThrowException() {
        requestDTO.setTenureMonths(36);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, application, product, borrower)
        );

        assertTrue(exception.getMessage().contains("cannot exceed"));
    }

    @Test
    void validateCreate_WithNonApprovedApplication_ShouldThrowException() {
        application.setStatus(LoanApplicationStatus.PENDING_REVIEW);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, application, product, borrower)
        );

        assertTrue(exception.getMessage().contains("APPROVED"));
    }

    @Test
    void validateCreate_WithExpiredApplication_ShouldThrowException() {
        application.setExpiresAt(LocalDateTime.now().minusDays(1));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, application, product, borrower)
        );

        assertTrue(exception.getMessage().contains("expired"));
    }

    @Test
    void validateCreate_WithMismatchedAmount_ShouldThrowException() {
        application.setApprovedAmount(new BigDecimal("60000"));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, application, product, borrower)
        );

        assertTrue(exception.getMessage().contains("does not match"));
    }

    @Test
    void validateCreate_WithMismatchedBorrowerId_ShouldThrowException() {
        application.setBorrowerId(UUID.randomUUID());

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, application, product, borrower)
        );

        assertTrue(exception.getMessage().contains("Borrower ID"));
    }

    @Test
    void validateCreate_WithMismatchedProductId_ShouldThrowException() {
        application.setProductId(UUID.randomUUID());

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                validator.validateCreate(requestDTO, application, product, borrower)
        );

        assertTrue(exception.getMessage().contains("Product ID"));
    }

    @Test
    void validateCreate_WithoutApplication_ShouldPass() {
        assertDoesNotThrow(() ->
                validator.validateCreate(requestDTO, null, product, borrower)
        );
    }
}