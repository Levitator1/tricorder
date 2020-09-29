package com.levitator.gqlib;

import com.levitator.gqlib.exceptions.GQTimeFormatException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;

public class TimeFormat {
    static public String format(LocalDateTime t, DateTimeFormatter fmt){
        return fmt.format(t);
    }
    
    static public String format(LocalTime t, DateTimeFormatter fmt){
        return fmt.format(t);
    }
    
    //Uses the functor mkfmt to attempt to parse str with each formatter in fmts until one is found that works.
    //Otherwise, an exception is thrown
    static private <T> T parse_something(String str, DateTimeFormatter[] fmts, Function<TemporalAccessor, T> mkfmt) throws GQTimeFormatException{
        
        T result = null;
        for(var f : fmts){
            try{
                result = mkfmt.apply(f.parse(str));
                return result;
            }catch(Exception ex){}
        }
        throw new GQTimeFormatException("Failed to parse time string");
    }
    
    static public LocalDateTime parseDateTime(String str, DateTimeFormatter[] fmts) throws GQTimeFormatException{
        try{
            return parse_something(str, fmts, (t) -> LocalDateTime.from(t) );
        }
        catch(Exception ex){
            throw new GQTimeFormatException("Could not parse date/time. " +
                    "Valid format is \"2000/12/29 1:30:00.0000AM\", in 12 or 24-hour fromat, with optional fractions of a second");
        }
    }
    
    static public LocalTime parseTime(String str, DateTimeFormatter[] fmts) throws GQTimeFormatException{
        try{
            return parse_something(str, fmts, (t) -> LocalTime.from(t) );
        }
        catch(Exception ex){
            throw new GQTimeFormatException("Could not parse time of day. Valid format is \"1:30:00AM\", in 12 or 24-hour format");
        }
    }
    
}
