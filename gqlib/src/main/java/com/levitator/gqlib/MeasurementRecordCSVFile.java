package com.levitator.gqlib;

import com.levitator.gqlib.csv.CSVFile;
import com.levitator.gqlib.exceptions.GQUserDataFormatException;
import com.levitator.gqlib.exceptions.internal.RecordTruncatedException;
import com.levitator.gqlib.structures.MeasurementRecord;
import com.levitator.util.io.InputSubStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

public class MeasurementRecordCSVFile extends CSVFile {
    private final DateTimeFormatter[] m_time_formats;
    
    public MeasurementRecordCSVFile(InputSubStream<?> reader, DateTimeFormatter[] time_formats) throws IOException{
        super(reader);
        m_time_formats = time_formats;
    }
    
    public MeasurementRecordCSVFile(String path, DateTimeFormatter[] time_formats) throws FileNotFoundException, IOException{
        super(path);
        m_time_formats = time_formats;
    }
    
    public MeasurementRecordCSVFile(Path path, DateTimeFormatter[] time_formats) throws FileNotFoundException, IOException{
        this(path.toString(), time_formats);
    }
        
    public MeasurementRecord next_record() throws IOException, RecordTruncatedException, GQUserDataFormatException{
        return new MeasurementRecord(next_csv_record(), m_time_formats);
    }
    
    //Find the middle of the file, in characters, and find the next immediate record boundary
    //So, split the file roughly in the middle, at a record boundary
    
    
}
