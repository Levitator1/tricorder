package com.levitator.tricorder.features;

import com.levitator.gqlib.GQDevice;
import com.levitator.gqlib.OkFailGuard;
import com.levitator.gqlib.exceptions.GQException;
import com.levitator.gqlib.exceptions.GQIOException;
import com.levitator.gqlib.exceptions.GQInterruptedException;
import com.levitator.gqlib.exceptions.GQProtocolException;
import com.levitator.util.Action;
import com.levitator.util.Ref;
import java.util.function.Consumer;
import java.util.function.Function;
import util.guards.Guard;

public class LoggingMode {
    
    //Accompanies a mode-setting operation with an OK/FAIL blurb
    //and returns a guard which can be either close()ed to undo the operation or discarded.
    //restore_msg_f() accepts the boolean value being restored and returns an OK/FAIL header
    //string to display for the restore operation
    static public Guard<GQException> logging_mode_feature(GQDevice device, String msg, boolean mode, Function<Boolean, String> restore_msg_f) 
            throws GQProtocolException, GQIOException, GQInterruptedException{
        
        final boolean old_value;
        var out = System.out;
        out.print(msg);
        try(var guard = new OkFailGuard()){
            old_value = device.set_save_data_onoff(mode);
            guard.status = true;
        }
        
        if(restore_msg_f != null)
            return ()->restore_logging_mode(device, old_value, restore_msg_f);
        else
            return null;
    }
    
    static private void restore_logging_mode(GQDevice device, boolean mode, Function<Boolean, String> restore_msg_f) 
            throws GQProtocolException, GQIOException, GQInterruptedException{
        
        if(restore_msg_f != null){
            System.out.print( restore_msg_f.apply(mode) );
            try(var guard = new OkFailGuard()){
                device.set_save_data_onoff(mode);
                guard.status = true;
            }
        }
        else{
            device.set_save_data_onoff(mode);
        }
    }

}
