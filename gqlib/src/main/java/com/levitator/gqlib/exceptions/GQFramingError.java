package com.levitator.gqlib.exceptions;

//This means marker bytes were not as defined in the protcol spec
//This is with the exception of 0xFFFF which we find, experimentally, to be blanked
//or unpopulated memory. That throws GQNvmEOFException instead.
public class GQFramingError extends GQProtocolException{
    
    public GQFramingError(String msg){
        super(msg);
    }
            
}

