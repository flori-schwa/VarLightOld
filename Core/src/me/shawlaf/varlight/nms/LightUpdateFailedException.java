package me.shawlaf.varlight.nms;

public class LightUpdateFailedException extends RuntimeException {

    public LightUpdateFailedException() {
        super("Light update failed!");
    }

    public LightUpdateFailedException(String message) {
        super(message);
    }

    public LightUpdateFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LightUpdateFailedException(Throwable cause) {
        super("Light update failed!", cause);
    }

    public LightUpdateFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
