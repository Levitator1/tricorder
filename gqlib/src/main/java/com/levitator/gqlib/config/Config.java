package com.levitator.gqlib.config;

import java.util.Collections;
import java.util.List;

public class Config extends In{
    
    //Currently just determines whether sercat_cmd is told to write an IO log
    //static public final boolean debug = true;
    
    //Native IO process
    static public final List<String> sercat_cmd = Collections.unmodifiableList(List.of(debug ? "./sercat.debug" : "./sercat", "--noint"));
    
    //Separate commands with a delay or the device will drop them
    //Experimentally determined. In ms.
    static public final long command_interval = 85;
    
    //Sometimes we don't know when a response is going to end, so we just
    //wait for a quiescent period to decide we're done
    static public final long io_poll_interval = 10;
    
    //Size of log chunks to retrieve. Mostly influences the rate of progress updates "....etc".
    //Obviously this has to be >= the size of the largest record, but that's just a few bytes
    static public final int log_chunk_size = 4096;
    //static public final int log_chunk_size = 18;
    
    //1 MB
    static public final int log_memory_size = 1024 * 1024;
    
    //Number of times to retry if we get a tiny result buffer reading the log
    //static public final int log_read_retries = 5;
    
    //Due to a firmware bug, we need to interpret this echo string as if it were a terminated line of text
    static public final String[] unterminated_echo_replies = {"->AllInOne"};
    
    //This is how we decide whether there is a lapse in the dataset
    //It is found, experimentally, that the log time updates at an interval that jitters around 3 min +/- ~1 sec
    //So, if we say 3m10s, then 10 seconds should be enough play to avoid false positives without overly distorting the data
    //if there is a vey short break in logging.
    static public final int native_time_granular = 60 * 3; //secs
    static public final int max_native_time_granular = native_time_granular + 10;
    
    //Default sample rate per minute.
    //We will dynamically calculate the sample rate.
    //However, we can't do that for datasets shorter than 9 minutes,
    //so for those, we apply this default found in testing
    //We need this to interpolate the timestamps and increase the time resolution from 3mins provided
    static public final int dt_per_sample = 1; //in seconds
}
