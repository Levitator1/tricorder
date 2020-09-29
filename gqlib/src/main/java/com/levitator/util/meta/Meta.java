package com.levitator.util.meta;

/**
 *
 * Metaprogramming stuff
 *
 */
public class Meta {
 
    //If you call common_base(U, V), then the compiler infers a common base for T
    @SafeVarargs
    static public <T> T common_base(T... a){
        return (T)null;
    }
    
}
