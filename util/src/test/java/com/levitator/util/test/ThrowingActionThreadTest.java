package com.levitator.util.test;

import com.levitator.util.ThrowingActionThread;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ThrowingActionThreadTest {
    
    static ThrowingActionThread<IOException> runtime_exception_thread, io_exception_thread, no_exception_thread;
    
    public ThrowingActionThreadTest() {}
    
    @BeforeClass
    public static void setUpClass() throws InterruptedException{
        runtime_exception_thread = new ThrowingActionThread<>( () -> throw_runtime_exception(), IOException.class );
        io_exception_thread = new ThrowingActionThread<>( () -> throw_io_exception(), IOException.class );
        no_exception_thread = new ThrowingActionThread<>( () -> {}, IOException.class );
        runtime_exception_thread.start();
        io_exception_thread.start();
        no_exception_thread.start();
        runtime_exception_thread.join();
        io_exception_thread.join();
        no_exception_thread.join();
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    static private void throw_runtime_exception(){
        throw new RuntimeException("runtime exception");
    }
    
    static private void throw_io_exception() throws IOException{
        throw new IOException("this is an IOException");
    }
    
    @Test
    public void test_no_exception() throws IOException{
       no_exception_thread.check();
    }
    
    @Test(expected = RuntimeException.class)
    public void test_runtime_exception() throws IOException{
        runtime_exception_thread.check();
    }
    
    @Test(expected = IOException.class)
    public void test_io_exception() throws IOException{
        io_exception_thread.check();
    }

}
