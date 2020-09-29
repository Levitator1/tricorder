package com.levitator.util.streams;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
* 
* Operations on Java stream sets
* 
*/
public class Streams {
    
    //Returns a lamda that always returns a copy of the same stream
    static public <T> Supplier<Stream<T>> stream_copy_supplier(Stream<T> source){
        List<T> data = source.collect(Collectors.toList());
        return ()->data.stream();
    }
   
    static public <T, U> BiFunction<T, U, String> joinStringsFunctor(){
        return (lhs, rhs) -> lhs.toString() + rhs;
    }
    
    //
    // Cartesian join, like in SQL
    //
   
    //Join lhs to each of rhs
    static public <T, U, R> Stream<R> join( BiFunction<T, U, R> f, T lhs, Stream<? extends U> rhs){
        return rhs.map( (r)->f.apply(lhs, r) );
    }
    
    //Join each of lhs to each of rhs
    static public <T, U, R> Stream<R> join(BiFunction<T, U, R> f, Stream<? extends T> lhs, Stream<? extends U> rhs){
        var copier = stream_copy_supplier(rhs);
        return lhs.flatMap( l -> Streams.join(f, l, copier.get()) );
    }
    
    //Join an arbitrary number of sets, 3 or more
    //The explanation for SafeVarargs is a bunch of incomprehensible gibberish to me
    //We don't write to the array, so hopefully that eliminates problems
    //TODO: Understand what is going on here
    @SafeVarargs
    static public <T, R> Stream<R> join(BiFunction<T, T, R> initial_f, BiFunction<R, T, R> f,
            Stream<? extends T> stream1, Stream<? extends T> stream2, Stream<? extends T> ...streams){
        
        var result = Streams.<T,T,R>join(initial_f, stream1, stream2);
        for(var str : streams){
            result = Streams.<R,T,R>join(f, result, str);
        }
        return result;
    }
    
    @SafeVarargs
    static public Stream<String> joinStrings(Stream<String> stream1, Stream<String> stream2, Stream<String> ... streams ){
        return join( 
                joinStringsFunctor(), 
                joinStringsFunctor(),
                stream1, stream2, streams);
    }
}
