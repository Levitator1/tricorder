package com.levitator.util;

/*
*
* Stopwatch in ms units
*/
public class Timer implements Cloneable{
    
    //Default shallow clone is fine for one long
    private long m_start;
    
    static public long time_ms(){ return System.currentTimeMillis(); }
    public Timer(){ reset(); }
    public long elapsed(){ return time_ms() - m_start; }
    public long reset(){ return m_start = time_ms(); }
    
    public Timer clone(){
        try{
            return (Timer)super.clone();
        }
        catch(CloneNotSupportedException ex){
            throw new RuntimeException("Unexpected exception: " + ex.getMessage());
        }
    }
}
