package in.zeta.microloan.platform.exception;

import lombok.Getter;

@Getter
public enum Error {
    DATABASE_ERROR(1001, "Database error"),
    BORROWER_NOT_FOUND(1002, "Borrower not found"),
    INVALID_STATUS(1003, "Invalid status for the operation"),
    HOUSEHOLD_NOT_FOUND(1004, "Household not found"),
    BORROWER_NOT_FOUND_WITH_PHONE(1005, "Borrower not found with phone"),
    BORROWER_NOT_FOUND_WITH_ID(1006, "Borrower not found with id"),
    HOUSEHOLD_NOT_VERIFIED(1007, "Household not verified"),
    LOAN_NOT_FOUND(1008, "Loan not found"),
    LOAN_PRODUCT_NOT_FOUND(1009, "Loan product not found"),
    LOAN_APPLICATION_NOT_FOUND(1010, "Loan application not found"),
    LOAN_EXISTS(1011, "Loan already exists for borrower");

    private final int errorCode;
    private final String description;

    Error(int errorCode, String description) {
        this.errorCode = errorCode;
        this.description = description;
    }
}
