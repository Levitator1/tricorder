
package com.levitator.gqlib.csv;

import com.levitator.util.Pair;
import com.levitator.util.io.InputSubStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;


public class CSVFile {
    private final InputSubStream<?> m_input;
    private final String[] m_header;
    private final Pair<String, Boolean> m_temp_field = new Pair<>(null, false);
    
    
    //Internal constructor for bisection
    protected CSVFile(InputSubStream<?> reader) throws IOException{
        m_input=reader;
        m_header = null;
        //m_header = init(); //don't read a header if we are being bisected
    }
    
    public CSVFile(String path) throws FileNotFoundException, IOException{
        m_input = new InputSubStream<BufferedInputStream>(
                new BufferedInputStream( new FileInputStream(path)),
                (ignore) -> new BufferedInputStream( new FileInputStream(path))
        );
        m_header = init();
    }
    
    public CSVFile(Path path, DateTimeFormatter[] time_formats) throws FileNotFoundException, IOException{
        this(path.toString());
    }
    
    private String[] init() throws IOException{
        //read the header
        return CSV.read_record(m_input, m_temp_field);
    }
    
    public String[] next_csv_record() throws IOException{
        return CSV.read_record(m_input, m_temp_field);
    }
    
    public String[] get_header(){
        return m_header;
    }
    
    //
    // IO
    //
    
    //Restart the underlying stream to the beginning
    public void rewind() throws IOException{
        m_input.seek(0);
    }
    
    //Skip count characters and then stream forward to resync to a record boundary (or eof)
    //Leaves both halves at start of stream
    public long bisect() throws IOException{
        
        var half = m_input.size();
        m_input.seek(half);
        
        //TODO ***************
        
        
        //Scan for a newline
        int ch;
        long count = 0;
        do{
            ch = m_input.read();
            if(ch != -1){
                ++count;
                if(ch == '\n')
                    break;
            }
            else
                break;
        }while(true);
        return count;
    }
}
