package com.levitator.gqlib.structures;

import com.levitator.gqlib.exceptions.GQException;
import com.levitator.gqlib.exceptions.GQUnexpectedException;
import com.levitator.gqlib.exceptions.internal.NvmBlankException;
import com.levitator.gqlib.exceptions.GQProtocolException;
import com.levitator.gqlib.exceptions.GQFramingError;
import com.levitator.gqlib.exceptions.internal.RecordTruncatedException;
import com.levitator.util.Util;
import java.nio.ByteBuffer;

public class RecordFactory {

    //Private to keep people from calling it and forgetting to restore the buffer position on exception
    static private Class<? extends NvmRecordBase> identify_record(ByteBuffer buf) 
            throws GQFramingError, RecordTruncatedException, NvmBlankException{
        
        var id = NvmRecordBase.peek_record_id(buf);
        if( TimeRecord.check_framing(id) )
            return TimeRecord.class;
        else if( MeasurementRecord.check_framing(id) )
            return MeasurementRecord.class;
        else if(id == 0xffff) //Seems like blank memory isn't always 0xFFFF?
            throw new NvmBlankException(); //Framing bytes appear to be blank memory
        else
            throw new GQFramingError("Framing bytes did not match any known record type: " + id);

    }
    
    //We do this a stupid way so that the debugger will trace properly
    static public NvmRecordBase read(ByteBuffer buf) throws GQFramingError, RecordTruncatedException, GQProtocolException, NvmBlankException{
       
       try(var guard=Util.push_buffer_pos(buf)){ 
            var cls = identify_record(buf);
            NvmRecordBase result;

            if(cls.equals(TimeRecord.class))
                result = new TimeRecord(buf);
            else
                result = new MeasurementRecord(buf);
           
            guard.status = true;
            return result;
       }
    }
    
    static public int sizeof(ByteBuffer buf) throws GQFramingError, RecordTruncatedException, GQProtocolException, GQUnexpectedException, NvmBlankException{
        try {
            return (int)identify_record(buf).getMethod("sizeof").invoke(null);
        }
        catch (ReflectiveOperationException | SecurityException ex) {
            throw new GQUnexpectedException("Unexpected exception retrieving record size", ex); 
        }
    }
    
}
