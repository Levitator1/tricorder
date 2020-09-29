/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.levitator.gqlib.csv;

import com.levitator.util.Pair;
import com.levitator.util.Util;
import static com.levitator.util.Util.eat_ws;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;

/**
 *
 * @author j
 */
public class CSV {
    
    //Returns true if at the end of a line.
    //Accepts LF, CRLF, or EOF as end-of-line
    static private boolean check_eol(int ch, Reader data) throws IOException{
        if(ch == '\n' || ch == -1) return true;
        if(ch == '\r'){
            ch = data.read();
            if(ch == -1 || ch == '\n')
                return true;
            else
                throw new IllegalArgumentException("Malformed end-of line. Found CR, no LF.");
        }
        else{
            return false;
        }
    }
    
    static public boolean check_end_of_field(int ch, int ech, Reader data) throws IOException{
        if(ch == ech) return true;
        if( check_eol(ch, data) ){
            //Unquoted case
            if(ch != ',')
                throw new IllegalArgumentException("Unexpected end of record");
            else
                return true;
        }
        return false;
    }
    
    //Returns <field, end-of-record>
    static public Pair<String, Boolean> read_field(Reader data) throws IOException{
        var result = new StringBuffer();
        int ech=',';
        var ch = eat_ws(data);
        if(ch == '"')
            ech = '"';
        
        while(!check_end_of_field(ch = data.read(), ech, data)){            
            result.append(ch);
        }

        return new Pair<>(result.toString(), check_eol(ch, data));
    }
    
    public static void write_field(String data, Writer buf, int i) throws IOException{
        if(data.contains("\""))
            throw new IllegalArgumentException("CSV field must not contain the double-quote character '\"'");
        
        boolean need_comma = i > 0;
        if(need_comma) buf.append(',');
        buf.append('"');
        buf.append(data);
        buf.append('"');
    }
    
    public static void finish_parsing_record(Reader data) throws IOException{
        int ch=eat_ws(data);
        if(!check_eol(ch, data)){
            throw new IllegalArgumentException("Expected end of record");
        }
    }
    
    public static void finish_writing_record(Writer writer) throws IOException{ writer.append('\n'); }
    
    static public String[] read_record(Reader data) throws IOException{
        var result = new ArrayList<String>();
        Pair<String, Boolean> field;
        
        do{
            field = read_field(data);
            result.add(field.first);
        }while(!field.second);
        return result.toArray((ct)->new String[ct]);
    }
    
    static public void write_record(String[] fields, Writer writer) throws IOException{
        int i=0;
        for( var f : fields ){ write_field(f, writer, i++);}
        finish_writing_record(writer);
    } 
}
