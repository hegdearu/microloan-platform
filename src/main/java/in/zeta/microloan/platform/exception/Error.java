package in.zeta.microloan.platform.exception;

public enum Error {
    DATABASE_ERROR(1001, "Database error");

    private final int errorCode;
    private final String description;

    Error(int errorCode, String description) {
        this.errorCode = errorCode;
        this.description = description;
    }
}
