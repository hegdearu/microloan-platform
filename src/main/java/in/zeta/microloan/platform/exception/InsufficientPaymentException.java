package in.zeta.microloan.platform.exception;

public class InsufficientPaymentException extends RuntimeException {
    public InsufficientPaymentException(String message) {
        super(message);
    }
}
