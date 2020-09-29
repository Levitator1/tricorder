package com.levitator.tricorder;

import com.levitator.tricorder.config.Config;
import com.levitator.tricorder.schedule.Schedule;
import com.levitator.gqlib.GQDevice;
import com.levitator.gqlib.OkFailGuard;
import com.levitator.gqlib.exceptions.GQException;
import com.levitator.gqlib.exceptions.GQIOException;
import com.levitator.gqlib.exceptions.GQInterruptedException;
import com.levitator.gqlib.exceptions.GQProtocolException;
import com.levitator.tricorder.exceptions.ScheduleInterrupted;
import com.levitator.tricorder.exceptions.TricorderException;
import com.levitator.tricorder.features.DumpLog;
import static com.levitator.tricorder.features.DumpLog.do_clear_log;
import com.levitator.tricorder.features.LoggingMode;
import com.levitator.tricorder.schedule.DumpTask;
import com.levitator.tricorder.schedule.Task;
import java.io.IOException;

/*
*
* Program state
* provides callback targets for the schedule to perform actions
*
*/
public class Tricorder implements AutoCloseable{
    Config m_conf;
    GQDevice m_device;
    Schedule m_schedule;
    public final boolean original_log_mode; //The log mode to which to restore on exit
                                            //Tricorder does not do this on close() becuase 
                                            //there are external error-handling considerations
    
    Tricorder(Config conf) throws GQIOException, GQInterruptedException, GQProtocolException{
        m_conf = conf;
        var out = System.out;
        out.print("Connecting to device: " + m_conf.get_device_path() + "... ");
        
        try( var guard = new OkFailGuard() ){
            m_device = new GQDevice(m_conf.get_device_path());
            guard.status = true;
        }
        out.println("Device version: " + m_device.get_version());
        out.println("Device clock: " + Config.format(m_device.get_time()));
        out.println("Logging to onboard memory: " + m_device.get_save_data_onoff());
        
        var conf_logmode = m_conf.get_set_log_mode();
        if(conf_logmode != null){
            original_log_mode = conf_logmode;   //Save the requested mode as the restore target
            LoggingMode.logging_mode_feature(m_device, "Setting device log mode as requested: " + conf_logmode + " ... ",
                    conf_logmode, null);
        }
        else
            original_log_mode = m_device.get_save_data_onoff();

        m_schedule = new Schedule(this);
    }
    
    public static String make_save_mode_restore_message(Config conf, boolean v){
        return 
            (conf.get_set_log_mode() == null ? 
                "Leaving log-save status as we found it on startup: " :
                "Leaving log-status as requested: ") 
                    + v + " ... ";
    }
    
    //clean-exit way to do it
    public void restore_original_log_mode() throws GQProtocolException, GQIOException, GQInterruptedException{
        var out = System.out;
        out.print(make_save_mode_restore_message(m_conf, original_log_mode));
        try(var okf = new OkFailGuard()){
            m_device.set_save_data_onoff(original_log_mode);
            m_device.navigate_home();
            okf.status = true;
        }
    }
    
    //Stuff we do once on startup, before we begin waiting on the schedule
    public void initial_actions() throws Exception{
        var out = System.out;
  
        //m_device.navigate_home();
        com.levitator.tricorder.features.Time.do_time_features(m_conf, m_device);
        
        boolean no_clear = true;
        if(m_conf.is_dump_log_now()){
            //Dump the log right away to a specific output file
            DumpLog.feature_dump(m_device, m_conf);
            if(m_conf.is_clear_after_dump())
                no_clear = false;

            //clear now if initial clear was requested, but don't do it twice
            if(m_conf.is_clear_log_now() && no_clear){
                do_clear_log(m_device);
                no_clear = false;
            }
        }
        else{
            if(m_conf.is_clear_log_now()){
                do_clear_log(m_device);
                no_clear = false;
            }
        }
        
        if(!m_conf.is_dump_log_now())
            out.println("No initial dump action requested.");
        
        if(!(m_conf.is_dump_log_now()) && !m_schedule.is_active()){
            if(no_clear && m_conf.is_clear_after_dump()) out.println("-c ignored because no dumps were requested. Do you mean -C or --CLEAR?");
            return;
        }

    }
        
    public void dump_device_log() throws IOException, GQProtocolException, GQIOException, GQInterruptedException{ 
        //Also clears the log if set in config
        DumpLog.feature_dump(m_device, m_conf);
    }
    
    //Everything that isn't initial_actions() happens here
    public void run_schedule()
            throws ScheduleInterrupted, GQException, IOException, TricorderException, ScheduleInterrupted{
        
        var out = System.out;
        Task task;
        
        if(!m_schedule.is_active()){
            out.println("No log-dumps scheduled.");
            return;
        }
        
       //We assume logging should be on if we are going to be waiting around for dump-time
       //Log mode should get restored on app exit
       LoggingMode.logging_mode_feature(m_device, "Enabling device logging because dump schedule is active: ",
            true, null);
        
        //Leave the user viewing the main display on the device between dumps
        //Otherwise, they are probably sitting at the save-mode-setting screen
        //and that is not very convenient
        m_device.navigate_home();
        
        //infinite loop, with the exception of exceptions and interrupts
        while(true){
            task = m_schedule.next();
            out.println(task.toString());
            task = m_schedule.pop_task();
            task.run();
        }
    }
    
    public void reset_schedule(){
        
        m_schedule.reset();
        var dump =  m_schedule.find_first(DumpTask.class);
        System.out.println("Today's dump schedule generated.");
        
        if(dump == null){
            System.out.println("Somehow, we started the day without any dumps scheduled.");
            System.out.println("That begs the question of how we got into scheduled mode in the first place.");
            System.out.println("This is probably a bug.");
        }
    }
    
    public Config get_config(){
        return m_conf;
    }
    
    @Override
    public void close() {
        m_device.close();
    }
    
}
