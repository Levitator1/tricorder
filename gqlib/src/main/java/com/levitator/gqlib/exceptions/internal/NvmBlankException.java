package com.levitator.gqlib.exceptions.internal;

import com.levitator.gqlib.exceptions.GQRecoverableException;

/**
 *
 * Experimentally, we find that NVM gets initialized to 0xFF, so if we get
 * 0xFFFF for framing bytes, then we decide that we've reached EOF
 * 
 */
public class NvmBlankException extends GQRecoverableException{
    
    public NvmBlankException() {
        super("Reached the end of populated device memory");
    }
}
