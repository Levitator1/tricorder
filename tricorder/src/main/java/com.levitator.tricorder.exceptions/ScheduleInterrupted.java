/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.levitator.tricorder.exceptions;

/**
 *
 * @author j
 */
public class ScheduleInterrupted extends TricorderException {

    public ScheduleInterrupted() {
        super("Received interrupt waiting for next scheduled task");
    }
}
