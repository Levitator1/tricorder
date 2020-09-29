package com.levitator.util.io;

import com.levitator.util.Util;
import com.levitator.util.function.ThrowingFunction;
import com.levitator.util.function.ThrowingSupplier;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;


/**
 * An InputStream which represents a subset of another
 * This was originally going to be character-oriented and 
 * then I remembered how random-access would crawl in that case,
 * because an MBCS has to be scanned from the beginning to reach any given index.
 * So it's ASCII and bytes instead.
 * 
 * Also, this was going to work as a layer over arbitrary InputStreams, but there
 * are problems with that. First of all, the stream has to support mark(), which,
 * counterintuitively, not even FileInputStream does. Then, furthermore, for this to be
 * general, it has to be implemented by monopolizing the mark() mechanism, which is kind broken,
 * to begin with, and then it's based on int indexes, limiting file sizes to 2GB.
 * So, to keep things simple, let's just implement this as a layer over RandomAccessInputStream.
 *
 */
public class InputSubStream extends RandomAccessInputStream{

    static protected final int ABSOLUTE=0;
    
    //A function that accepts a a RandomAccessFile and returns a RandomAccessFile
    //so as to either copy the file passed to it, or to produce an equivalent one.
    //Here, we can even return the original file without reopening it, for serial operations
    //But then it is necessary to reposition the file when switching from one substream to
    //another (if having the same underlying file). Otherwise, the intended usage is to copy
    //or reopen the stream so that the substreams can be used in parallel threads.
    private ThrowingFunction<RandomAccessFile, RandomAccessFile, IOException> m_file_f;
    
    //Both in terms of the underlying stream
    private long m_begin; //[begin, end)
    private long m_end; //[begin, end)
    
    //flattening constructor so that logically nested substreams are not implemented recursively
    protected InputSubStream(InputSubStream other, long abs_begin, long abs_end, int unused_absolute_offsets) throws IOException{
        super(other.m_file_f.apply(other.file()));
        if(abs_begin > abs_end)
            throw new IllegalArgumentException("range ends before it begins");
        
        m_file_f = other.m_file_f;
        m_begin = abs_begin;
        m_end = abs_end;
        reset();
    }
    
    public InputSubStream(InputSubStream other, long begin, long end) throws IOException{
        this(other, other.check_abs_offs( other.m_begin + begin  ), other.check_abs_offs(other.m_begin + end), ABSOLUTE );
    }
    
    public InputSubStream(InputSubStream other, long begin) throws IOException{
        this(other, begin, other.length());
    }
    
    public InputSubStream(InputSubStream other) throws IOException{
        this(other, 0, other.length());
    }
  
    public InputSubStream(RandomAccessFile file, ThrowingFunction<RandomAccessFile, RandomAccessFile, IOException> file_copy_f,
            long begin, long end) throws IOException{
        
        super( file );
        m_file_f = file_copy_f;
        m_begin = begin;
        m_end = end;
        
        //top-level SubStream can point beyond file end
        if(m_begin < 0)
            throw new RuntimeException("offset out of range");
        
        if(m_end < m_begin)
            throw new RuntimeException("end offset comes before beginning");
        
        reset();
    }
    
    public InputSubStream(RandomAccessFile file, ThrowingFunction<RandomAccessFile, RandomAccessFile, IOException> copy_f, long begin) throws IOException{
        this(file, copy_f, begin, file.length() );
    }
    
    public InputSubStream(ThrowingFunction<RandomAccessFile, RandomAccessFile, IOException> copy_f, long begin) throws IOException{
        this(copy_f.apply(null), copy_f, begin);
    }
    
    public InputSubStream(ThrowingFunction<RandomAccessFile, RandomAccessFile, IOException> copy_f) throws IOException{
        this(copy_f.apply(null), copy_f, 0);
    }
    
    public InputSubStream(ThrowingFunction<RandomAccessFile, RandomAccessFile, IOException> copy_f, long begin, long end) throws IOException{
        this(copy_f.apply(null), copy_f, begin, end );
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        
        final var avail=available();
        if( avail <= 0 )
            return -1;
        
        return super.read(b, off, Math.min(len, avail) );
    }
    
    private long check_abs_offs(long flat_pos){
        if( flat_pos != Util.clamp(flat_pos, m_begin, m_end)  )
            throw new IllegalArgumentException("absolute offset out of range");
        
        return flat_pos;
    }
    
    private long check_logical_offs(long log_pos){
        if(log_pos > length() || log_pos < 0)
            throw new IllegalArgumentException("logical offset out of range");
        
        return log_pos;
    }
    
    @Override
    public long length(){
        return m_end - m_begin;
    }
    
    @Override
    public long position() throws IOException{
        return check_logical_offs(super.position() - m_begin);
    }
    
    @Override
    public void seek(long pos) throws IOException{      
        super.seek( check_abs_offs(m_begin + pos) );
    }
    
    @Override
    public void rewind() throws IOException{
        seek(0);
        mark(Integer.MAX_VALUE); //closest native Java concept to clearing mark()
    }
    
    private void left_bisect(long offs) throws IOException{ 
        m_end = check_abs_offs(m_begin + offs);
        rewind();
    }
    
    //Truncates this stream at the specified offset
    //and returns a new stream representing the remaining half.
    //Both streams are positioned at the beginning with mark cleared unless
    //the underlying file is shared, then it is positioned at the start of the left side
    public InputSubStream bisect(long offs) throws IOException{
        var result = new InputSubStream(this, offs);
        left_bisect(offs);
        rewind();
        return result;
    }
    
    //Bisect, but do the right half of the bisection lazily, for worker threads to do.
    //This should probably only be used with non-shared instances of the backing file.
    //Because a shared file will have its position reset when the lazy right-side is completed.
    //and that is probably going to be a race in a parallel scenario.
    //How expensive is it to reopen a file?
    public ThrowingSupplier<InputSubStream, IOException> lazy_bisect(long offs) throws IOException{
        final var lbegin_abs = m_begin + offs;
        final var lend_abs = m_begin + length();
        left_bisect(offs);
        rewind();
        
        //Use absolute positions because the left-side has mutated already
        return () -> new InputSubStream(this, lbegin_abs, lend_abs, ABSOLUTE);
    }

    //Divide the stream into ct streams. Element 0 is this.
    //If stream does not divide evenly, then first element contains the remainder.
    public InputSubStreamArray divide(int ct) throws IOException{
        
        var sz = length();
        var szeach = sz / ct;
        if(szeach < 1){
           var result = new InputSubStreamArray(1);
           result.add( () -> this );
           return result;
        }
        
        var result = new InputSubStreamArray(ct);
        result.add( ()->this );
        Util.fill(result, null, ct - 1);
        
        for(var i=ct-1; i >= 0; --i){
            result.set(i, lazy_bisect(length() - szeach) );
        }
        
        return result;
    }
    
    @Override
    //Unlike the usual available(), this is not an estimate, it is the total
    //available
    public int available() throws IOException {
        return (int)Math.min(length() - position(), Integer.MAX_VALUE);
    }
    
    @Override
    public long skip(long n) throws IOException {
        
        //available() is an implicit check of position range
        n = Math.min(n, available());
        return super.skip(n);
    }

    @Override
    public void mark(int readAheadLimit){
        try{
            check_abs_offs(super.position());
        }
        catch(IOException ex){
            throw new RuntimeException("got IOException retrieving file position");
        }
        super.mark(readAheadLimit);
    }
        
    @Override
    public void reset() throws IOException{
        super.reset();
        position(); //checks range as side-effect
    }

    @Override
    public boolean equals(Object obj) {
        return file().equals(((InputSubStream)obj).file());
    }

    //Does toString() do anything special for streams?
    //@Override
    //public String toString();
}
