package com.levitator.util;
import com.levitator.util.function.Action;

//Thread whose run() method calls an Action
//Wanted to do this as a lambda type, but then I don't see how
//you would be able to retrieve the Thread object so that you could join() it and such
public class ActionThread extends Thread{

    private final Action m_action;
    
    public ActionThread(Action action){
        m_action = action;
    }
    
    @Override
    public void run() {
        m_action.apply();
    }
    
}
