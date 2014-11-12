package com.tapstream.rollbar;

public class RollbarException extends Exception{
    private static final long serialVersionUID = 2215482856278191394L;
    public RollbarException() {
    }

    public RollbarException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RollbarException(String message, Throwable cause) {
        super(message, cause);
    }

    public RollbarException(String message) {
        super(message);
    }

    public RollbarException(Throwable cause) {
        super(cause);
    }
}
