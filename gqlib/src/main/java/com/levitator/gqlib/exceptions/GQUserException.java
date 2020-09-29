package com.levitator.gqlib.exceptions;

/**
 * Exceptions caused by user input
 */
public class GQUserException extends GQRecoverableException {
    public GQUserException(String msg){
        super(msg);
    }
    
    public GQUserException(String msg, Throwable cause){
        super(msg, cause);
    }
}
