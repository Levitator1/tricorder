package com.levitator.util.function;

public interface ThrowingSupplier<R, E extends Exception> {
    public R get() throws E;
}
