package com.levitator.tricorder.config;

import com.levitator.gqlib.TimeFormat;
import com.levitator.gqlib.exceptions.GQTimeFormatException;
import com.levitator.tricorder.ConfigException;
import com.levitator.util.Ref;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import com.levitator.util.Timer;
import com.levitator.util.levArrays;
import com.levitator.util.streams.Streams;
import java.time.format.DateTimeFormatter;
import static java.time.temporal.ChronoUnit.SECONDS;
import java.util.Map;
import java.util.Map.Entry;
import static java.util.Map.entry;
import java.util.TreeMap;
import java.util.stream.Stream;

public class Config extends In {
    
    static public final String version_string = "V1.0RC2";
    
    //When true, display interrupt exceptions as errors
    //Otherwise, attempt a clean exit
    static public final boolean stack_trace_on_interrupt = debug;
    
    //public static Config instance = new Config()
    static private final String[] time_format_strings = {"h:mm:ssa", "k:mm:ss"};
    
    static private final String[] date_time_format_strings = Stream.concat(
            Streams.joinStrings(
                Stream.of("yyyy/M/d ", "yyyy-M-d "),
                Stream.of("h:mm:ssa", "kk:mm:ss", "h:mm:ss.Sa", "kk:mm:ss.S")),
            Streams.joinStrings( 
                    Stream.of("dd M yyyy"),
                    Stream.of("kkmm:ss", "kkmm:ss.S") )
        )
        .toArray((ct)->new String[ct]);
            
    static private DateTimeFormatter[] make_formatters(String [] strs){
        return Arrays.stream(strs).map( fmt -> DateTimeFormatter.ofPattern(fmt)  ).toArray( ct -> new DateTimeFormatter[ct] );
    }
    
    static private final DateTimeFormatter[] time_formats = make_formatters(time_format_strings);
    static private final DateTimeFormatter[] date_time_formats = make_formatters(date_time_format_strings);
    
    static public final DateTimeFormatter display_time_format = DateTimeFormatter.ofPattern("h:mm:ss.Sa");
    static public final DateTimeFormatter display_date_time_format = DateTimeFormatter.ofPattern("yyyy/M/d h:mm:ss.Sa");
    static public final DateTimeFormatter csv_date_time_format = DateTimeFormatter.ofPattern("yyyy/M/d h:mm:ssa");
    static public final DateTimeFormatter file_name_date_time_format = DateTimeFormatter.ofPattern("yyyy-MM-dd-kk-mm-ss");
    static public final DateTimeFormatter time_period_format = DateTimeFormatter.ofPattern("kk:mm:ss");
    
    private final String[] arguments;
    private final Timer start_time; //Remember what time we launched
    private boolean set_time = false;
    private LocalDateTime time_to_set = null; //Sends current system time to device if null
    
    private boolean show_help = false;
    private Path device_path = Path.of("/dev/ttyUSB0");
    private Path dump_path = Path.of("emf_data.csv");
    private boolean append_dump = true;
    private boolean single_file = true;    //Keep writing to the same file if true, or name a new file for each time range
    private boolean dump_log_now = false;    //Request the log dump action
    private boolean clear_after_dump = false;   //Clear the device log. Occurs after each dump, if requested.
    private boolean clear_log_now = false;       //Clear the device log immediately upon startup
    private LocalTime dump_period = null; //Time period to wait between dumps
    private LocalTime[] dump_times;      //Comma list of local times at which to dump the log.
    private Boolean set_log_mode = null; //On startup, enable/disable logging to device NVM
    
    static boolean is_switch(String arg){
        return arg.startsWith("-");
    }
    
    //If the next argument is present and it is a non-switch, then return it
    //and advance the index
    static String next_non_switch(String[] args, Ref<Integer> i){
        var i2 = i.value + 1;
        if(i2 >= args.length) return null;
        var arg = args[i2];
        if(is_switch(arg)) return null;
        ++i.value;
        return arg;
    }
    
    //throw if missing
    static String demand_next_non_switch(String[] args, Ref<Integer> i) throws ConfigException{
        var result = next_non_switch(args, i);
        if(result == null)
            throw new ConfigException("switch is missing required argument");
        else
            return result;
    }
    
    static public void usage(){
        var out = System.out;
        var defaults = new Config();
        
        out.println("Usage: tricorder [-h|--help] [<-t|--time> <time string>] [-T|--systime] [-u|--unique] [-o|--overwrite] [-d|--dump]");
        out.println("\t[-c|--clear] [<-p|--period> <duration>] [--schedule] [-f|--file] [<-l|--log> <true|false>] [device path]");
        out.println();
        out.println("\t-t|--time: set the device time as specified by <time string>");
        out.println("\t-T|--systime: set the device time to the current host system time");
        out.println("\t-u|--unique: Generate a date/time range-based filename for each log dump instead of appending to the same file");
        out.println("\t-o|--overwrite: overwrite dump file instead of appending");
        out.println("\t-d|--dump: dump the device log immediately");
        out.println("\t-c|--clear: clear the device log memory after each dump");
        out.println("\t-C|--CLEAR: clear the device log memory immediately. Happens after -d dump,  if specified.");
        out.println("\t-p|--period: periodically dump (and possibly clear) the log at the interval specified");
        out.println(    "\t\texample: -p " + LocalTime.of(23, 59, 59));
        out.println("\t--shedule: specify a list of times-of-day to perform a dump (and clear, if specified)");
        out.println("\t-f|--file: log dump output file. If -u is specified, then the file-part of this path is the suffix used for the name");
        out.println("\t\tDefault: " + defaults.dump_path);
        out.println("\t-l|--log: flip device logging mode on or off. This is overriden if any non-immediate dumps are scheduled,");
        out.println("\t\tin which case logging is forced on during the wait. Why would you schedule dumps if you aren't logging?");
        out.println("\t[device path]: path of serial device to use for I/O. Default: " + defaults.device_path);
        out.println();
    }
    
    private interface switch_handler{
        void apply (Config conf, Ref<Integer> i) throws Exception;
        static public Entry<String, switch_handler> entry(String key, switch_handler f){
            return Map.entry(key, f);
        }
    }
    //TreeMap<String, switch_handler> switch_handlers = Map.ofEntries();
    
    static private void process_time_switch(Config conf, Ref<Integer> i) throws Exception{
        try{
            conf.set_time = true;
            conf.time_to_set = TimeFormat.parseDateTime(demand_next_non_switch(conf.arguments, i), Config.date_time_formats);
        }
        catch(Exception ex){
            throw new Exception("must be followed by a valid date/time: " + ex.toString());
        }

        if(conf.time_to_set == null)
            throw new Exception("No time/date specified");
    }
    
    static private void process_auto_time_switch(Config conf){
        //Set clock to current system time
        conf.set_time = true;
        conf.time_to_set = null;
    }
    
    static private void process_unique_file_switch(Config conf){
        //Select distinct/seperate output files
        conf.single_file = false;
    }
    
    static private void process_overwrite_switch(Config conf){
        conf.append_dump = false;
    }
    
    static private void process_dump_now_switch(Config conf){
        //Dump log right away
        conf.dump_log_now = true;
    }
    
    static private void process_clear_log_switch(Config conf){
        //Clear log after each dump, or right away if no dumps requested
        conf.clear_after_dump = true;
    }
    
    static private void process_period_switch(Config conf, Ref<Integer> i) throws Exception{
        var period_arg = demand_next_non_switch(conf.arguments, i);

        try{
            conf.dump_period = parsePeriod(period_arg);
        }
        catch(Exception ex){
            throw new Exception("must be followed by a valid time duration");
        }
    }
    
    static private void process_schedule_switch(Config conf, Ref<Integer> i) throws Exception{
        var times_arg = next_non_switch(conf.arguments, i);
        try{
            var time_strings = times_arg.split(",");

            conf.dump_times = new LocalTime[time_strings.length];
            int j = 0;
            for(var ts : time_strings){
                conf.dump_times[j++] = TimeFormat.parseTime(ts, Config.time_formats);
            }
            Arrays.sort(conf.dump_times, Comparator.<LocalTime>naturalOrder() );
        }
        catch(Exception ex){
            throw new Exception("must be followed by 1 or more valid times of day, comma-separated");
        }
    }
    
    static private void process_file_switch(Config conf, Ref<Integer> i) throws Exception{
        try{
            conf.dump_path = Path.of(next_non_switch(conf.arguments, i));
        }
        catch(Exception ex){
            throw new Exception("must be followed by a valid ouput file path: " + ex.toString());
        }
    }
    
    static private void process_log_mode_switch(Config conf, Ref<Integer> i) throws Exception{
        var setting = next_non_switch(conf.arguments, i);
        if(setting == null) throw new Exception("must specify true or false");
        try{
            conf.set_log_mode = Boolean.parseBoolean(setting);
        }catch(Exception ex){
            throw new Exception("could not parse true/false argument");
        }
    }
    
    static private void process_help_switch(Config conf){
        conf.show_help = true;
    }

    static private void process_clear_log_now_switch(Config conf){
        conf.clear_log_now = true;
    }
    
    //from name, to name
    static private TreeMap<String, switch_handler>  process_synonyms( Map<String, String> syn, Map<String, switch_handler>  in){
        
        var result = new TreeMap<String, switch_handler>(in);
        for(var ent : syn.entrySet() ){
            result.put(ent.getValue(), in.get(ent.getKey()));
        }
        
        return result;
    }
    
    static private final TreeMap<String, String> synonyms = new TreeMap<>(Map.<String, String>ofEntries(
            entry("-t","--time"),
            entry("-T", "--systime"),
            entry("-u", "--unique"),
            entry("-o", "--overwirte"),
            entry("-d", "--dump"),
            entry("-c", "--clear"),
            entry("-p", "--period"),
            entry("-f", "--file"),
            entry("-h", "--help"),
            entry("-l", "--log"),
            entry("-C", "--CLEAR")
    ));
    
    static private final TreeMap< String, switch_handler> switch_handlers = new TreeMap<>(process_synonyms(synonyms, Map.<String, switch_handler>ofEntries(
            switch_handler.entry("-t", (conf, i)        -> process_time_switch(conf, i) ),
            switch_handler.entry("-T", (conf, i)        -> process_auto_time_switch(conf)),
            switch_handler.entry( "-u", (conf, i)       ->  process_unique_file_switch(conf) ),
            switch_handler.entry("-o", (conf, i)        -> process_overwrite_switch(conf)),
            switch_handler.entry("-d", (conf, i)        -> process_dump_now_switch(conf) ),
            switch_handler.entry("-c", (conf, i)        -> process_clear_log_switch(conf) ),
            switch_handler.entry( "-p", (conf, i)       -> process_period_switch(conf, i)),
            switch_handler.entry("--schedule", (conf, i)-> process_schedule_switch(conf, i)),
            switch_handler.entry("-f", (conf, i)        -> process_file_switch(conf, i) ),
            switch_handler.entry("-h", (conf, i)        -> process_help_switch(conf) ),
            switch_handler.entry("-l", (conf, i)        -> process_log_mode_switch(conf, i)),
            switch_handler.entry("-C", (conf, i)        -> process_clear_log_now_switch(conf))
    )));        
    
    private void process_switch(Ref<Integer> i) throws ConfigException, Exception{
        
        var arg = arguments[i.value];
        var handler = switch_handlers.get(arg);
        
        if(handler == null)
            throw new ConfigException("Unrecognized switch: " + arg);
        else
            handler.apply(this, i);
    }
    
    public void process_args() throws Exception, ConfigException{
        Ref<Integer> i = new Ref<>();
        
        //process switches
        for(i.value=0; i.value < arguments.length; ++i.value){
            var arg = arguments[i.value];
            if(is_switch(arg)){
                try{
                    process_switch(i);
                }
                catch(ConfigException ex){
                    throw ex;
                }
                catch(Exception ex){
                    throw new ConfigException(arg + ": " + ex.getMessage());
                }
            }
            else
                break;
        }
        
        if(dump_times == null)
            dump_times = new LocalTime[0];
          
        var remain = arguments.length - i.value;
        
        if(remain > 1)
            throw new ConfigException("Too many non-switch arguments");
        
        if(remain > 0)
            device_path = Path.of(arguments[i.value]);
    }
 
    //Useful for retrieving defaults
    public Config(){ 
        start_time = null;
        arguments = null;
    }

    public static String[] get_time_format_strings() {
        return levArrays.copyOf(time_format_strings);
    }

    public static String[] get_date_time_format_strings() {
        return levArrays.copyOf(date_time_format_strings);
    }

    public static DateTimeFormatter[] get_time_formats() {
        return levArrays.copyOf(time_formats);
    }

    public static DateTimeFormatter[] get_date_time_formats() {
        return levArrays.copyOf(date_time_formats);
    }

    //Believe DateTimeFormatters are immutable and final, so don't need an accessor

    public String[] get_arguments() {
        return levArrays.copyOf(arguments);
    }

    public Timer get_start_time() {
        return start_time.clone();
    }

    public boolean is_set_time() {
        return set_time;
    }

    public LocalDateTime get_time_to_set() {
        return time_to_set;
    }

    public boolean is_show_help() {
        return show_help;
    }

    public Path get_device_path() {
        return device_path;
    }

    public Path get_dump_path() {
        return dump_path;
    }

    public boolean is_append_dump() {
        return append_dump;
    }

    public boolean is_single_file() {
        return single_file;
    }

    public boolean is_dump_log_now() {
        return dump_log_now;
    }

    public boolean is_clear_after_dump() {
        return clear_after_dump;
    }

    public boolean is_clear_log_now() {
        return clear_log_now;
    }

    public Duration get_dump_period() {
        if(dump_period != null)
            return Duration.of(dump_period.toSecondOfDay(), SECONDS);
        else
            return null;
    }

    public LocalTime[] get_dump_times() {
        return levArrays.copyOf(dump_times);
    }

    public Boolean get_set_log_mode() {
        return set_log_mode;
    }
    
    public Config(String[] args, Timer start) throws Exception, ConfigException{
        start_time = start;
        arguments = args;
        process_args();
    }
    
    //public Config(Timer timer){
    //    start_time = timer;
    //}
    
    static public LocalDateTime parseDateTime(String t) throws GQTimeFormatException{ return TimeFormat.parseDateTime(t, date_time_formats); }
    static public LocalTime parseTime(String t) throws GQTimeFormatException{ return TimeFormat.parseTime(t, time_formats); }
    static public LocalTime parsePeriod(String t) throws GQTimeFormatException{ return LocalTime.from(time_period_format.parse(t)); }
    static public String format(LocalDateTime t){ return TimeFormat.format(t, display_date_time_format ); }
    static public String format(LocalTime t){ return TimeFormat.format(t, display_time_format); }
    static public String formatPeriod(LocalTime t){ return TimeFormat.format(t, time_period_format); }
}
