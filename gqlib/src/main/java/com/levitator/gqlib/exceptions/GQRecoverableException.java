package com.levitator.gqlib.exceptions;

/**
 *
 * Something went wrong but the protocol stream is still intact
 * 
 */
public class GQRecoverableException extends GQException{

    public GQRecoverableException(String msg){
        super(msg);
    }
    
    public GQRecoverableException(String msg, Throwable cause){
        super(msg, cause);
    }
    
    @Override
    public boolean recoverable() {
        return true;
    }
    
}
