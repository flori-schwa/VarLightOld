package me.florian.varlight.nms;

public class VarLightInitializationException extends RuntimeException {

    public VarLightInitializationException() {
        super("Failed to initialize NmsHandler for");
    }

    public VarLightInitializationException(String message) {
        super(message);
    }

    public VarLightInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public VarLightInitializationException(Throwable cause) {
        super("Failed to initialize NmsHandler for", cause);
    }

    public VarLightInitializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
