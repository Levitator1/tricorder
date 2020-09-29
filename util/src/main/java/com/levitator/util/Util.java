package com.levitator.util;

import com.levitator.util.function.ThrowingSupplier;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import java.util.Collection;
import java.util.function.Predicate;
import com.levitator.util.guards.CondGuard;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class Util {
    
    static final int byte_sign_bit = ((int)Byte.MAX_VALUE) + 1;
    static final int byte_not_sign_bits = (char)(Byte.MAX_VALUE);
    
    //Signed bytes to unsigned chars
    //Assumes every byte represents a character, and converts it to its signed value
    //Useful for ASCII, or to represent binary data as a Java string
    //Starts at the beginning of "in", starts writing at outi
    static public void byte2positivechar(byte[] in, char[] out, int outi, int ct){
        char ch;
        int b, ini;
        
        for(ini=0; ini < ct; ++ini){
            b = in[ini];
            
            if(b >= 0)
                ch = (char)b;
            else
                //mask out sign-extension and then OR the sign bit back in in its original position
                ch = (char)(b & byte_not_sign_bits  | byte_sign_bit); 
            
            out[outi++] = ch;
        }
    }
    
    
    //Empties all buf into out
    static public void bytes2positivechar(ByteBuffer buf, char[] out){
        byte2positivechar(buf.array(), out, 0, buf.position());
        buf.position(0);
    }
    
    //Empties all of buf into out starting at outi in out
    static public void bytes2positivechar(ByteBuffer buf, char[] out, int outi){
        byte2positivechar(buf.array(), out, outi, buf.position());
        buf.position(0);
    }
    
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
    static public LocalDateTime round_to_seconds(LocalDateTime t){
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
    static public int eat_ws(InputStream data) throws IOException{
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
    
    //Wrap an operation in a stack trace handler. This useful because Junit is stupid.
    public <R, E extends Exception> R stack_trace_wrap( ThrowingSupplier<R, E> f ) throws E{
        
        try{
            return f.get();
        }
        catch(Exception ex){
            stack_traces(ex, System.out);
            throw ex;
        }
        
    }
    
    static public long clamp(long v, long min, long max){
        return Math.max(Math.min(v, max), min);
    }
    
    static public int clamp_to_int(long v){
        return (int)clamp(v, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    /* This concept seems pretty hopeless due to type erasure, but than that poses
    //the question of why ParameterizedType exists.
    
    @SuppressWarnings("unchecked")
    static public <T> Class<? extends T> get_generic_parameter(Object obj, T pass_null_for_inferencing, int i){
        var objtype = (Type)(obj.getClass());
        var objptype = (ParameterizedType)objtype;
        return (Class<? extends T>)objptype.getActualTypeArguments()[i];
    }
    */
    
    static public <T> void fill(Collection<T> col, T v, int count){
        for(var i=0;i<count;++i){
            col.add(v);
        }
    }
    
}
