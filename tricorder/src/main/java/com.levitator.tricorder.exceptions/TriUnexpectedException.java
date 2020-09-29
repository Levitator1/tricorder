package com.levitator.tricorder;


public class TriUnexpectedException extends RuntimeException {
    public TriUnexpectedException(String msg){
        super(msg);
    }
    
    public TriUnexpectedException(String msg, Throwable cause){
        super(msg, cause);
    }
}
