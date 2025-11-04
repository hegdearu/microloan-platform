package in.zeta.microloan.platform.exception;

public class EventPublishingException extends RuntimeException {
    public EventPublishingException(String message, Throwable cause) {
        super(message);
    }

    public EventPublishingException(String message) {
        super(message);
    }
}
