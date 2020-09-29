package com.levitator.util.io;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * A base class for layering features over another stream, but preserves
 * the CustomInputStream property that all reads are implemented in
 * terms of read(byte[], int, int)
 *
 */
public class InputStreamFilter extends CustomInputStream {
    
    private InputStream m_stream;
    
    public InputStreamFilter(InputStream stream){
        m_stream = stream;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException{
        return m_stream.read(b, off, len);
    }
    
    protected InputStream stream(){
        return m_stream;
    }
    
    protected void stream(InputStream stream){
        m_stream = stream;
    }
    
     @Override
    public boolean markSupported() {
        return m_stream.markSupported();
    }

    @Override
    public synchronized void reset() throws IOException {
        m_stream.reset();
    }

    @Override
    public synchronized void mark(int readlimit) {
        m_stream.mark(readlimit);
    }

    @Override
    public void close() throws IOException {
        m_stream.close();
    }

    @Override
    public int available() throws IOException {
        return m_stream.available();
    }

    @Override
    public long skip(long n) throws IOException {
        return m_stream.skip(n);
    }
}
