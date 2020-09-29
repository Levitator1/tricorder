package com.levitator.gqlib.exceptions;

/**
 *
 * We got back corrupt or incomprehensible data from the device, and now we're
 * not sure whether the stream is usable
 *
 */
public class GQProtocolException extends GQIrrecoverableException {
    public GQProtocolException(String msg){
        super(msg);
    }
}
