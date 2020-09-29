package com.levitator.gqlib;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 *
 * These have the potential to overlap, so maybe we should omit a natural ordering
 * However, sorting them by end-time seems reasonable.
 * 
 */
public class LocalDateTimeRange{
    private final LocalDateTime m_start, m_end;
    
    public LocalDateTimeRange(LocalDateTime start, LocalDateTime end){
        m_start = start;
        m_end = end;
    }
    
    public LocalDateTime getStart(){ return m_start; }
    public LocalDateTime getEnd(){ return m_end; }
    public Duration asDuration(){ return Duration.between(m_start, m_end); }
}
