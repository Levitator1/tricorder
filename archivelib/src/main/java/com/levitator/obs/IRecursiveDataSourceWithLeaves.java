package com.levitator.obs;

/**
 *
 * A data source that contains other data sources like itself
 * and has leaf nodes of some other data source.
 * This is with the goal, in view, of having a bisectable directory tree structure
 * with dataset files at the bottom of the hierarchy
 * 
 * K is the key, or X axis, or time value for the data source, the identifying value used to specify a range of samples or records
 * T is Y or the data record, or ultimate value contained in the archive
 * 
 */
public interface IRecursiveDataSourceWithLeaves<K, T,
        DS extends IDataSource<? extends K, ? extends  T>,
        RS extends IRecursiveDataSource<? extends K, ? extends T, ? extends IDataSource<? extends K, ? extends T>>>
        extends IDataSource<K, T>{
}
