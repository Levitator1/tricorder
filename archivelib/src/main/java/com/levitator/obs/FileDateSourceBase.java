package com.levitator.obs;

import java.nio.file.Path;

/**
 *
 * A file-based data source whose range can be obtained from its filename.
 * The filename will be of the form Kmin_to_Kmax_suffix
 *
 */
public abstract class FileDateSourceBase<K, T> implements IDataSource<K, T>{
    private K m_start, m_finish; 
    protected abstract K parse_key(String str) throws Exception;
    
    protected void set_range(Path p) throws Exception{
        var tok = p.getFileName().toString().split("_");
        m_start = parse_key(tok[0]);
        m_finish = parse_key(tok[1]);
    }

    @Override
    public K range_begin() {
        return m_start;
    }

    @Override
    public K range_end() {
        return m_finish;
    }
}
