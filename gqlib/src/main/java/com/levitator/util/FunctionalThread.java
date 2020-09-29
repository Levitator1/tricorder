package com.levitator.util;

/**
 * Thread that is a lambda
 */
public class FunctionalThread extends Thread {

    private final Action m_action;
    
    public FunctionalThread(Action action){
        m_action = action;
    }
    
    @Override
    public void run() {
        m_action.apply();
    }
}
