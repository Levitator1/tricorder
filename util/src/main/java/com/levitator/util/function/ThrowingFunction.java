package com.levitator.util.function;

public interface ThrowingFunction<T, R, E extends Exception> {
    public R apply(T v) throws E;
}
