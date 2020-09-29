package com.levitator.util.io;

import com.levitator.util.Util;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * Arbitrarily positionable InputStream
 * Also, it supports mark(), with irrelevant read-limit
 * And no playback buffer
 *
 */
public class RandomAccessInputStream extends CustomInputStream {

    private RandomAccessFile m_file;
    private long m_mark;
    
    public RandomAccessInputStream(RandomAccessFile file){
        m_file = file;
    }
    
    //
    // Extension methods
    //
    
    //mark is not reset
    public void file(RandomAccessFile file){
        m_file = file;
    }
    
    public RandomAccessFile file(){
        return m_file;
    }
    
    public long length() throws IOException{
        return m_file.length();
    }
    
    public long position() throws IOException{
        return m_file.getFilePointer();
    }
    
    public void seek(long pos) throws IOException{
        m_file.seek(pos);
    }
    
    public void rewind() throws IOException{
        m_mark = 0;
        seek(0);
    }
    
    //
    // Overrides
    // 
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return m_file.read(b, off, len);
    }

    @Override
    public boolean markSupported() {
        return true;
    }
    
    @Override
    public synchronized void reset() throws IOException {
        seek(m_mark);
    }

    @Override
    public synchronized void mark(int readlimit) {
        
        try{
            m_mark = position();
        }
        catch(Exception ex){
            throw new RuntimeException("Error retrieving file position", ex);
        }
    }

    @Override
    public void close() throws IOException {
        m_file.close();
    }

    @Override
    public int available() throws IOException {
        var result = length() - position();
        return Util.clamp_to_int(result);
    }

    @Override
    public long skip(long n) throws IOException {
        return m_file.skipBytes(Util.clamp_to_int(n));
    }

    //Does toString() do anything special on InputStreams?
    //public String toString();

    //@Override
    //protected Object clone() throws CloneNotSupportedException {
    //   return super.clone();
    //}

    @Override
    public boolean equals(Object obj) {
        return m_file.equals(((RandomAccessInputStream)obj).m_file);
    }

    //@Override
    //public int hashCode();

}
