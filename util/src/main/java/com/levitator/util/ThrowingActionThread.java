package com.levitator.util;

import com.levitator.util.function.ThrowingAction;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


/*
*
* If an anticipated exception is thrown, then it gets caught and stashed away instead of
* forwarded to the termination handler. It can be retrieved via exception(). Other exceptions are treated as uncaught.
*
*/
public class ThrowingActionThread<E extends Exception> extends Thread{

    private Class<E> m_exclass;
    private ThrowingAction<E> m_action;
    
    //The exception we expect
    private E m_exception;
    
    //Two bases of unchecked exception
    private RuntimeException m_runtime_exception;
    private Error m_error;

    public ThrowingActionThread(ThrowingAction<E> action, Class<E> exception_class){
        m_action = action;
        m_exclass = exception_class;
    }
    
    Throwable exception(){
        if(m_exception != null)
            return m_exception;
        else if (m_runtime_exception != null)
            return m_runtime_exception;
        else
            return m_error;
    }
    
    public void check() throws E{
        if(m_exception != null)
            throw m_exception;
        else if (m_runtime_exception != null)
            throw m_runtime_exception;
        else if(m_error != null)
            throw m_error;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void run(){  
        try{
            m_action.apply();
            
            //We try to catch all exceptions and store them for retrieval
        }
        catch(Error ex){
            m_error = ex;
            throw ex;
        }
        catch(RuntimeException ex){
            m_runtime_exception = ex;
            throw ex;
        }
        catch(Exception ex){
            if( m_exclass.isInstance(ex) ){
                m_exception = (E)ex; //normal exit with exception stored
            }
            else{
                m_runtime_exception = new RuntimeException("unexpected exception", ex);
                throw m_runtime_exception; //abnormal thread termination
            }
        }
    }
}
