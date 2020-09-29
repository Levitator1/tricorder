package com.levitator.tricorder;

import com.levitator.tricorder.exceptions.TricorderException;

public class ConfigException extends TricorderException{
   public ConfigException(String msg){
       super(msg);
   }
}
