package com.levitator.obs;

import java.util.Spliterator;

public interface IDataSource<K,T> extends Spliterator<T>{
    public K range_begin();
    public K range_end();
}
