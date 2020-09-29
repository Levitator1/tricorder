package com.levitator.gqlib.exceptions;

/**
 *
 * Received an interrupt/signal
 *
 */
public class GQInterruptedException extends GQSometimesRecoverable{

    public GQInterruptedException(boolean recoverable, String msg) {
        super(recoverable, msg);
    }
    
    public GQInterruptedException(boolean recoverable, String msg, Throwable cause) {
        super(recoverable, msg, cause);
    }
}
