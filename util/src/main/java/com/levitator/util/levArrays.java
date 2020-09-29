package com.levitator.util;

//This could be called "Arrays", but then it's a pain to disambiguate from java.util

import java.util.Arrays;

public class levArrays {

    //Because it's annyoing to have to specify the length of the array when it's known
    static public <T> T[] copyOf(T[] array){
        return Arrays.copyOf(array, array.length);
    }
    
    //Same as Array.sort(), except it returns the array, making it suitable for use in initializers
    static public <T> T[] sort(T[] a){
        Arrays.sort(a);
        return a;
    }
    
}
