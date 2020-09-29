package com.levitator.tricorder.features;

import com.levitator.gqlib.GQDevice;
import com.levitator.gqlib.OkFailGuard;
import java.nio.file.Path;
import com.levitator.gqlib.structures.MeasurementRecord;
import com.levitator.util.Pair;
import com.levitator.util.Ref;
import com.levitator.gqlib.LogDataSet;
import com.levitator.gqlib.SessionTimes;
import com.levitator.gqlib.exceptions.GQException;
import com.levitator.gqlib.exceptions.GQIOException;
import com.levitator.gqlib.exceptions.GQInterruptedException;
import com.levitator.gqlib.exceptions.GQUnexpectedException;
import com.levitator.gqlib.exceptions.GQProtocolException;
import com.levitator.gqlib.exceptions.GQFramingError;
import com.levitator.tricorder.config.Config;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import util.guards.Guard;

/*
*
* For dumping the measurment log
*
*/

public class DumpLog {
    
    //Assumes the dataset is sorted, which it is when it comes out of ArrayList<> get_log()
    static public Pair<LocalDateTime, LocalDateTime> time_range(LogDataSet data){
        return new Pair<>( data.first().time, data.last().time );
    }
    
    
    //Generate a name based on the time range of a dataset
    static public Path time_name(Path path, LogDataSet data) throws IOException{
        var times = time_range(data);
        var base = path.getFileName();
        var fmt = Config.file_name_date_time_format;
        var name = fmt.format(times.first) + "_to_" + fmt.format(times.second) + "_" + base;
        return path.toAbsolutePath().getParent().resolve(name);
    }
    
    //Derive a temporary file name for writing from the target path
    static public Path temp_path(Path path){
        
        Path result;
        try{
            result = path.getParent();
            if(!result.toFile().isDirectory())
                throw new IllegalArgumentException("Parent of output path is not a directory: " + path);
            result = result.resolve(path.getFileName() + ".tmp");
            return result;
        }catch(Exception ex){
            throw new IllegalArgumentException("Invalid output path: " + ex.getMessage());
        }
    }
    
    static public void write_log(LogDataSet records, Path path, boolean append) throws IOException{
        var out = System.out;
        var file_str = path.getFileName();
        String disposition = append ? "(append)" : "(new/overwrite)";
        out.print("Opening output file" + disposition + ": " + file_str + " ... ");
        
        BufferedWriter writer;
        try(var guard = new OkFailGuard()){
            writer = new BufferedWriter( new FileWriter(path.toString(), append) );
            guard.status = true;
        }
        
        try(Guard<RuntimeException> on_close=()->out.println("Dump file closed"); writer ){
            MeasurementRecord.write_CSV_header(writer);
            for(var r : records){
                r.toCSV(Config.csv_date_time_format, writer);
            }
        }
    }
    
    static String stats_format(double v){
        return String.format("%.3f", v);
    }
    
    static public LogDataSet dump(GQDevice device) throws 
            GQProtocolException, GQIOException, GQInterruptedException, GQUnexpectedException, GQFramingError{
       
        var out = System.out;
        var bytes_in = new Ref<Integer>(0);
        
        final Guard<GQException> restore_save = 
                LoggingMode.logging_mode_feature(device, "Halting device log for dump... ", false, 
                        (v) -> "Restoring device logging to mode: " + v + " ... " );
        
        LogDataSet data;
        var all_sessions = new SessionTimes();
        var guess_sessions = new SessionTimes();

        try(restore_save){
            try(var okf = new OkFailGuard("Dumping device log at device time: " + Config.format(device.get_time()) + "...")){
                data = device.get_log( () -> out.print("."), bytes_in, all_sessions, guess_sessions );
                okf.status = true;
            }
        }
        catch(GQProtocolException | GQIOException | GQInterruptedException ex){
            throw ex;
        }
        catch(GQException ex){
            throw new GQUnexpectedException("Unexpected exception performing NVM dump", ex);
        }
        
        //Leaves the user viewing the main display
        device.navigate_home();
        
        //Don't restore the log mode here because if we exit, it gets restsored there
        //And if we go into scheduled mode, it immediately gets forced on, and then restored on exit

        if(data.size() == 0){
            out.println("0 records. Log was empty.");
            return data;
        }
        
        var times = time_range(data);
        var fmt = Config.csv_date_time_format;
        out.println("" + data.size() + " records and " + (bytes_in.value / 1024) + "kB retrieved for the period " + 
                fmt.format(times.first) + " - " + fmt.format(times.second));
        
        out.println("Logging sessions found:");
        for(var s : all_sessions){
            out.println("\t" + Config.format(s.getStart()) + " - " + Config.format(s.getEnd()));
        }
        
        if(guess_sessions.size() > 0){
            out.println();
            out.println("The log contained 1 or more sessions which were too short to interpolate the timestamps without guessing at parameters.");
            out.println("If you want to avoid this, then make sure log sessions are more than about 10 minutes, though this may vary by model.");
            out.println("We assume values observed with an EMF-390.");
            
            for(var s : guess_sessions){
                out.println("\t" + Config.format(s.getStart()) + " - " + Config.format(s.getEnd()));
            }
            out.println();
        }
        
        int emf_min = Integer.MAX_VALUE, emf_max = Integer.MIN_VALUE;
        double emf_sum = 0;
        float ef_min = Float.MAX_VALUE, ef_max = Float.MIN_VALUE;
        double ef_sum = 0;
        double rf_min = Double.MAX_VALUE, rf_max = Double.MIN_VALUE;
        double rf_sum = 0;
        for(var r : data){
            emf_min = Math.min(emf_min, r.get_emfx10());
            emf_max = Math.max(emf_max, r.get_emfx10());
            emf_sum += r.get_emfx10();
            
            ef_min = Math.min(ef_min, r.ef);
            ef_max = Math.max(ef_max, r.ef);
            ef_sum += r.ef;
            
            var rf = r.get_rf_mWm2();
            rf_min = Math.min(rf_min, rf);
            rf_max = Math.max(rf_max, rf);
            rf_sum += rf;
        }
        
        emf_sum /= (data.size() * 10);
        ef_sum /= data.size();
        rf_sum /= data.size();

        out.println();
        out.println("min/avg/max");
        out.println("RF " + MeasurementRecord.rf_unit + ": " + stats_format(rf_min) + "/" + stats_format(rf_sum) + "/" + stats_format(rf_max));
        out.println("EF " + MeasurementRecord.ef_unit + ": " + stats_format(ef_min) + "/" + stats_format(ef_sum) + "/" + stats_format(ef_max));
        out.println("EMF " + MeasurementRecord.emf_unit + ": " + MeasurementRecord.format_emfx10(emf_min) +
                "/" + stats_format(emf_sum) + "/" + MeasurementRecord.format_emfx10(emf_max));
        out.println();
        
        return data;
    }
    
    static public void do_clear_log(GQDevice device) throws 
            GQProtocolException, GQIOException, GQInterruptedException{
        
        var out = System.out;
        try(var okf = new OkFailGuard() ){
            out.print("Clearing device log memory...");
            device.clear_log();
            okf.status = true;
        }
    }
    
    static public void feature_dump(GQDevice device, Config conf) throws IOException, GQIOException,
               GQInterruptedException, GQUnexpectedException, GQProtocolException, GQFramingError{
        
        var out = System.out;
        var data = dump(device);
        var append = conf.is_append_dump();
        
        var path = conf.get_dump_path();
        if(!conf.is_single_file()){
            if(data.size() == 0){
                out.println("Device log was empty, so we can't generate an output filename without a time range.");
                out.println("No file written.");
                return;
            }
            
            path = time_name(path, data);
            if(path.toFile().exists()){
                out.println("Warning: Generated file name already exists and this is surprising: " + path);
                out.println("Append mode is forced for this file.");
                append = true;
            }
        }
        
        var ppath = path.toAbsolutePath().getParent();
        if(!ppath.toFile().exists())
            throw new IOException("Output directory does not exist");
        
        write_log(data, path, append); //Will at least create an empty file even if there are no records
        if(conf.is_clear_after_dump()) do_clear_log(device);
    }

    public static void do_log_features(Config conf, GQDevice dev) throws Exception {

    }
    
}
