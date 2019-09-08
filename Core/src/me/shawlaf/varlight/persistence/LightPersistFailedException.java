package me.shawlaf.varlight.persistence;

public class LightPersistFailedException extends RuntimeException {

    public LightPersistFailedException() {
        super("Failed to persist Custom Light sources");
    }

    public LightPersistFailedException(String message) {
        super(message);
    }

    public LightPersistFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LightPersistFailedException(Throwable cause) {
        super("Failed to persist Custom Light sources", cause);
    }

    public LightPersistFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
