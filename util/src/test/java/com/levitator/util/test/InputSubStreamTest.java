package com.levitator.util.test;

import com.levitator.util.ThrowingActionThread;
import com.levitator.util.function.ThrowingAction;
import com.levitator.util.function.ThrowingSupplier;
import com.levitator.util.io.IO;
import com.levitator.util.io.InputSubStream;
import com.levitator.util.io.InputSubStreamArray;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

public class InputSubStreamTest {
    
    static public final int THREADS=8;
    static public final int TEST_DATA_SIZE = 1024 * 1024 * 128;
    static public final Path TEST_DATA_PATH = Config.TEMP_DIR.resolve("test_bytes.tmp");
    static public final Path TEST_DATA_OUT = Config.TEMP_DIR.resolve("test_bytes_out.tmp");
    public static File test_file;
    static public InputSubStream stream;
    static public InputSubStreamArray substreams;
    
    public InputSubStreamTest(){}

    @BeforeClass
    public static void setUpClass() throws IOException {
        System.out.println("*****************************************************");
        System.out.println("Writing " + Integer.toString(TEST_DATA_SIZE) + " test bytes to " + TEST_DATA_PATH );
        var every_byte = new byte[256];
        for(var i=0; i<256; ++i){
            every_byte[i] = (byte)i;
        }
        
        test_file = TEST_DATA_PATH.toFile();
        try(var out = new BufferedOutputStream( new FileOutputStream(test_file)) ){
            int i;
            for(i = 0; i < TEST_DATA_SIZE - 256; i += 256){
                out.write(every_byte);
            }
            
            out.write(every_byte, 0, TEST_DATA_SIZE - i );
        }
        stream = new InputSubStream( (ignore)->new RandomAccessFile(TEST_DATA_PATH.toString(), "r"), 0 );
    }
    
    private void verify_bytes(byte[] data, long offs){
        byte b=0
        for(var i=0; i < data.length; ++i){
            assertEquals("byte did not match", (long)b++, (long)data[i] );
        }
    }
    
    private void test_substream( ThrowingSupplier<InputSubStream, IOException> supplier) throws Exception{
        var stream = supplier.get();
        var offs = stream.file().getFilePointer();
        var buf = stream.readAllBytes();
        verify_bytes(buf, offs);
        var out = new RandomAccessFile(TEST_DATA_OUT.toFile(), "rw");
        out.seek(offs);
        out.write(buf);
        assertEquals(stream.read(), -1);
        buf = stream.readNBytes(1);
        assertEquals(buf.length, 0);
        stream.rewind();
        buf = stream.readNBytes(1000);
        verify_bytes(buf, 0);
        buf = stream.readNBytes(1024);
        verify_bytes(buf, 1000);
    }
    
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
    @Test
    public void basic_InputSubStream_test() throws FileNotFoundException, IOException, InterruptedException, Exception{
        var threads = new ArrayList<ThrowingActionThread<IOException>>(THREADS);
        var substream = new InputSubStream( (ignored) -> new RandomAccessFile(TEST_DATA_PATH.toFile(), "r") );
        var substreams = substream.divide(THREADS); //8 is a common number of processors
        
        int offs=0, i=0;
        for(var s : substreams){
            threads.add( new ThrowingActionThread<IOException>( ()->test_substream(s), IOException.class ) );
            threads.get(i++).start();
        }
        
        for(var t : threads){
            t.join();
            t.check();
        }
        
        var comp = IO.compare_files(TEST_DATA_PATH.toFile(), TEST_DATA_OUT.toFile()); 
        assertEquals("SubStream test files do not match", comp, 0);
    }
}
