package com.levitator.tricorder.schedule;

import com.levitator.gqlib.exceptions.GQException;
import com.levitator.tricorder.config.Config;
import com.levitator.tricorder.Tricorder;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 *
 * A scheduled task for dumping the log memory
 *
 */
public class DumpTask extends Task{
    public DumpTask(Tricorder app, LocalDateTime t){ 
        super(app, t); 
    }

    @Override
    public void run() throws GQException, IOException {
        app.dump_device_log();
    }

    @Override
    public String toString() {
        return "Next dump task is scheduled for: " + Config.format(time);
    }
}
