package tfc.jlluavm.exec.err;

public class LUAException extends RuntimeException {
    public LUAException() {
    }

    public LUAException(String message) {
        super(message);
    }

    public LUAException(String message, Throwable cause) {
        super(message, cause);
    }

    public LUAException(Throwable cause) {
        super(cause);
    }

    public LUAException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
