package com.levitator.tricorder.exceptions;

public class TricorderException extends Exception {

    public TricorderException() {
    }

    public TricorderException(String message) {
        super(message);
    }

    public TricorderException(String message, Throwable cause) {
        super(message, cause);
    }

    public TricorderException(Throwable cause) {
        super(cause);
    }
}
