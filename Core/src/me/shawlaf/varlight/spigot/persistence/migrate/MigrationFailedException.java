package me.shawlaf.varlight.spigot.persistence.migrate;

public class MigrationFailedException extends RuntimeException {

    public MigrationFailedException() {
    }

    public MigrationFailedException(String message) {
        super(message);
    }

    public MigrationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MigrationFailedException(Throwable cause) {
        super(cause);
    }

    public MigrationFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
