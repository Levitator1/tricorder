package com.levitator.gqlib;

import com.levitator.gqlib.config.Config;
import com.levitator.gqlib.structures.MeasurementRecord;
import com.levitator.util.Pair;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 *
 * So, these devices are memory-constrained at 1MB of storage, so time records
 * get written about once every three minutes. Here, we perform a lot of magic
 * to try to fill in precise times from the information provided, or if we don't have
 * enough data to do that, we fall back on defaults obtained by playing with an EMF-390
 *
 */
public class TimeInterpolate {
    
    //Starting at i+1, find a record whose time differs from the preceeding record
    static public  int seek_dt(ArrayList<MeasurementRecord> data, int i){
        var ct=data.size();
        LocalDateTime pt = data.get(i++).time;
        
        for(; i<ct; ++i){
            if( pt.compareTo(data.get(i).time) != 0 )
                break;
        }
        return i;
    }
    
    //For a complete (non-truncated, i.e. not beginng or end of dataset) series of same-timestamp records, calculate the sample rate and interpolate the times
    //Place the completed records in result.
    //Return dt between samples (1/(sample rate))
    static public Duration interpolate_session_times(ArrayList<MeasurementRecord> sess, LogDataSet result, int di0, int di1){
        Duration sd;
        var r0 = sess.get(di0);
        var r1 = sess.get(di1);
        var dt = Duration.between(r0.time, r1.time);
        sd = dt.dividedBy(di1 - di0);
        var t = sess.get(di0).time;
        MeasurementRecord r;
        for(var i=di0; i<di1;++i){
            r = sess.get(i);
            r.time = t;
            r.uncertain_time = false;
            result.add(r);
            t = t.plus(sd);
        }
        return sd;
    }
    
    //For the record count specified (presumably all having the same hardware timestamp), take a guess as to its dt and sample interval
    static public Pair<Integer, Duration> guess_dt(int ct){
        Duration sd;
        
        //dt for dataset assuming the default sample interval
        var dt = Config.dt_per_sample * (ct-1);

        //Default sample interval says dt is greater than the granularity, so compress the per-sample interval
        //to fit inside the expected time granularity
        if(dt > Config.native_time_granular){
            dt = Config.native_time_granular;

            //sample-delta doesn't actually matter if there is only one sample, so divide by 1 just to avoid div0
            sd = Duration.ofSeconds(Config.native_time_granular).dividedBy(ct > 1 ? ct-1 : 1);
        }
        else
            sd = Duration.ofSeconds(Config.dt_per_sample);
        
        return new Pair<>(dt, sd);
    }
    
    //We consider this block of records to be continuous, so we will calculate the sample rates betwen time updates
    //and use that to interpolate the times. Then we will extrapolate at both ends using the first and last known sample rates
    //We return true if we were forced to pull default guesses from Config for lack of sufficient data
    static public boolean interpolate_session_times(ArrayList<MeasurementRecord> sess, LogDataSet result) {
        
        int i, ct=sess.size(), di0, di1;
        MeasurementRecord r;
        Duration sd;
        
        //Find the location of the first time delta
        di0 = seek_dt(sess, 0);
        
        //No dt case. Just asssume that the dataset window is centered within a time granularity period
        //of default length, and assume the samples are the default rate, unless they don't fit, in which case we compress them
        if(di0 >= ct){
            
            var guess = guess_dt(ct);
            var dt = guess.first;
            sd = guess.second;
            
            //Now center our data set in the midddle of the granularity window
            var t = sess.get(0).time.plusSeconds( (Config.native_time_granular - dt) / 2);
            
            for(i=0; i<ct; ++i){
                r = sess.get(i);
                r.time = t;
                r.uncertain_time = true;
                result.add(r);
                t = t.plus(sd);
            }
            return true;
        }
        else{
            //Second time delta
            di1 = seek_dt(sess, di0);
            if(di1 >= ct){
                //single dt case. Assume that time is accurate at di0 and extrapolate outward
                var guess0 = guess_dt(di0);
                var sd0 = guess0.second;
                var guess1 = guess_dt(ct - di0);
                var sd1 = guess1.second;
                var tm = sess.get(di0).time;
                var t = tm;
                
                //at di0
                r = sess.get(di0);
                r.uncertain_time = false;   //This one is certain, we think, because it has a new timestamp
                result.add(r);              //The others are not because we're not entirely sure how long they dwell at the same timestamp
                
                //after di0
                for(i=di0-1; i>=0; --i){
                    t = t.minus(sd0);
                    r = sess.get(i);
                    r.time = t;
                    r.uncertain_time = true;
                    result.add(r);
                }
                
                //before di0
                t = tm;
                for(i=di0+1;i<ct;++i){
                    t = t.plus(sd1);
                    r = sess.get(i);
                    r.time = t;
                    r.uncertain_time = true;
                    result.add(r);
                }
                
                return true;
            }
        }
        
        //
        //2 or more deltas
        //
        var sd0 = interpolate_session_times(sess, result, di0, di1);
        sd = sd0;
        
        //Extrapolate at start
        var t = sess.get(di0).time;
        for(i=di0-1;i>=0;--i){
            r = sess.get(i);
            t = t.minus(sd);
            r.time = t;
            r.uncertain_time = false;
            result.add(r);
        }
        
        di0 = di1;
        di1 = seek_dt(sess, di0);
        
        //interpolate in the middle
        while(di1 < ct){
            sd = interpolate_session_times(sess, result, di0, di1);
            di0 = di1;
            di1 = seek_dt(sess, di0);
        }
        
        //extrapolate at the end
        t = sess.get(di0).time;
        for(i = di0+1; i<ct;++i){
            t = t.plus(sd);
            r = sess.get(i);
            r.time = t;
            r.uncertain_time = false;
            result.add(r);
        }
        return false;
    }
    
    static public ArrayList<ArrayList<MeasurementRecord>> divide_into_sessions(LogDataSet data){
        //Ok. Now we divide the dataset into continuous strings of readings, in case the log contains multiple sessions
        //We just search for pauses or lapses
        var sessions = new ArrayList<ArrayList<MeasurementRecord>>();
        var sess = new ArrayList<MeasurementRecord>();
        
        if(data.size() <= 0) return sessions;
        var pr = data.first();
        for(var r : data){    
            //Is there a lapse? If so, push this session and start a new one.
            if(Duration.between(pr.time, r.time).getSeconds() > Config.max_native_time_granular){
                sessions.add(sess);
                sess = new ArrayList<>();
            }
            sess.add(r);
            pr = r;
        }
        
        //There is always one record, so there is always one session
        sessions.add(sess);
        return sessions;
    }
}
