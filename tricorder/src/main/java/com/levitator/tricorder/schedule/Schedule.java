package com.levitator.tricorder.schedule;
import com.levitator.tricorder.Tricorder;
import com.levitator.tricorder.exceptions.ScheduleInterrupted;
import com.levitator.util.Util;
import java.time.LocalDateTime;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import static java.time.temporal.ChronoUnit.DAYS;

/**
 *
 * Schedule of things to do
 * 
 * Currently, we schedule two kinds of events. NVM dumps. And then we have to
 * remember to recalculate the schedule at midnight.
 *
 */
public class Schedule {
    public final SortedSet<Task> tasks = new TreeSet<>();
    private final Tricorder app;
    private boolean m_active = false;
    
    //So that if we have more than one expired dump task, we just do the last one
    //DumpTask collapse_dumps = null;
    
    //So that when we start the new day, our periodic task times are scheduled relative
    //to the previous one from yesterday
    DumpTask last_dump = null;
    
    public Task find_first(Class<? extends Task> cls){
        return Util.find_first( tasks, (t) -> cls.isAssignableFrom(t.getClass()) );
    }
    
    public void queue_dump(DumpTask t){
        //Collapse expired tasks
        //queue at most one expired dump task. It will get done immediately,
        //but the others are discarded
        if(t.expired()){
            var dup = find_first(DumpTask.class);
            if(dup == null || !dup.expired())
                tasks.add(t);
        }
        else
            tasks.add(t);
    }
    
    private Task wait_for_task(Task task) throws ScheduleInterrupted{
        
        try{
            
            //Poll a bit, in case the system clock is altered
            long due;
            do{
                due = Math.min(2, task.due());
                TimeUnit.SECONDS.sleep(due);
            }
            while(due > 0);
        }
        catch(InterruptedException ex){
            throw new ScheduleInterrupted();
        }
        return task;
    }
    
    public Task next(){
            return tasks.first();
    }
    
    public Task pop_task() throws ScheduleInterrupted{
        Task result;
        
        result = tasks.first();
        wait_for_task(result);
        tasks.remove(result);
        
        if(result instanceof DumpTask)
            last_dump = (DumpTask)result;
        
        return result;
    }
    
    //Empty means empty with the exception of the new-day task, which is always present
    //to rebuild the schedule at the end of the day
    public boolean empty(){
        return tasks.first().getClass().equals(NewDayTask.class);
    }
    
    public void reset(){
        var now = LocalDateTime.now();
        var today = now.truncatedTo(DAYS);
        tasks.clear();
        m_active = false;
        
        //End-of day
        tasks.add(new NewDayTask(app, now));
        
        //Periodic dumps
        LocalDateTime t;
        if(last_dump != null)
            t = last_dump.time; //Resume generating periodic dumps relative to the time of the previous one
        else
            t = now;            //There is no previous dump event, so start at present time
        
        var period = app.get_config().get_dump_period();
        
        if(period != null){
            m_active = true;
            while(true){
                t = t.plus(period);
                if(t.getDayOfMonth() != now.getDayOfMonth()) //One day at a time
                    break;
                queue_dump(new DumpTask(app, t));
            }
        }
        
        //Specific-time dumps
        var times = app.get_config().get_dump_times();
        if(times.length > 0)
            m_active = true;
        
        for( var ts : app.get_config().get_dump_times() ){
            queue_dump(new DumpTask(app, today.with(ts)));
        }
    }
    
    public Schedule(Tricorder tri){
        app=tri;
        reset();
    }
    
    //While evaluating Config, we decide whether to consider scheduling as active.
    //It's active if any periodic or specific dump-times have beenn specified.
    public boolean is_active(){
        return m_active;
    }
    
}
