package util.guards;

import java.util.function.Consumer;
import com.levitator.util.Action;
import com.levitator.util.ThrowingAction;

/*
* RAII-style idiom
* Do one of two things upon closing
*
*/

public class CondGuard<E extends Exception, F extends Exception> implements AutoCloseable {

    ThrowingAction<E> f_t;
    ThrowingAction<F> f_f;
    public boolean status = false;
    
    public CondGuard(ThrowingAction<E> true_f, ThrowingAction<F> false_f, boolean initial_status){
        f_t = true_f;
        f_f = false_f;
        status = initial_status;
    }
    
     public CondGuard(ThrowingAction<E> true_f, ThrowingAction<F> false_f){
        this(true_f, false_f, false);
    }
    
    @Override
    public void close() throws E, F  {
        if(status)
            f_t.apply();
        else
            f_f.apply();
    }    
}
