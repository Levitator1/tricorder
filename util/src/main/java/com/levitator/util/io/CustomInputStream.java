package com.levitator.util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * Here, we attempt to make InputStream customizable by defining
 * all read operations in terms of read(byte[], int, int)
 *
 */
public abstract class CustomInputStream extends InputStream{
    static private final int BUF_SIZE = IO.BLOCK_SIZE;
    private final byte[] m_buf = new byte[BUF_SIZE];
    
    /*
    *
    * All other read operations are defined in terms of this method, so this
    * is a single point of override to implement all input operations
    *
    */
    @Override
    public abstract int read(byte[] b, int off, int len) throws IOException;
   
    //Failure is specified to be unrecoverable
    //No -1 on EOF. Just 0 or throw.
    @Override
     public long transferTo(OutputStream out) throws IOException{
        long ct=0;
        int result;
        
        while(true){
            result = read(m_buf);
            if(result <= 0)
                return ct;
            
            out.write(m_buf, 0, result);
            ct += result;
        }
    }

    //Unrecoverable exceptions
    //No -1 for EOF, just 0
    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        int end = off + len;
        int i, result;
        
        for(i = off; i < end; i += result){
            result = read(b, i, end - i);
            if(result <= 0)
                break;
        }
        return i - off;
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        var buf = new ByteArrayOutputStream(BUF_SIZE);
        int result;
        
        while( (result=read(m_buf, 0, Math.min(len, m_buf.length))) > 0 ){
            buf.write(m_buf, 0, result);
        }
        return buf.toByteArray();
    }

    @Override
    //It's weird that some Java offsets are long, especially when the length
    //of an array is limited to positive int
    public byte[] readAllBytes() throws IOException {
        return readNBytes(Integer.MAX_VALUE);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }     

    @Override
    public int read() throws IOException {
        var result = read(m_buf, 0, 1);
        if(result < 0)
            return result;
        else
            return m_buf[0];
    }
}

