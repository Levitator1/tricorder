package com.levitator.util;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import util.guards.CondGuard;

public class Util {
    
    //bytes are, somewhat insanely, signed. We convert them to positive short
    static public short byte2positiveshort(byte b){
        final short sign_bit = (short)Byte.MAX_VALUE + 1;
        var tmp = (short) (b & Byte.MAX_VALUE);
        return (short) (b >= 0 ? tmp : tmp | sign_bit);
    }
    
    //Positive short byte to (crazy) signed byte
    static public byte positiveshort2byte(short v){
        if(v > (short)Byte.MAX_VALUE - Byte.MIN_VALUE )
            throw new IllegalArgumentException("byte value out of range: " + Short.toString(v));
        
        if(v <= Byte.MAX_VALUE)
            return (byte)v;
        else{
            return (byte)((short)Byte.MIN_VALUE + (v - Byte.MAX_VALUE) - 1 );
        }
    }
    
    //Try to convert bytes directly to codepoints and return them in a string
    static public String bytes2String(byte[] data){
        var sb = new StringBuilder(data.length);
        
        char c;
        for(var b : data){
            c = (char)byte2positiveshort(b);
            sb.append(c);
        }
        return sb.toString();
    }
    
    //Convert each codepoint directly to a byte value
    //throw if overflow
    static public byte[] String2bytes(String data){
       
        var chars = data.toCharArray();
        var result = new byte[data.length()];
        int i = 0;
        for(var c : chars){
            if(c > 0xff)
                throw new IllegalArgumentException("Character does not fit in byte: " + String.format("%x", c));
            
            result[i++] = (byte)c;
        }
        return result;
    }    
    
    //
    //Binary data
    //
   
   //Truncate a positive int to a certain number of bytes and write it
   static public void put_subuint_bytes(int value, int bytec, ByteBuffer buf){
       
       int max = (1 << (bytec * 8)) - 1;
       
       if(value < 0 || value > max)
           throw new IllegalArgumentException("uint24 value is out of range");
       
       if(bytec > 3)
           throw new IllegalArgumentException("Requested more than 3 bytes of an int");
       
       var tb = ByteBuffer.allocate(4);
       tb.putInt(value);
       
       if(buf.order() == ByteOrder.BIG_ENDIAN){
           buf.put(tb.array(), 4 - bytec, bytec);
       }
       else{
           buf.put(tb.array(), 0, bytec);
       }
   }
   
   static public void put_uint16(int value, ByteBuffer buf){
       put_subuint_bytes(value, 2, buf);
   }
   
   static public void put_uint24(int value, ByteBuffer buf){
       put_subuint_bytes(value, 3, buf);
   }
    
    //Binary OR without sign extension
    static public int bin_or(int lhs, short rhs){
        var result = lhs | (rhs & Short.MAX_VALUE);
        if(rhs < 0){
            result |= 1 << 15;
        }
        return result;
    }
    
    //Round time to the nearest second
    static public LocalDateTime round_seconds(LocalDateTime t){
        final long secns = 1000000000;
        final var halfns = secns / 2;
        final var nanos = t.getNano();
        var nsremain = secns - nanos;
        if(nsremain <= halfns)
            return t.plusNanos(nsremain);
        else
            return t.minusNanos(nanos);
    }
    
    //eat whitespace until the next non-whitespace character and return that or EOF/-1
    static public int eat_ws(Reader data) throws IOException{
        int ich;
        char ch;
        while((ich = data.read()) != -1 ){
            ch = (char)ich;
            if(!Character.isSpaceChar(ch)) return ich;
        }
        return -1;
    }
    
    static public void stack_trace(Throwable ex, PrintStream out, int indent){
        try{
            var elements = ex.getStackTrace();
            
            out.println("Stack trace: " + ex.getClass().getName());
            for(var el : elements){
                for(int i=0;i<indent;++i){
                    out.print('\t');
                }
                out.println(el.toString());
            }
        }
        catch(Exception ex2){
            //Fail silently
        }
    }
    
    static public void stack_traces(Throwable ex, PrintStream out){
        try{
            stack_trace(ex, out, 0);
            var level=1;
            while((ex = ex.getCause()) != null){
                stack_trace(ex, out, level++);
            }
            
        }
        catch(Exception ex2){
            //Fail silently
        }
    }
    
    static public CondGuard<RuntimeException, RuntimeException> push_buffer_pos(Buffer buf){
        int pos = buf.position();
        return new CondGuard<>( ()->{}, ()->buf.position(pos));
    }
    
    //Round a date/time to the nearest day
    //Useful to disambiguate edge cases, like if you schedule someting for midnight
    //But it actually occurs an instant earlier
    static public LocalDateTime round_to_day(LocalDateTime t){
        var tt = t.truncatedTo(DAYS);
        if(tt.until(t, HOURS) >= 12)
            return tt.plusDays(1);
        else
            return tt;
    }
    
    static public <T> T find_first(Collection<T> col, Predicate<T> pred){
       for( var obj : col ){
           if(pred.test(obj))
               return obj;
       }
       return null;
    }
}
