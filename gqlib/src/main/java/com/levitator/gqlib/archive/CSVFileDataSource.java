package com.levitator.gqlib.archive;
import com.levitator.obs.FileDateSourceBase;
import com.levitator.gqlib.TimeFormat;
import com.levitator.gqlib.MeasurementRecordCSVFile;
import com.levitator.gqlib.exceptions.GQTimeFormatException;
import com.levitator.gqlib.exceptions.GQUnexpectedException;
import com.levitator.gqlib.structures.MeasurementRecord;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 *
 * An implementation of an archivelib data source keyed by sample time
 * and representing a sequence of MeasurementRecords
 * 
 */
public class CSVFileDataSource extends FileDateSourceBase<LocalDateTime, MeasurementRecord> {
    
    private final DateTimeFormatter[] m_time_formats;
    private Path m_path;
    private MeasurementRecordCSVFile m_reader; //lazy open
    
    public CSVFileDataSource(Path path, DateTimeFormatter[] time_formats) throws GQTimeFormatException{
        m_path = path;
        m_time_formats = time_formats;
        set_range(path);
    }

    @Override
    protected void set_range(Path p) throws GQTimeFormatException {
        try{
            super.set_range(p); //To change body of generated methods, choose Tools | Templates.
        }
        catch(GQTimeFormatException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception parsing archive filename", ex);
        }
    }
    
    @Override
    protected LocalDateTime parse_key(String str) throws GQTimeFormatException{
        return TimeFormat.parseDateTime(str, m_time_formats);
    }

    @Override
    public boolean tryAdvance(Consumer<? super MeasurementRecord> cnsmr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void forEachRemaining(Consumer<? super MeasurementRecord> action) {
        super.forEachRemaining(action); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Spliterator<MeasurementRecord> trySplit() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long estimateSize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long getExactSizeIfKnown() {
        return super.getExactSizeIfKnown(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int characteristics() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasCharacteristics(int characteristics) {
        return super.hasCharacteristics(characteristics); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Comparator<? super MeasurementRecord> getComparator() {
        return super.getComparator(); //To change body of generated methods, choose Tools | Templates.
    }
    
}
