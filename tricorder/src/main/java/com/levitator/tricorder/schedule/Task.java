package com.levitator.tricorder.schedule;

import com.levitator.gqlib.exceptions.GQException;
import com.levitator.tricorder.Tricorder;
import com.levitator.tricorder.exceptions.TricorderException;
import java.io.IOException;
import java.time.LocalDateTime;
import static java.time.temporal.ChronoUnit.SECONDS;

public abstract class Task implements Comparable<Task>{
    public final Tricorder app;
    public final LocalDateTime time;
    
    public Task(Tricorder program, LocalDateTime t){
        app = program;
        time = t; 
    }
    
    //Due time in 0 or positive seconds
    public long due(){
        return due(LocalDateTime.now());
    }
    
    public long due(LocalDateTime now){
        return Math.max(0, now.until(time, SECONDS));
    }
    
    public boolean expired(){
        return due() <= 0;
    }
    
    public abstract void run() throws GQException, IOException, TricorderException;
    
    @Override
    public int compareTo(Task rhs){ return time.compareTo(rhs.time); } 
}
