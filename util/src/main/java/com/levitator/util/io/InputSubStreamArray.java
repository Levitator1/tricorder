package com.levitator.util.io;

import com.levitator.util.function.ThrowingSupplier;
import java.io.IOException;
import java.util.ArrayList;

public class InputSubStreamArray extends ArrayList< ThrowingSupplier<InputSubStream, IOException> > {
    public InputSubStreamArray(int size){
        super(size);
    }
}
