package pfa.dev.authservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthException extends RuntimeException {
    private final HttpStatus status;

    public AuthException(String message) {
        this(HttpStatus.UNAUTHORIZED, message);
    }

    public AuthException(String message, Throwable cause) {
        this(HttpStatus.UNAUTHORIZED, message, cause);
    }

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public AuthException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
