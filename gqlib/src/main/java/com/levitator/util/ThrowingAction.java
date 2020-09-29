package com.levitator.util;

@FunctionalInterface
public interface ThrowingAction<E extends Exception>{
    public void apply() throws E;
    
    public default <F extends E> ThrowingAction<E> andThen(ThrowingAction<F> after) throws E, F{
        return () -> { this.apply(); after.apply(); };
    }
    
}
