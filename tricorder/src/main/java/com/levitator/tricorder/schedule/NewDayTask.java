package com.levitator.tricorder.schedule;

import com.levitator.gqlib.exceptions.GQException;
import com.levitator.gqlib.exceptions.GQInterruptedException;
import com.levitator.tricorder.config.Config;
import com.levitator.tricorder.Tricorder;
import com.levitator.util.Util;
import java.io.IOException;
import java.time.LocalDateTime;
import static java.time.temporal.ChronoUnit.DAYS;
import java.util.concurrent.TimeUnit;

/**
 *
 * Stuff to do first thing in the morning
 *
 */
public class NewDayTask extends Task{

    //We take a time argument to ensure a consistent time is used
    //throughout the Schedule generation process
    public NewDayTask(Tricorder tri, LocalDateTime now){
        //Worst case, if we're minutely early, we just end up running two or more NewDayTasks until it finally works
        super(tri, now.truncatedTo(DAYS).plusDays(1));
    }

    @Override
    public void run() throws GQException, IOException {
        
        //This does indeed get called early so, make sure it's really midnight or later before proceeding
        try{
            while(true){
                var now=LocalDateTime.now();
                var today=Util.round_to_day(now);
                if(today.getDayOfYear() != now.getDayOfYear())
                    TimeUnit.SECONDS.sleep(1);
                else
                    break;
            }
        }
        catch(InterruptedException ex){
            throw new GQInterruptedException(true, "Interrupted waiting on schedule");
        }
        
        //Just make up a fresh schedule
        app.reset_schedule();
    }
    
        @Override
    public String toString() {
        return "Today's schedule is complete. Waiting for tomorrow...";
    }
    
}
