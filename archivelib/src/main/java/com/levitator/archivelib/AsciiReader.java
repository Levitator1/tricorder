package com.levitator.archivelib;

import com.levitator.util.Util;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/*
*
* Assumes that every character is one byte wide and converts it to positive char
* This is better than a multi-byte reader because it is very fast for random access,
* assuming you don't need any multi-byte characters
*
* Also, we do not buffer between operations or hold any stream content state, so the underlying
* InputStream can be repositioned and AsciiReader will continue reading wherever the InputStream is pointing
*
*/
public class AsciiReader extends Reader{

    static private final int BUFFER_SIZE=4096;
    private InputStream m_input;
    private final ByteBuffer m_inbuf = ByteBuffer.allocate(BUFFER_SIZE); //4096 is a typical page/block transfer size
    
    public AsciiReader(InputStream in_stream){
        m_input = in_stream;
    }
    
    //Underlying input stream is public because operations on its position are well-defined
    public InputStream stream(){
        return m_input;
    }
    
    public void stream(InputStream in){
        m_input = in;
    }
    
    
    //Pull up to ct characters from the underlying input stream
    private int buffer(int ct) throws IOException{
        m_inbuf.position(0);
        ct = Math.min(ct, m_inbuf.capacity());
        var result = m_input.read( m_inbuf.array(), 0, ct );
        if(result > 0)
            m_inbuf.position(result);
   
        return result;
    }
    
    @Override
    public int read(char[] chars, int i, int len) throws IOException {
        var result = buffer(len);
        if(result < 0)
            return result;
        Util.bytes2positivechar(m_inbuf, chars, i);
        return result;
    }

    //Is specified to return "the number of bytes transferred", so no -1
    //Also, failure is unrecoverable, so don't bother
    @Override
    public long transferTo(Writer out) throws IOException {
        final char[] m_outbuf = new char[m_inbuf.capacity()];
        long ct=0;
        
        while(true){
            var result = buffer(m_inbuf.capacity());
            if(result <= 0)
                return ct;
            
            Util.bytes2positivechar(m_inbuf, m_outbuf);
            out.write(m_outbuf, 0, result);
            ct += result;
        }
    }

    @Override
    public void reset() throws IOException {
        m_input.reset();
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        m_input.mark(readAheadLimit);
    }

    @Override
    public boolean markSupported() {
        return m_input.markSupported();
    }

    @Override
    public long skip(long n) throws IOException {
        return m_input.skip(n);
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    @Override
    public int read() throws IOException {
        int b = m_input.read();
        if(b == -1)
            return -1;
        else{
            return Util.byte2positiveshort((byte)b);
        }
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        
        int result, ct=0;
        while(target.remaining() > 0){
            result = read(target.array(), target.position(), target.remaining());
            if(result < 0)
                return result;
            target.position( target.position() + result );
            ct += result;
        }
        return ct;
    }

    @Override
    public String toString() {
        return m_input.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return m_input.equals(obj); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int hashCode() {
        return m_input.hashCode(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close() throws IOException {
        m_input.close();
    }
    
}
