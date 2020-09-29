package com.levitator.tricorder;
import com.levitator.tricorder.config.Config;
import com.levitator.gqlib.*;
import com.levitator.gqlib.exceptions.GQException;
import com.levitator.gqlib.exceptions.GQIOException;
import com.levitator.gqlib.exceptions.GQInterruptedException;
import com.levitator.gqlib.exceptions.GQProtocolException;
import com.levitator.gqlib.exceptions.GQUnexpectedException;
import com.levitator.tricorder.exceptions.ScheduleInterrupted;
import com.levitator.tricorder.features.LoggingMode;
import com.levitator.util.Timer;
import com.levitator.util.FunctionalThread;
import com.levitator.util.Util;
import java.io.*;

public class Main {
    
    //dirty shutdown
    static private void restore_save_mode(Config conf, boolean original_log_mode){
       
        var out = System.out;
        try{
            //In case our IO state was bad, we reopen a temporary device instance to
            //ensure that the log-enable status is restored before we exit
            out.print("Reopening device...");

            GQDevice dev;
            try(var okf = new OkFailGuard()){
                
                //Don't know what to do about the interrupted() flag getting stuck on sometimes
                //Give the IO process time to die off
                Thread.interrupted();
                Thread.sleep(2000);

                dev = new GQDevice(conf.get_device_path());
                okf.status = true;
            }

            var msg = Tricorder.make_save_mode_restore_message(conf, original_log_mode);
            LoggingMode.logging_mode_feature(dev, msg, original_log_mode, null);
        }
        catch(Exception ex){
            //Can't do anything
            out.println("Failed last attempt at cleaning up: " + ex.getMessage());
        }
    }
    
    static private void shutdown_handler(Thread t){
        t.interrupt();
        
        while(true){
            try{
                t.join();
                break;
            }
            catch(InterruptedException ex){}
        }
    }
    
    /*
    private static void restore_log_mode_for_exit(Tricorder app, boolean mode)
            throws GQProtocolException, GQIOException, GQInterruptedException{
        
        var conf_mode = app.get_config().get_set_log_mode(); 
        if(conf_mode != null){
            LoggingMode.logging_mode_feature(app.m_device, "Restoring device logging mode to requested value: " + conf_mode,
                    conf_mode, null);
        }
        else{
            LoggingMode.logging_mode_feature(app.m_device, "Leaving device logging mode as we found it on startup: " + mode,
                    mode, null);
        }
        
    }
    */
    
    private static void run(Config conf) throws IOException, InterruptedException, Exception{
        
        
        //This traps JVM shutdown, which happens on interrupt, SIGINT, ctrl-C, etc
        //It launches a thread which stalls shutdown until the main thread exits, which it should
        //because it will throw an Interrupted exception
        {
            var this_thread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook( 
                new FunctionalThread( ()->shutdown_handler(this_thread) ) );
        }
        
        var out = System.out;
        boolean recoverable = true;
        var app = new Tricorder(conf);
        
        //This is what was found on the device on startup
        //Or what was immediately set on the device per user request in new Tricorder()
        var original_log_mode = app.original_log_mode;
        
        try{
            app.initial_actions();
            app.run_schedule();
        }
        catch(ScheduleInterrupted ex){
            //This is where, statistically, most of the program's idle time will be
            return; //Clean exit
        }
        catch(InterruptedException ex){
            //This should mean we got interrupted outside of GQDevice code,
            //so our device state should remain well-defined
            recoverable = true;
            out.println();
            out.println("Caught an interrupt outside device code. Shutdown on existing connection...");
            if(conf.stack_trace_on_interrupt)
                throw ex;
            //Clean exit
        }
        catch(GQException | GQUnexpectedException ex){
            boolean is_gqinterrupt = GQInterruptedException.class.isAssignableFrom(ex.getClass());
            recoverable = ex.recoverable();
            String msg;
            
            if(!is_gqinterrupt)
                msg = "Caught a GQlib exception. Shutting down... ";
            else
                msg = "Caught interrupt. Shutting down... ";
            
            if(recoverable)
                msg = msg + "Connection should still be live...";
            else
                msg = msg + "Discarding bad connection....";
            
            out.println(msg);
            out.println();
            if(!is_gqinterrupt || Config.stack_trace_on_interrupt)
                throw ex; //Stack trace
        }
        catch(Exception ex){
            out.println();
            out.println("Caught an unexpected exception outside device code. Shutting down using existing connection...");
            recoverable = true;
            throw ex; //For a stack trace
        }
        finally{
            
            //This block should perform app.close() in all branches
            try{
                if(recoverable){
                    app.restore_original_log_mode(); //can throw for failsafe exit
                    app.close(); //close here
                }
                else
                    throw new Exception("Need failsafe exit"); //causes close()
            }
            catch(Exception ex){
                //Failsafe exit
                app.close(); //close() here
                restore_save_mode( conf, original_log_mode );
            }
        }
        
    }

    public static void main(String[] args){
        Timer launch_time = new Timer();
        var out = System.out;
        out.println();
        out.println("tricorder " + Config.version_string);
        out.println("Portable GQ EMF-390 tool");
        out.println();
        
        try{
            var conf = new Config(args, launch_time);
            if(conf.is_show_help())
                Config.usage();
            else
                run(conf);
        }
        catch(ConfigException ex){
            Config.usage();
            System.out.println("Invalid argument(s): " + ex.getMessage());
        }
        catch( Exception ex ){
            System.out.println();
            System.out.println( "Error: " + ex.toString() );
            Util.stack_traces(ex, System.out);
        }
        
        System.out.println("Exit");
        System.out.println();
    }
}
