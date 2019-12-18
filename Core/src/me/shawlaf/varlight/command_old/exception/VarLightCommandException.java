package me.shawlaf.varlight.command_old.exception;

@Deprecated
public class VarLightCommandException extends RuntimeException {

    public VarLightCommandException() {
    }

    public VarLightCommandException(String message) {
        super(message);
    }

    public VarLightCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public VarLightCommandException(Throwable cause) {
        super(cause);
    }
}
