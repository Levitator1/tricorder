package com.levitator.gqlib.exceptions;

/**
 *
 * Data provided by the user was unparsable
 * 
 */
public class GQUserDataFormatException extends GQUserException {
    public GQUserDataFormatException(String msg){
        super(msg);
    }
    
    public GQUserDataFormatException(String msg, Throwable cause){
        super(msg, cause);
    }
}
