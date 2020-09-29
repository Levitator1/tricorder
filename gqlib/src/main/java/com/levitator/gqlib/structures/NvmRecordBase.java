package com.levitator.gqlib.structures;

import com.levitator.gqlib.exceptions.GQFramingError;
import com.levitator.util.Util;
import com.levitator.gqlib.exceptions.internal.RecordTruncatedException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;

// NVM timestamp/measurment records are called SPI, for some reason
// Here is a base class so that we can sequence both types in the same container
public abstract class NvmRecordBase {
    
    //Two leading bytes identify the type of record
    public static final int id_size = 2;
    public final int framing_bytes;
    public LocalDateTime time;
    
    //Get the framing bytes but leave the buffer position alone
    static public int peek_record_id(ByteBuffer data) throws RecordTruncatedException{
        int result, pos = data.position();
        try{
            result = get_record_id(data);
        }
        finally{
            data.position(pos);
        }
        return result;
    }
    
    static private int get_record_id(ByteBuffer data) throws RecordTruncatedException{
        try{
            return Util.bin_or(0, data.getShort());
        }
        catch(BufferUnderflowException ex){
            throw new RecordTruncatedException(id_size);
        }
    }
    
    public NvmRecordBase(ByteBuffer data) throws RecordTruncatedException{
        framing_bytes = get_record_id(data);
    }
    
    //Derived class checks that framing is correct or throws
    protected abstract void check_framing() throws GQFramingError;
    
}
