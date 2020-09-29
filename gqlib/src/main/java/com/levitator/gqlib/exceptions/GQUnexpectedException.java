package com.levitator.gqlib.exceptions;

/*
*
* We don't know how this exception snuck in and we don't know what to do with it
*
*/

public class GQUnexpectedException extends RuntimeException implements IGQException{
    
    public GQUnexpectedException(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public boolean recoverable() {
        return false;
    }
    
}
