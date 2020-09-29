package com.levitator.tricorder.features;

import com.levitator.gqlib.GQDevice;
import com.levitator.gqlib.OkFailGuard;
import com.levitator.gqlib.exceptions.GQIOException;
import com.levitator.gqlib.exceptions.GQInterruptedException;
import com.levitator.gqlib.exceptions.GQProtocolException;
import com.levitator.util.Timer;
import com.levitator.tricorder.config.Config;
import java.time.LocalDateTime;

/*
*
* TimeFormat-related features
*
*/
public class Time {
    
    static public void do_time_features(Config conf, GQDevice dev) throws 
            GQProtocolException, GQIOException, GQInterruptedException{
        
        if(!conf.is_set_time()) return;
        var out = System.out;
        LocalDateTime time;
        
        if(conf.get_time_to_set() != null){
            time = dev.adjusted_out_time(conf.get_time_to_set(), conf.get_start_time());
            out.print("Setting device time to: " + Config.format(time) + "... ");
        }
        else{
            time = dev.adjusted_out_time( LocalDateTime.now(), new Timer());
            out.print("Setting device time to system time: " + Config.format(time) + "... ");
        }
        
        try( var guard = new OkFailGuard() ){
            dev.set_time(time);
            guard.status = true;
        }
    }
}
