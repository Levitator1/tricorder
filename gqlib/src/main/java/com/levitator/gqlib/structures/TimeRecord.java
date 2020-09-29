package com.levitator.gqlib.structures;

import com.levitator.gqlib.exceptions.GQFramingError;
import com.levitator.gqlib.exceptions.internal.RecordTruncatedException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;

/*
*
* A record from the measurment log on a GQ EMF-390 (or similar?)
* 
*/
public class TimeRecord extends NvmRecordBase{
    
    static public boolean check_framing(int value){
        return value == 0x55aa;
    }
    
    @Override
    protected void check_framing() throws GQFramingError{
        if(!check_framing(framing_bytes))
            throw new GQFramingError("Framing bytes were wrong for expected TimeRecord");
    }
    
    static public int sizeof(){
        return 8;
    }
    
    public TimeRecord(ByteBuffer buf) throws GQFramingError, RecordTruncatedException{
        
        super(buf);
        try{
            time = LocalDateTime.of(2000 + buf.get(), buf.get(), buf.get(), buf.get(), buf.get(), buf.get());
        }
        catch(BufferUnderflowException ex){
            throw new RecordTruncatedException(sizeof());
        }
    }
}
