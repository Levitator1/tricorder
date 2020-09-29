package com.levitator.util.function;

import java.util.function.Consumer;

/*
*
* Accepts nothing, returns nothing
*
*/
@FunctionalInterface
public interface Action{
    public void apply();
    
    //Compose actions
    public default Action andThen(Action after){
        return ()->{ this.apply(); after.apply(); };
    }
}
