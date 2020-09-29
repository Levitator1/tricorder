#include <sys/select.h>
#include <unistd.h>
#include <signal.h>
#include <exception>
#include <iostream>
#include <string>
#include <algorithm>
#include "config.hpp"
#include "exception.hpp"
#include "util.hpp"
#include "GQDevice.hpp"
#include "ringbuf.hpp"
#include "meta.hpp"

using namespace std;
using namespace jab;
using namespace jab::util;
using namespace jab::exception;
using namespace jab::file;
using namespace jab::io;
using namespace sercat;
using namespace std::string_literals;

static void print_ok(){ cerr << "OK" << endl; }
static void print_fail(){ cerr << "FAIL" << endl; }

struct ok_fail:public util::cond_guard<decltype(&print_ok), decltype(&print_fail)>{
    ok_fail():cond_guard(&print_ok, &print_fail){}
};

static void usage(){
    cerr << "Usage: sercat [-d] [--noint] <path>" << endl;
    cerr <<     "\tWhere <path> is a serial device" << endl;
    cerr <<     "\t-d is to print I/O trace to stderr" << endl;
    cerr <<     "\t--noint means to ignore interrupt signals. We only die voluntarily if a stream closes." << endl;
    cerr << endl;
}

struct eof_exception:public std::runtime_error{
    eof_exception():std::runtime_error("EOF"){}
};

static ::ssize_t do_read( File &file, ringbuf<char> &buf ){
    
    auto avail = file.available();
    
    if(!avail){
        file.close();
        return 0;
    }
    
    avail = std::min(avail, buf.chunk_in_size());
    file.read_exactly( buf.chunk_in_begin(), avail );
    buf.push(avail);
    return avail;
}

static ::ssize_t do_write( ringbuf<char> &buf, File &file ){
    
    auto ct = file.write( buf.chunk_out_begin(), buf.chunk_out_size() );
    if(!ct){
        file.close();
        return 0;
    }
    
    buf.pop(ct);
    
    //If source buffer is empty, then that may or may not be the end of a message
    //So, flush just in case
    if(!buf.size())
        file.flush();
    
    return ct;
}

static void run(const std::filesystem::path &dev_path){
    
    GQDevice dev;
    cerr << "Opening device: " << dev_path << "...";
    {
        ok_fail okfail;
        dev = {dev_path};
        okfail.status = true;
    }
    
    //Configure I/O tracing
    bool trace = sercat::Config::debug_mode;
    dev.in.debug(trace);
    dev.out.debug(trace);
    StdFile::stdin.debug(trace);
    StdFile::stdout.debug(trace);
    
    {
        cerr << "Fetching hardware version: ";
        util::cond_guard guard(
            [](){},
            [](){ cerr << "FAILED" << endl; }
        );
        
        dev.out.write(as_bin("<GETVER>>"s));
        dev.out.flush();
        std::string version = dev.in.read_until('\n');
        version = version.substr(0, version.length() - 1);
        cerr << version << endl;
        ::usleep(Config::command_delay); //GQ does not like its commands too close together
        guard.status = true;
    }
    
    cerr << "Initialization successful." << endl;
            
    //We promise that all output after this, on stderr, is actual error messages (unless io debugging is enabled)
    cerr << "READY" << endl;
    
    //Enter main loop and start forwarding data back and forth
    ringbuf<char> stdinbuf(Config::ringbuffer_stdin_size);
    ringbuf<char> serialinbuf(Config::ringbuffer_devin_size);
    
    while( true ){    
        file::FDSet rfds, wfds, efds(StdFile::stdin, StdFile::stdout, dev.in, dev.out);
        
        //
        //Turn on write-waiting for populated buffers
        //
        if(stdinbuf.size())
            wfds.set(dev.out);
        
        if(serialinbuf.size())
            wfds.set(StdFile::stdout);

        //
        //Read
        //
        if(stdinbuf.chunk_in_size())
            rfds.set(StdFile::stdin);
        
        if(serialinbuf.chunk_in_size())
            rfds.set(dev.in);
        
        if(! va::logical_and(StdFile::stdin, StdFile::stdout, dev.in, dev.out) ){
            //If any stream has closed, apply a 1 second inactivity timeout before exiting
            auto count = file::select(rfds, wfds, efds, file::timeval(1, 0));
            if( !count )
                break;
        }
        else{
            file::select(rfds, wfds, efds);
        }
        
        //Handle I/O
        if( rfds.isset(StdFile::stdin) )
            do_read(StdFile::stdin, stdinbuf);
        
        if( rfds.isset(dev.in) )
            do_read(dev.in, serialinbuf);
        
        if( wfds.isset(StdFile::stdout) )
            do_write(serialinbuf, StdFile::stdout);
        
        if( wfds.isset(dev.out) )
            do_write(stdinbuf, dev.out);
    }
}

//Not really an exception, technically
class help_exception:public std::exception{};

void process_switch(int &argc, char** argv, int &argn, const std::string &arg){
    
    if(arg == "-d")
        Config::debug_mode = true;
    else if(arg == "--noint")
        Config::no_interrupt = true;
    else if(arg == "-h" || arg == "--help")
        throw help_exception();
    else
        throw std::runtime_error("Unreconized switch: "s + arg);
}

int process_switches(int &argc, char **argv){
    std::string arg;
    int i;
    for(i=1; i < argc; ++i){
        arg = { argv[i] };
        if(arg[0] == '-')
            process_switch(argc, argv, i, arg);
        else
            break;
    }
    return i;
}

int main(int argc, char** argv) {
    
    cerr << endl; 
    cerr << "I dub thee 'sercat' V1.0" << endl;
    cerr << "GQ device serial stream process" << endl << endl;
    
    int argi = process_switches(argc, argv);
    
    int argleft = argc - argi; 
    
    if(argleft > 1){
        usage();
        cerr << "Wrong number of arguments" << endl;
        return -1;
    }
    
    if(argleft == 1){
        sercat::Config::device_path = std::string(argv[argi]);
    }
    
    if(Config::no_interrupt){
        auto result = ::signal(SIGINT, SIG_IGN);
        if(result == SIG_ERR)
            cerr << "Warning: failed disabling SIGINT";
    }
    
    try{
        run(sercat::Config::device_path);
        return 0;
    }
    catch(const help_exception &){
        usage();
        return 0;
    }
    catch(const eof_exception &){
        //Clean exit
        return 0;
    }
    catch(const std::exception &ex){
        cerr << "Error: " << ex.what() << endl;
        return -1;
    }
}

