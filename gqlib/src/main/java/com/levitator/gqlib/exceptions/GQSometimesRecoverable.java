/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.levitator.gqlib.exceptions;

/**
 *
 * Sometimes we can keep going after this happens
 * 
 */
public class GQSometimesRecoverable extends GQException{

    private final boolean m_recoverable;
    
    public GQSometimesRecoverable(boolean recoverable, String msg, Throwable cause) {
        super(msg, cause);
        m_recoverable = recoverable;
    }

    public GQSometimesRecoverable(boolean recoverable, String msg) {
        super(msg);
        m_recoverable = recoverable;
    }

    @Override
    public boolean recoverable() {
        return m_recoverable;
    }
}
