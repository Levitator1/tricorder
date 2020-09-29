package com.levitator.gqlib.exceptions;

/**
 *
 * We have two exception base classes and this allows a uniform interface
 * to query them for recoverable status
 *
 */
public interface IGQException{
    public boolean recoverable();
}
