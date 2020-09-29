package com.levitator.gqlib.exceptions;

/**
 *
 *
 */
public class GQIrrecoverableException extends GQException{

    public GQIrrecoverableException(String msg){
        super(msg);
    }
    
    public GQIrrecoverableException(String msg, Throwable cause){
        super(msg, cause);
    }
    
    @Override
    public boolean recoverable() {
        return false;
    }
    
}
