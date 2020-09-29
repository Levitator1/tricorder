package com.levitator.gqlib.exceptions;

/**
 *
 * Base class for library exceptions
 *
 */
public abstract class GQException extends Exception implements IGQException{
    
    public GQException(String msg){
        super(msg);
    }
    
    public GQException(String msg, Throwable cause){
        super(msg, cause);
    }
}
