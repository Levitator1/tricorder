#include <unistd.h>
#include <string>
#include <sys/select.h>
#include <vector>
#include "exception.hpp"
#include "File.hpp"
#include "GQDevice.hpp"

using namespace jab;
using namespace jab::exception;
using namespace sercat;
using namespace std::string_literals;

//Uninitialized device
GQDevice::GQDevice(){}

GQDevice::GQDevice(const std::filesystem::path& p):
    in(p, file::r | file::noctty),
    out(p, file::w | file::noctty | file::nonblock){
    
    //Close out any previous partial command or output noise
    //The close-tag sequence seems to reset the parser in the firmware
    out.write(util::as_bin(">>"s));
    out.flush();
    
    //Drain all device output until it is quiet for 1 second    
    ::size_t avail;
    do{
        file::select( file::FDSet(in), file::FDSet(), file::FDSet(), file::timeval(1,0) );
        avail = in.available();
        std::vector<char> buf(avail);
        in.read_exactly(buf.data(), avail);
    }while(avail);
    
    in.purge();
}

