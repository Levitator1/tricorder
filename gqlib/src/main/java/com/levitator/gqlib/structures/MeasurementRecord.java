package com.levitator.gqlib.structures;

import com.levitator.gqlib.TimeFormat;
import com.levitator.gqlib.csv.CSV;
import com.levitator.gqlib.exceptions.GQProtocolException;
import com.levitator.gqlib.exceptions.GQUserDataFormatException;
import com.levitator.gqlib.exceptions.GQFramingError;
import com.levitator.gqlib.exceptions.internal.RecordTruncatedException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.format.DateTimeFormatter;

/*
*
* A record of measurement data
*
*/
public class MeasurementRecord extends NvmRecordBase implements Comparable<MeasurementRecord>{
    
    public static final int framing_id = 0xaa55;
    
    //EMF is in m/G, milligauss
    //EF is in V/m, volts per meter
    //RF seems to be W/m2 scaled by 100,000,000
    static public final String emf_unit = "m/G";
    static public final String ef_unit = "V/m";
    static public final String rf_unit = "mW/m2";
    
    //Dvide by this number to get W/m2
    public final long rf_multiplier = 100000000;
    
    public int emf, emf_10ths; //12 bits, 4 bits
    public float ef, rf;
    public int seq_no = 0; //nth record following last time record
    public boolean uncertain_time = false; //We consulted Config constants to guess at the time interpolation
                                   //Otherwise, we calculated the sample rate from adjacent timestamps
                                   //Or we applied such calculated sample rates to adjacent unknowns at the ends of the dataset
                                   //We feel pretty good about the accuracy of the timestamp if this is false.
    
    //encode single-place fixed decimal as int x 10
    public int get_emfx10(){
       return emf * 10 + emf_10ths;
    }
    
    //mW/m2
    public void set_rf_mWm2(double v){
        rf = rf * (rf_multiplier  / 1000);
    }
    
    public double get_rf_mWm2(){
        return rf  / (rf_multiplier / 1000);
    }
    public void set_rf_Wm2(double v){
        rf = (float)(v * rf_multiplier);
    }
    
    //Watts per square meter
    public double get_rf_Wm2(){
        return rf / rf_multiplier;
    }
    
    //Format a fixed-point single-decimal place value represented as an int multiplied by 10
    static public String format_emfx10(int v){
        //TODO: Observe locale conventions for the decimal point
        return Integer.toString(v / 10) + "." + Integer.toString(v % 10);
    }
    
    static public boolean check_framing(int id){
        return id == framing_id;
    }
    
    @Override
    protected void check_framing() throws GQFramingError{
        if(!check_framing(framing_bytes))
            throw new GQFramingError("Wrong framing bytes for expected MeasurementRecord");
    }
    
    static public int sizeof(){
        return 12;
    }
    
    public MeasurementRecord(String[] fields, DateTimeFormatter[] time_formats) throws RecordTruncatedException, GQUserDataFormatException{
        super(framing_id);
        parse_text(fields, time_formats);
    }
    
    public MeasurementRecord(ByteBuffer data) throws RecordTruncatedException, GQFramingError, GQProtocolException{
        
        super(data);
        try{     
            emf = data.get();
            emf <<= 4;
            emf_10ths = data.get();
            emf |= (emf_10ths >> 4);
            emf_10ths &= 0x0f;

            if(emf_10ths > 9)
                throw new GQProtocolException("EMF 10ths place is more than 9: " + Integer.toString(emf_10ths));
            
            var endian = data.order();
            try{
                data.order(ByteOrder.LITTLE_ENDIAN);
                ef = data.getFloat();
                rf = data.getFloat();
            }
            finally{
                data.order(endian);
            }
        }
        catch(BufferUnderflowException ex){
            throw new RecordTruncatedException(sizeof());
        }
    }

    @Override
    public int compareTo(MeasurementRecord rhs) {
        var comp = time.compareTo(rhs.time);

        if(comp == 0){
            //Time is the same, sort by sequence
            comp = Integer.compare( seq_no, rhs.seq_no );
            return comp;
        }
        else
            return comp; //sorted by time
    }
    
    //Field ordinals for serialization purposes
    private enum fieldnos{
        DATE_TIME(0), TIME_UNCERTAIN(1), SEQ_NO(2), EMF(3), EF(4), RF(5);
        static int count(){ return 6; }
        public final int value;
        fieldnos(int v){ value = v; }
    }
    
    public String [] to_text(DateTimeFormatter fmt){
        var result = new String[fieldnos.count()];
        result[fieldnos.DATE_TIME.value] = fmt.format(time);
        result[fieldnos.TIME_UNCERTAIN.value] = Boolean.toString(uncertain_time);
        result[fieldnos.SEQ_NO.value] = Integer.toString(seq_no);
        result[fieldnos.EMF.value] = format_emfx10(get_emfx10());
        result[fieldnos.EF.value] = Float.toString(ef);
        result[fieldnos.RF.value] = Double.toString(get_rf_mWm2());
        return result;
    }
    
    public void toCSV(DateTimeFormatter fmt, Writer wr) throws IOException{
        CSV.write_record(to_text(fmt), wr);
    }
    
    public void parse_text(String[] fields, DateTimeFormatter[] fmts) throws RecordTruncatedException, GQUserDataFormatException{
        
        if(fields.length < 6)
            throw new RecordTruncatedException(6);
        
        //All exceptions here should necessarily be parsing-related
        try{
            time = TimeFormat.parseDateTime(fields[fieldnos.DATE_TIME.value], fmts);
            uncertain_time = Boolean.parseBoolean(fields[fieldnos.TIME_UNCERTAIN.value]);
            seq_no = Integer.parseInt(fields[fieldnos.SEQ_NO.value]);
            var emf_str = fields[fieldnos.EMF.value];
            var demf = Double.parseDouble(emf_str);
            emf = (int)demf;
            emf_10ths = (int)((demf * 10d) % 10d);
            ef = Float.parseFloat(fields[fieldnos.EF.value]);
            set_rf_mWm2(Double.parseDouble(fields[fieldnos.RF.value]));
        }
        catch(Exception ex){
            var sw = new StringWriter();
            var pw = new PrintWriter( sw );
           
            pw.println("Error parsing field text: " + ex.getMessage());
            pw.print("Field contents: ");
            try{ CSV.write_record(fields, pw);}catch(Exception ex2){}
            pw.flush();
            throw new GQUserDataFormatException(sw.toString(), ex);
        }
    }
    
    static public void write_CSV_header(Writer out) throws IOException{
        final String[] headings = {"Date/Tme", "Uncertain T", "Sequence", "EMF " + emf_unit, "EF " + ef_unit, "RF " + rf_unit};
        CSV.write_record(headings, out);
    }
    
    /*
    //obs
    public void from_CSV(Reader data, DateTimeFormatter[] fmts) throws IOException, RecordTruncatedException, GQUserDataFormatException{
        var record = CSV.read_record(data);
        parse_text(record, fmts);
    }
    */
    
}