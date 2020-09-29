package com.levitator.util.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class IO {
    
    //A non-critical guess at a reasonable number to use as a multiple for buffer
    //and transaction size
    static public final int BLOCK_SIZE=4096;
    
    //If the stream is marked, then offset will be relative to mark, unless
    //the mark is expired, in which case this is an exception.
    //Note: This turns out to be pretty useless since stock FileInputStreams
    //don't support reset()/mark() and what else are you going to seek, if not
    //a disk file?
    static public void seek(InputStream in, long offs) throws IOException{
        in.reset();
        if(in.skip(offs) != offs)
            throw new IOException("Failed to seek to specified offset");
    }
    
    //Seek to the next instance of the specified byte
    static public long seek_byte(InputStream in, byte target) throws IOException{
        int ch;
        long count = 0;
        
        //Is there no way to clear mark on a stream?
        //Guess we have to do this one character at a time
        do{
            ch = in.read();
            if(ch != -1){
                ++count;
                if(ch == target)
                    break;
            }
            else
                break;
        }while(true);
        return count;
    }
    
    static public int compare_files(File lhs, File rhs) throws FileNotFoundException, IOException{
        var file1 = new BufferedInputStream(new FileInputStream(lhs));
        var file2 = new BufferedInputStream(new FileInputStream(rhs));
        byte[] buf1 = new byte[BLOCK_SIZE];
        byte[] buf2 = new byte[BLOCK_SIZE];
        int result1, result2, result3;
        
        do{
            result1 = file1.readNBytes(buf1, 0, BLOCK_SIZE);
            result2 = file2.readNBytes(buf2, 0, BLOCK_SIZE);
            result3 = Integer.compare(result1, result2);
            if(result3 != 0)
                return result3;
            
            result3 = Arrays.compare(buf1, 0, result1, buf2, 0, result1);
            if(result3 != 0)
                return result3;
        }while( result1 > 0 || result2 > 0 );
        return 0;
    }

}
