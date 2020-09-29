package com.levitator.gqlib.exceptions;

/**
 *
 * There was an IO error accessing the device streams
 * 
 */
public class GQIOException extends GQIrrecoverableException{
    
    public GQIOException(String msg) {
        super(msg);
    }
    
    public GQIOException(String msg, Throwable cause){
        super(msg, cause);
    }
    
}
