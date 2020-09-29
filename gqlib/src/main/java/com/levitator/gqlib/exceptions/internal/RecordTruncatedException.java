package com.levitator.gqlib.exceptions.internal;

//size is the number of elements (bytes, etc) which are required but were not found
//offs is the buffer offset at which to retry with more data
public class RecordTruncatedException extends Exception{
    public final int size;
    
    //size is the minimum size needed for the operation to succeed
    public RecordTruncatedException(int sz){ 
        super("Record incomplete");
        size = sz;
    }
}
