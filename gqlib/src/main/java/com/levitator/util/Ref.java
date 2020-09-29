package com.levitator.util;

/*
*
* Mutable reference
*
*/
public class Ref<T extends Object>{
    public T value;
    
    public Ref(){
        value = null;
    }
    
    public Ref(T obj){
        value = obj;
    }
}
