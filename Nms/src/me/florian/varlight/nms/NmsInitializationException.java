package me.florian.varlight.nms;

public class NmsInitializationException extends RuntimeException {

    public NmsInitializationException() {
        super("Failed to initialize NmsHandler for");
    }

    public NmsInitializationException(String message) {
        super(message);
    }

    public NmsInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NmsInitializationException(Throwable cause) {
        super("Failed to initialize NmsHandler for", cause);
    }

    public NmsInitializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
