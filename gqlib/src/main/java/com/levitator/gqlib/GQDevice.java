package com.levitator.gqlib;

import com.levitator.gqlib.config.Config;
import com.levitator.gqlib.exceptions.GQException;
import com.levitator.gqlib.exceptions.GQIOException;
import com.levitator.gqlib.exceptions.GQInterruptedException;
import com.levitator.gqlib.exceptions.GQProtocolException;
import com.levitator.gqlib.exceptions.GQUnexpectedException;
import com.levitator.gqlib.exceptions.GQFramingError;
import com.levitator.gqlib.exceptions.internal.NvmBlankException;
import com.levitator.gqlib.structures.RecordFactory;
import com.levitator.gqlib.structures.TimeRecord;
import com.levitator.gqlib.structures.MeasurementRecord;
import com.levitator.gqlib.structures.NvmRecordBase;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import com.levitator.util.Util;
import com.levitator.gqlib.exceptions.internal.RecordTruncatedException;
import com.levitator.util.Action;
import com.levitator.util.Ref;
import com.levitator.util.Timer;
import com.levitator.util.levArrays;
import static java.lang.Integer.min;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import util.guards.Guard;

public class GQDevice implements AutoCloseable{
    
    //Native process that handles serial I/O
    private Process m_process;
    private InputStream m_in;
    private OutputStream m_out;
    private InputStream m_error;
    
    private String m_device_version;
    private Timer m_last_cmd_time = new Timer();
    private boolean m_echo_status = true; //assume it's true so that when we turn it off it comes up dirty and commits to the device
    private Boolean m_save_log_status = null; //It's kind of a big production to fetch this, so be lazy
    
    //
    //We attempt to trap all unhandled exceptions as GQUnexpectedException so that users can tell that
    //the exception came from here, so they can discard the stream, or at least not expect it to be valid
    //
    
    public GQDevice(Path dev) throws GQIOException, GQInterruptedException, GQProtocolException, GQUnexpectedException{
        
        try{
            try{
                var cmd = new ArrayList<String>(Config.sercat_cmd);
                cmd.add(dev.toString());
                var pb = new ProcessBuilder(cmd);
                m_process = pb.start();
                m_in = m_process.getInputStream();
                m_out = m_process.getOutputStream();
                m_error = m_process.getErrorStream();
                finish_command();
            }
            catch(IOException ex){
                throw new GQIOException("Failed opening devce or IO process", ex);
            }

            //We cache this because we need it for use as a response delimiter
            m_device_version = fetch_version();

            //In case someone left it on
            echo_off();
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception initializing new GQDevice", ex);
        }
    }
    
    @Override
    public void close() throws GQUnexpectedException{
        try{
            m_process.destroy();
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception closing GQDevice", ex);
        }
    }
    
    protected String read_line() throws GQIOException, GQInterruptedException{
        var sb = new StringBuilder(32);
        int b;

        while(true){
            b = read();
            if(b == '\n')
                break;
            else{
                sb.append( (char)b );
            }
        }
        
        //If the line eneded in crlf, then lop off the cr, too
        int last = sb.length() - 1;
        if(last >= 0 && sb.charAt(last) == '\r' )
            sb.deleteCharAt(last);
        
        return sb.toString();
    }
    
    //Ok. Some echo messages are not line-terminated, so we look for those
    //and if we match one, we interpret it as a line even though it's bugged.
    //TODO: This may make cross-device compatibility something of a mess, unless
    //they are all bugged the same way. Will probably need to add to the initialization
    //sequence a probe for device quirks (bugs)
    protected String read_buggy_line(String... unterminated) throws GQIOException, GQInterruptedException{
        var sb = new StringBuilder(32);
        int b;
        
        Arrays.sort(unterminated);
        while(true){
            b = read();
            if(b == '\n')
                break;
            else{
                sb.append( (char)b );
                if( Arrays.binarySearch(unterminated, sb.toString()) > -1 )
                    return sb.toString();
            }
        }
       
        //If the line eneded in crlf, then lop off the cr, too
        int last = sb.length() - 1;
        if(last >= 0 && sb.charAt(last) == '\r' )
            sb.deleteCharAt(last);
        
        return sb.toString();
    }
    
    
    //General rule for this is that anything that implies the completion of
    //a command, with reply completed, is implied to call finish_command() when done.
    //If you do any weird low-level IO stuff to complete a command, then call this yourself.
    //Also, you can't call this too much. Just not enough.
    protected void finish_command(){
        m_last_cmd_time.reset();
    }
    
    protected void send_string(String str) throws GQIOException{
        try{
            m_out.write( Util.String2bytes(str) );
        }
        catch(IOException ex){
            throw new GQIOException("Failed transmitting string", ex);
        }
    }
    
    protected void read(byte[] data, int i, int len) throws GQIOException, GQInterruptedException{
        try{
           var end = i + len;
           while(i < end){
               var result = m_in.read(data, i, len);
               if(result < 0)
                   throw new EOFException("Device or IO process disconnected while reading byte array");
               i += result;
               len -= result;
           } 
        }
        catch(InterruptedIOException ex){
            throw new GQInterruptedException(false, "Interrupted reading device byte array", ex);
        }
        catch(IOException ex){
            throw new GQIOException("Error reading byte string from device", ex);
        }
    }
    
    protected void read(byte[] data) throws GQIOException, GQInterruptedException{
        read(data, 0, data.length);
    }
    
    protected String read_string(int len) throws GQIOException, GQInterruptedException{
        var data = new byte[len];
        read(data);
        return Util.bytes2String(data);
    }
    
    protected void write_command(String cmd) throws GQIOException{
        send_string("<");
        send_string(cmd);
        send_string(">>");
        
        try{
            m_out.flush();
        }
        catch(IOException ex){
            throw new GQIOException("Failed flushing device out-stream", ex);
        }
    }
    
    //How long to wait before we can transmit another command
    public long throttle_time() throws GQUnexpectedException {
        try{
            var tmp = Config.command_interval - m_last_cmd_time.elapsed();
            return Math.max(tmp, 0);
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception obtaining current throttle wait", ex);
        }
    }
    
    //Wait until the current throttle_time() elapses
    public long throttle_wait() throws GQInterruptedException, GQUnexpectedException{
        long t;
        try {
            TimeUnit.MILLISECONDS.sleep(t = throttle_time());
        }
        catch (InterruptedException ex) {
            throw new GQInterruptedException(true, "Interrupted waiting for command-rate throttling to elapse", ex);
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception waiting for throttle delay to expire", ex);
        }
        return t;
    }
    
    // Observes the maximum command rate before transmitting a command
    // Checks to make sure that there is no spurious data inbound before proceeding
    protected void send_command(String cmd) throws GQIOException, GQProtocolException, GQInterruptedException{
        
        throttle_wait();
        
        try{
            if(m_in.available() > 0)
                throw new GQProtocolException("Stray reply data found before issuing command '" + cmd + "': " + read_string(m_in.available()));
            write_command(cmd);
        }
        catch(IOException ex){
            throw new GQIOException("Error checking IO in-queue size", ex);
        }
        finally{
            finish_command();  //Assumes no reply. Call again later otherwise.
        }
    }
    
    //Perplexingly, some command responses are variable length AND lack a terminator, so we follow these
    //commands with a NOP command which does produce a terminator, and this allows us
    //to locate the end of the original command's response
    protected String artificially_delimit() throws GQInterruptedException, GQIOException, GQProtocolException{
        String reply;
        try{
            throttle_wait();
            write_command("GETVER");
            reply = read_line();
        }
        catch(GQInterruptedException ex){
            //Force this non-recoverable because it leaves us with half a command out
            throw new GQInterruptedException(false, "Interrupted setting up artificial protcol response delimiter", ex);
        }
        finally{
            finish_command();
        }
        
        var verlen = get_version().length();
        var replylen = reply.length();
        var delpos = replylen - verlen;
        
        if( !reply.substring(delpos).equals(get_version()) )
            throw new GQProtocolException("Expected the device model/revision ID, but got something else: " + reply.substring(delpos));
        
        return reply.substring(0, delpos);
    }
    
    protected int read() throws GQIOException, GQInterruptedException{
        try {
            var result = m_in.read();
            if(result == -1)
                throw new EOFException("Disconnected from device while reading byte");
            return result;
        }
        catch (InterruptedIOException ex){
            throw new GQInterruptedException(false, "Interrupted reading byte", ex);
        }
        catch (IOException ex) {
            throw new GQIOException("Error reading byte from device", ex);
        }
    }
    
    //Do a command whose expected response is a single byte
    protected void do_byte_result_command(String cmd, byte b) throws GQProtocolException, GQIOException,
            GQInterruptedException, GQUnexpectedException{
        
        try{
            send_command(cmd);
            int ch;
            ch = read();
            byte ch2 = (byte)ch;
            if(ch2 != b)
                throw new GQProtocolException("Device command '" + cmd + "' failed. Reply invalid.");
        }
        catch(GQException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception running device command for single-byte result", ex);
        }
        finally{
            finish_command();   
        }
    }
    
    //Run a command that returns a line of text
    protected String do_line_command(String cmd) throws GQInterruptedException, GQIOException, GQProtocolException{
        String result;
        try{
            send_command(cmd);
            result = read_line();
        }
        finally{
            finish_command();
        }       
        return result;
    }
    
    //
    // Device commands
    //    
    protected String fetch_version() throws GQInterruptedException, GQIOException, GQProtocolException{
        return do_line_command("GETVER");
    }
    
    public String get_version(){ return m_device_version; }
    
    private String get_mode_impl() throws GQInterruptedException, GQProtocolException, GQIOException{
        send_command("GETMODE");
        return artificially_delimit();
    }
    
    public String get_mode() throws GQInterruptedException, GQProtocolException, GQIOException{
        try{
            return get_mode_impl();
        }
        catch(GQIOException | GQInterruptedException | GQProtocolException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception retrieving device mode string", ex);
        }
    }
    
    //Ok. Echo is really annoying because the top-most screen is temporary
    //and it times out by itself, so you will get two echo messages for it
    //and there is no way to discern whether to expect one or two, other than
    //to quickly move out of the offending screen before it times out of its own accord
    private void echo_on() throws GQProtocolException, GQInterruptedException, GQIOException, GQUnexpectedException{
        if(!m_echo_status){
            do_byte_result_command("ECHOON", (byte)0xaa);
            m_echo_status = true;
        }
    }
    
    public void echo_off() throws GQProtocolException, GQIOException, GQInterruptedException, GQUnexpectedException{
        if(m_echo_status){
            do_byte_result_command("ECHOOFF", (byte)0xaa);
            m_echo_status = false;
        }
    }
    
    public boolean echo(){
        return m_echo_status;
    }
    
    public void echo(boolean v) throws GQProtocolException, GQInterruptedException, GQIOException, GQUnexpectedException{
        if(v)
            echo_on();
        else
            echo_off();
    }
    
    protected String do_echo_result_command(String cmd) throws GQIOException, GQProtocolException, GQInterruptedException{
        String result;
        try{
            send_command(cmd);
            result = read_buggy_line(Config.unterminated_echo_replies);
        }
        finally{
            finish_command();
        }
        return result;
    }
    
    //Returns the resulting echo string, or null if echo is disabled
    //Don't tarry in timeout screens or we will get an extra echo which we interpret as noise
    public String key_impl(int no) throws GQIOException, GQProtocolException, GQInterruptedException{
        
        //Beware of idling in extreme top or bottom-level screens because they time out
        //and produce spontaneous echo messages which trash our state
        
        //This returns nothing, per RFC
        //Unless echo is on, then it seems to return <echo message><crlf>
        var cmd = "KEY" + no;
        if(m_echo_status){
            return do_echo_result_command(cmd);
        }
        else{
            send_command(cmd);
            return null;
        }
    }
    
     public String key(int no) throws GQIOException, GQProtocolException, GQInterruptedException{
         try{
             return key_impl(no);
         }
         catch(GQException ex){
             throw ex;
         }
         catch(Exception ex){
             throw new GQUnexpectedException("Unexpected exception sending device keystroke", ex);
         }
     }
    
    public String key_with_echo(int no) throws GQIOException, GQProtocolException, GQInterruptedException, GQUnexpectedException{
        try( var guard = push_echo(true) ){
            return key(no);
        }
        catch(GQIOException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception in key_with_echo()", ex);
        }
    }
    
    private Guard<GQException> push_echo(boolean v) throws GQProtocolException, GQIOException, GQInterruptedException, GQUnexpectedException{
        boolean previous = echo();
        echo(v);
        return ()->echo(previous);
    }
    
    public Guard<GQException> push_save_onoff(boolean v) throws GQProtocolException, GQIOException, GQInterruptedException, GQUnexpectedException{
        var throwaway = new Ref<Boolean>();
        return push_save_onoff(v, throwaway);
    }
    
    public Guard<GQException> push_save_onoff(boolean v, Ref<Boolean> prev) throws GQProtocolException, GQIOException,
            GQInterruptedException, GQUnexpectedException{
        
        boolean previous = set_save_data_onoff(v);
        if(prev != null) prev.value = previous;
        return ()->set_save_data_onoff(previous);
    }

    //This must remain sorted. "const" would be nice
    static private final String[] main_modes = levArrays.sort( new String[]{"AllInOneMode RF", "Vertical RF", "TableMode RF", 
        "EMF Graphs", "RF Browser", "2.400 - 2.504 GHz", "240.00 - 1040.00 MHz", "Exit"} );
    
    //Find out whether we are on a main function screen
    public boolean is_main_mode(String mode){
        return Arrays.binarySearch(main_modes, mode) >=0;
    }
    
    //Keep hitting key 0 until we reach a main display
    //That way, we are at a known reference point from which to reach other menus
    //Accepts a list of echo strings which are acceptable early stopping points instead of the home screen
    //Returns the echo string result from the last keypress performed
    public String navigate_home(String... target_echos) throws GQProtocolException, GQIOException,
            GQInterruptedException{
        
        var es = "";
        Arrays.sort(target_echos);
        
        try(var eguard = push_echo(true)){
            while( !is_main_mode(get_mode()) ){
                es = key_with_echo(0);
                if(target_echos.length > 0){
                    if(Arrays.binarySearch(target_echos, es) >= 0)
                        return es;
                }
            }
        }
        catch(GQProtocolException | GQIOException | GQInterruptedException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected error navigating to home/main display", ex);
        }
        
        return es;
    }
    
    //Verify the mode or throw
    protected void demand_mode(String mode) throws GQInterruptedException, GQProtocolException, GQIOException{
        var actual = get_mode();
        if(!actual.equals(mode))
            throw new GQProtocolException("Unexpected mode. Wanted: " + mode + ". Got: " + actual);
    }
    
    //Seek echo
    //Keep pushing key 1 (down-arrow) until we receive a desired echo response.
    //If we wrap around to the original response, then we know we have traversed all menu items, so we throw.
    //If "current" is provided, then we check "target" against that first before proceeding to cycle through the menu items.
    private void seek_echo_impl(String target, String current) throws GQIOException, GQProtocolException, GQInterruptedException, GQUnexpectedException{
        String first;

        if(current != null)
            first = current;
        else
            first = key_with_echo(1);

        String echo;
        if(first.equals(target)) return;
        while(!(echo = key_with_echo(1)).equals(target)){
            //Oops. Wrapped around
            if(echo.equals(first))
                throw new GQProtocolException("Failed locating menu item: " + target);
        }
    }
    
    public void seek_echo(String target, String current) throws GQProtocolException, GQIOException, GQInterruptedException, GQUnexpectedException{
        
        try( var guard = push_echo(true) ){
            seek_echo_impl(target, current);
        }
        catch(GQProtocolException | GQIOException | GQInterruptedException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception seeking menu item", ex);
        }
    }
    
    private String navigate_main_menu_impl() throws GQProtocolException, GQIOException, GQInterruptedException{
        navigate_home();
        var current = key_with_echo(3);
        demand_mode("Main Menu");
        return current;
    }
    
    //Returns the echo value of the default/initial menu item
    public String navigate_main_menu() throws GQProtocolException, GQIOException, GQInterruptedException, GQUnexpectedException{
        try( var guard = push_echo(true) ){
            return navigate_main_menu_impl();
        }
        catch(GQProtocolException | GQIOException | GQInterruptedException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception navigating to main menu", ex);
        }
    }
    
    private String navigate_save_data_impl() throws GQProtocolException, GQIOException, GQInterruptedException{
            //Look for the special case where we're already sitting just outside the menu
            //likely after having visited it before
            var echo = navigate_home("->Save Data");
            if(echo.equals("->Save Data"))
                return key_with_echo(3);
            
            //No luck, take the long route
            echo = navigate_main_menu();
            seek_echo("->Save Data", echo);
            return key_with_echo(3);
    }
    
    //Navigate to screen: Main Menu->Save Data
    //And return echo
    public String navigate_save_data() throws GQProtocolException, GQIOException, GQInterruptedException{
        try(var guard = push_echo(true)){
            return navigate_save_data_impl();
        }
        catch(GQProtocolException | GQIOException | GQInterruptedException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception navigating to Save Data menu", ex);
        }
    }
    
    private String navigate_save_data_onoff_impl() throws GQProtocolException, GQIOException, GQInterruptedException{
        
        try(var guard = push_echo(true)){
            var echo = navigate_save_data();
            seek_echo("->On/Off", echo);
            return key_with_echo(3);
        }
        catch(GQProtocolException | GQIOException | GQInterruptedException ex){
            throw ex;
        }
        catch(GQException ex){
            throw new GQUnexpectedException("Unexpected exception navigating to save-data menu option", ex);
        }
    }
    
    public String navigate_save_data_onoff() throws GQProtocolException, GQIOException, GQInterruptedException{
        try(var guard = push_echo(true)){
            return navigate_save_data_onoff_impl();
        }
        catch( GQProtocolException | GQIOException | GQInterruptedException ex){
            throw ex;
        }
        catch (Exception ex) {
            throw new GQUnexpectedException("Unexpected exception navigating to Save Data->On/Off menu", ex);
        }
    }
    
    public boolean get_save_data_onoff() throws GQProtocolException, GQIOException, GQInterruptedException{
        try(var guard = push_echo(true)){
            return onoff2bool( navigate_save_data_onoff() );
        }
        catch(GQProtocolException | GQIOException | GQInterruptedException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception retrieving log-save mode", ex);
        }
    }

    public boolean onoff2bool(String v) throws IllegalArgumentException{
        switch (v) {
            case "ON":
                return true;
            case "OFF":
                return false;
            default:
                throw new IllegalArgumentException("Unrecognized On/Off string: " + v);
        }
    }
    
    public String bool2onoff(boolean v){ return v ? "ON" : "OFF"; }
    
    //Set data logging on or off and return the previous setting
    private boolean set_save_data_onoff_impl(boolean value) throws GQProtocolException, GQIOException, GQInterruptedException{
        try(var guard = push_echo(true)){
            if(m_save_log_status == null || m_save_log_status != value){
                var old_status = m_save_log_status = onoff2bool(navigate_save_data_onoff());

                //Remember to back out of the on/off screen because it will time out
                //but we prefer well-defined behavior and not race conditions
                try(Guard<GQException> back_out = ()->key(0) ){
                    if(m_save_log_status != value){
                        var newb = onoff2bool(key_with_echo(1));
                        if(newb != value)
                            throw new GQProtocolException("Failed to set data logging status to: " + bool2onoff(value));
                        m_save_log_status = value;
                    }
                    return old_status;
                }
            }
            else
                return m_save_log_status;
        }
        catch(GQProtocolException | GQIOException | GQInterruptedException ex){
            throw ex;
        }
        catch(GQException ex){
            throw new GQUnexpectedException("Unexpected exception setting device log mode", ex);
        }
    }
    
    public boolean set_save_data_onoff(boolean value) throws GQProtocolException, GQIOException, GQInterruptedException, GQUnexpectedException{
        try{
            return set_save_data_onoff_impl(value);
        }
        catch(GQProtocolException | GQIOException | GQInterruptedException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected error setting device logging on/off", ex);
        }
    }
    
    // Debug
    // Poll for data at a fixed interval and stop when no data is available
    /*
    public byte[] poll_read() throws InterruptedException, IOException{
        int avail;
        var buf = new ByteArrayOutputStream();
        byte[] inb;
                
        do{
            TimeUnit.MILLISECONDS.sleep(Config.io_poll_interval);
            avail = m_in.available();
            inb = m_in.readNBytes(avail);
            buf.write(inb);
        }while(avail > 0);
        
        return buf.toByteArray();
    }
    */
    
    protected byte[] readNBytes( int count ) throws GQIOException, GQInterruptedException{
        var buf = new byte[count];
        read(buf);
        return buf;
    }
    
    private byte[] get_log_impl(int address, int count) throws GQIOException, GQProtocolException, GQInterruptedException{
        String cmd;
        byte[] result;
        
        try{
            var argbytes = ByteBuffer.allocate(5);
            Util.put_uint24(address, argbytes);
            Util.put_uint16(count, argbytes);
            cmd = "SPIR" + Util.bytes2String(argbytes.array());
            send_command(cmd);
            //var result = poll_read();
            result = readNBytes(count);
        }
        finally{
            finish_command();
        }
        return result;
    }
    
    //Fetch a block of raw binary data directly from device NVM
    public byte[] get_log(int address, int count) throws GQIOException, GQProtocolException, GQInterruptedException{
        try{
            return get_log_impl(address, count);
        }
        catch(GQIOException | GQProtocolException | GQInterruptedException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception fetching raw log data", ex);
        }
    }
    
    //Scan a block of binary data aligned to the start of a record and read as many records as possible
    //The number of bytes processed are returned. The start of NVM is always a block start, right?
    private void parse_chunk(ByteBuffer data, ArrayList<NvmRecordBase> records) throws 
            GQFramingError, GQProtocolException, RecordTruncatedException, NvmBlankException{
        
        while(data.remaining() > 0){
            records.add( RecordFactory.read(data) );
        }
        
        //We only support two record types, but they should all ensure that the buffer position
        //is restored in the event of a truncation error, as those need to be recoverable
        
        //Return normally if we reach the end of the chunk, though we are more likely to
        //either hit a partial record or unpolulated NVM space first
    }
    
    //Read and parse the entire device log or until a framing error occurs, which we interpret as EOF
    //Since the record size is variable, it's unclear what's suppposed to happen at the seam of a full ringbuffer where the head meets the tail
    //Seems like that could lead to a desync in the middle, but have to wait and see.
    private LogDataSet get_log_impl(Action progress_f, Ref<Integer> byte_count, SessionTimes all_sessions, SessionTimes guessed_times)
            throws GQIOException, GQProtocolException, GQInterruptedException, GQFramingError{
        
        var result = new ArrayList<NvmRecordBase>();
        ByteBuffer buf;
        int addr = 0;
        int to_read;
        int min_read = NvmRecordBase.id_size; //minimum bytes needed to make progress
        int mem_remain;
        
        do{
            mem_remain = Config.log_memory_size - addr;
            to_read = min(mem_remain, Config.log_chunk_size);
            
            //Ran out of memory to read
            if(to_read < min_read)
                break;
           
            buf = ByteBuffer.wrap(get_log(addr, to_read));
            
            try{
                parse_chunk(buf, result);
                
                //Ended on a record boundry, so minimum size to read is 2, so that we can
                //determine the type of next record
                min_read = NvmRecordBase.id_size;
                if(progress_f != null) progress_f.apply();
            }
            catch(NvmBlankException ex){
                //We got blanked memory for framing bytes
                break;
            }
            catch(RecordTruncatedException ex){
                //There was a partial record, but we remember the minimum size for the next chunk to make progress
                min_read = ex.size; 
            }
            finally{
                //Resume reading device memory at the end of the last successful operation
                addr += buf.position();
            }
        }while(true);
        
        if(byte_count != null)
            byte_count.value = addr;
        
        return process_record_times(result, all_sessions, guessed_times);
    }
    
    public LogDataSet get_log(Action progress_f, Ref<Integer> byte_count, SessionTimes all_sessions, SessionTimes guesses) throws
            GQIOException, GQProtocolException, GQInterruptedException, GQFramingError{
        
        try{
            return get_log_impl(progress_f, byte_count, all_sessions, guesses);
        }
        catch(GQException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception fetching or parsing device data log", ex);
        }
    }
    
    //Match measurment records with corresponding time records, and then sort them all
    //Populates a set of all sessions found.
    //Populates a set of sessions for which we made assumptions regarding time interpolation.
    private LogDataSet process_record_times(ArrayList<NvmRecordBase> data, SessionTimes all_sessions, SessionTimes guess_sessions){
        LocalDateTime time = null;
        int leading_no_times = 0;
        int seq = 0;
        var result = new LogDataSet();
        var result2 = new LogDataSet();
        
        //apply each time record to the subsequent series of measurement records
        for(var r : data){
            if(r.getClass().equals(TimeRecord.class)){
                //Record the time and discard the time record
                //Duplicate time records are ignored
                if( time==null || !time.equals(r.time) ){
                    time = r.time;
                    seq = 0;
                }
            }
            else{
                var m = (MeasurementRecord)r;
                m.time = time;
                m.seq_no = seq++;
                if(time == null)
                    ++leading_no_times;
                else
                    if(!result.add(m))
                        throw new RuntimeException("Unexpected error. Somehow wound up with a duplicate log record or sequence number.");
            }
        }
        
        //Empty set if missing all time data
        if(time == null){
            result.clear();
            return result;
        }

        //If we have a leading string of records with no time associated,
        //see if the time at the end of the array is consistent with ring-buffer
        //roll-over
        //if( time.compareTo( result.get(leading_no_times).time ) <= 0 ){
        if(time.compareTo( result.first().time ) <= 0){
            //The times wrap around, so apply the buffer-end time and sequence to the beginning
            for(var r : data.subList(0, leading_no_times) ){
                var r2 = (MeasurementRecord)r;
                r2.time = time;
                r2.seq_no = seq++;
                if(!result.add(r2)) throw new RuntimeException("Unexpected duplicate record!");
            }
        }
        //Leading records lacking times must be discarded if the surrounding times don't wrap
        if(result.size() <= 0)
            return result;
        
        var sessions = TimeInterpolate.divide_into_sessions(result);
        
        boolean guessed;
        LocalDateTimeRange dr;
        for(var s : sessions){
            guessed = TimeInterpolate.interpolate_session_times(s, result2);
            
            LocalDateTime t0=s.get(0).time, t1=s.get(s.size() -1).time;
            dr = new LocalDateTimeRange(t0, t1);
            
            if(all_sessions != null)
                all_sessions.add( dr );
                
            if(guessed && guess_sessions != null){
                guess_sessions.add( dr );
            }
        }
        
        return result2;
    }
    
    /*
    //0x0F, etc.
    private String hex_byte(int v){
        if(v > 0xff)
            throw new IllegalArgumentException("Character value out of range: " + Integer.toString(v));
                    
        return String.format("0x%02X", v);
    }
    */
    
    private void do_date_command_impl(String cmd, int v) throws GQProtocolException, GQIOException, GQInterruptedException{
        byte[] b = {(byte)v};
        var cmd2 = cmd + Util.bytes2String(b);
        do_byte_result_command(cmd2, (byte)0xaa);
    }
    
    private void do_date_command(String cmd, int v) throws GQProtocolException, GQIOException, GQInterruptedException{
        try{
            do_date_command_impl(cmd, v);
        }
        catch(GQException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception processing date-setting command: " + cmd, ex);
        }
    }
    
    public void set_year(int y) throws GQProtocolException, GQIOException, GQInterruptedException{
        do_date_command("SETDATEYY", y - 2000);
    }
    
    public void set_month(int m) throws GQProtocolException, GQIOException, GQInterruptedException{
        do_date_command("SETDATEMM", m);
    }
    
    public void set_day(int d) throws GQProtocolException, GQIOException, GQInterruptedException{
        do_date_command("SETDATEDD", d);
    }
    
    public void set_hour(int h) throws GQProtocolException, GQIOException, GQInterruptedException{
        do_date_command("SETTIMEHH", h);
    }
    
    public void set_minute(int m) throws GQProtocolException, GQIOException, GQInterruptedException{
        do_date_command("SETTIMEMM", m);
    }
    
    public void set_second(int s) throws GQProtocolException, GQIOException, GQInterruptedException{
        do_date_command("SETTIMESS", s);
    }
    
    public void set_time(LocalDateTime time) throws GQProtocolException, GQIOException, GQInterruptedException{
        //Reset the seconds so that they don't roll over and alter the other
        //time fields while we are setting them
        set_second(0);
        
        set_year(time.getYear());
        set_month(time.getMonthValue());
        set_day(time.getDayOfMonth());
        set_hour(time.getHour());
        set_minute(time.getMinute());
        set_second(time.getSecond());
    }
    
    //Takes a desired time setting and adjusts it with the offsets and delays incurred and anticipated
    //in sending it to the device
    public LocalDateTime adjusted_out_time(LocalDateTime time, Timer initial){
        var dt = 
                Config.command_interval * 6 +  //We have to wait 6 full throttle times to complete the operation
                throttle_time() + //plus initial throttle delay
                initial.elapsed(); //plus offset relative to the moment we determined "time"
        
        return Util.round_seconds(time.plusNanos(dt * 1000000));
    }
    
    public void update_config() throws GQProtocolException, GQIOException, GQInterruptedException{
        do_byte_result_command( "CFGUPDATE", (byte)0xaa );
    }
    
    public void clear_log() throws GQProtocolException, GQIOException, GQInterruptedException{
        do_byte_result_command("SPIE", (byte)0xaa);
    }
    
    private LocalDateTime get_time_impl() throws GQIOException, GQProtocolException, GQInterruptedException{
        
        byte[] reply;
        try{
            send_command("GETDATETIME");
            reply = readNBytes(7);
        }
        finally{
            finish_command();
        }
        
        if(reply[6] != (byte)0xaa) 
            throw new GQProtocolException("Date terminator did not match");
        
        return LocalDateTime.of(2000 + reply[0], reply[1], reply[2], reply[3], reply[4], reply[5]);
    }
    
    public LocalDateTime get_time() throws GQIOException, GQProtocolException, GQInterruptedException{
        try{
            return get_time_impl();
        }
        catch(GQException ex){
            throw ex;
        }
        catch(Exception ex){
            throw new GQUnexpectedException("Unexpected exception retrieving device RTC time", ex);
        }
    }
}
