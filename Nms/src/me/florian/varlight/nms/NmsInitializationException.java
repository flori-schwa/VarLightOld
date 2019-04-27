package me.florian.varlight.nms;

import me.florian.varlight.VarLightPlugin;

public class NmsInitializationException extends RuntimeException {

    public NmsInitializationException() {
        super("Failed to initialize NmsHandler for " + VarLightPlugin.getServerVersion());
    }

    public NmsInitializationException(String message) {
        super(message);
    }

    public NmsInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NmsInitializationException(Throwable cause) {
        super("Failed to initialize NmsHandler for " + VarLightPlugin.getServerVersion(), cause);
    }

    public NmsInitializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
