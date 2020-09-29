#pragma once
#include <algorithm>
#include <vector>

namespace jab{
namespace io{
    
template<typename T>
class ringbuf{
    std::vector<T> m_data;
    T *m_head, *m_tail;
    ::size_t m_size;

    const T *const_bufend() const{ return &m_data.back() + 1; }
    T *bufend(){ return const_cast<T *>(const_bufend());  }
    const T *bufend() const{ return const_bufend(); }
    
    void bump_ptr(T *&ptr, size_t ct){
        ptr += ct;
        if(ptr >= bufend())
            ptr = (&m_data.front()) + (ptr - bufend());
    }
    
public:
    ringbuf(::size_t cap){
        m_size = 0;
        m_data.resize(cap);
        m_head = m_tail = &m_data.front();
    }
 
    T *chunk_in_begin(){ return m_head; }
    const T *chunk_in_begin() const{ return m_head; }
    
    const T *const_chunk_in_end() const{
        return m_tail > m_head ? m_tail : bufend();
    }
    
    const T *chunk_in_end() const{ return const_chunk_in_end(); }
    T *chunk_in_end(){ return const_cast<T *>( const_chunk_in_end() );  }
    
    
    ::size_t chunk_in_size() const{ return chunk_in_end() - chunk_in_begin(); }
    
    const T *chunk_out_begin() const{ return m_tail; }
    
    const T *chunk_out_end() const{
        return m_tail < m_head ? m_head : bufend();
    }
    
    ::size_t chunk_out_size() const{ return chunk_out_end() - chunk_out_begin(); }
    
    void push(size_t c){
        bump_ptr(m_head, c);
        m_size += c;
    }
    
    void pop(size_t c){
        
        m_size -= c;
        
        //If the buffer is empty, reset the pointers for max chunk size
        if(!m_size)
            m_head = m_tail = &m_data.front();
        else
            bump_ptr(m_tail, c);
    }
    
    size_t size() const{ return m_size; }
};

}
}
