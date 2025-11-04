package in.zeta.microloan.platform.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(Error message) {
        super(String.valueOf(message));
    }
}
