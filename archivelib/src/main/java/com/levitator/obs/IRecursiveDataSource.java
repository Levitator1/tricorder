package com.levitator.obs;

/*
*
* A date source that contains other data sources like itself
* i.e., a directory tree
*
*/
public interface IRecursiveDataSource<K, T, DS extends IDataSource<? extends K, ? extends T> > extends IDataSource<K, T>{
    
}
