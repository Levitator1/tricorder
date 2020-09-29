/*
*
* On closure, print OK or FAIL
*
*/
package com.levitator.gqlib;

import java.io.PrintStream;
import com.levitator.util.guards.CondGuard;

public class OkFailGuard extends CondGuard<RuntimeException, RuntimeException> {
    
    public OkFailGuard(PrintStream out){
        super( 
            ()->{ out.println("OK"); },
            ()->{ out.println("FAIL"); }, 
            false );
    }
    
    public OkFailGuard(){
        this( System.out );
    }
    
    public OkFailGuard(PrintStream out, String header){
        this(out);
        out.print(header);
    }
    
    public OkFailGuard(String header){
       this(System.out, header);
    }
    
}
