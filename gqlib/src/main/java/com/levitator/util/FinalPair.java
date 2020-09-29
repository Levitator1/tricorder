package com.levitator.util;

/**
 *
 * Immutable pair
 * 
 * @param <T>
 * @param <U>
 */
public class FinalPair<T,U> {
    private final T first;
    private final U second;
    
    public FinalPair(T t, U u){
        first = t;
        second = u;
    }
    
}
