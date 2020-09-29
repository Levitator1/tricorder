package util.guards;

import com.levitator.util.ThrowingAction;

//Have to support "throws Exception" because that's how AutoCloseable is defined
public interface Guard<E extends Exception> extends ThrowingAction<E>, AutoCloseable{
    @Override
    public default void close() throws E{
        apply();
    }
    
    //Compose
    public default <F extends E> Guard<E> andThen(ThrowingAction<F> after){
        return ()->{ this.close(); after.apply(); };
    }
}

