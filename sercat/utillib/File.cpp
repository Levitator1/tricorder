#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/select.h>
#include <string>
#include <vector>
#include <iostream> //For debugging
#include "include/exception.hpp"
#include "include/File.hpp"

using namespace jab;
using namespace jab::exception;
using namespace std::string_literals;
using namespace jab::file;

File::File(){}
File::File(fd_t fd):m_state{fd}{}

static void check_ofl(int in, int &out, int fl, int bits){
    out |= in & fl ? bits : 0;
}

File::File(const std::filesystem::path &path, int fl){
    
    int ofl = 0;
    if( fl & r && fl & w){
        ofl |= O_RDWR;
    }
    else{
        check_ofl(fl, ofl, r, O_RDONLY);
        check_ofl(fl, ofl, w, O_WRONLY);
    }
    check_ofl(fl, ofl, ndelay, O_NDELAY);
    check_ofl(fl, ofl, noctty, O_NOCTTY);
    check_ofl(fl, ofl, nonblock, O_NONBLOCK);
    
    m_state.m_fd = ::open(path.native().c_str(), ofl);
    if(m_state.m_fd == null_fd)
        throw posix_exception("Error opening file: "s + path.c_str());

}

//TODO: Log a warning if a file doesn't close, since we shouldn't throw here
File::~File(){
    try{
        cleanup();
    }
    catch(const posix_exception &){
    }
}

void File::cleanup(){ close(); }

void File::close(){
    if(*this){
        if(::close(m_state.m_fd) == -1)
            throw posix_exception("Failed closing file");
        else
            m_state.m_fd = null_fd;
    }
}

File::operator bool() const{ return m_state.m_fd != null_fd; }
File::operator fd_t() const{ return m_state.m_fd; }

File::File(File &&rhs){
    m_state = rhs.m_state;
    rhs.m_state.m_fd = null_fd;
}

File &File::operator=(File &&rhs){
    if(this == &rhs) return *this;
    m_state = rhs.m_state;
    rhs.m_state.m_fd = null_fd;
    return *this;
}

::ssize_t File::read(char *data, len_t len){
    ::ssize_t result = ::read(m_state.m_fd, data, len);
    if(result == -1) 
        throw posix_exception("Error reading file");
    
    if(m_state.m_debug)
        std::cerr << "R(" << m_state.m_fd << "): " << util::hex_format(std::string(data, result)) << std::endl;
    
    return result;
}

//Insist on reading a certain length
void File::read_exactly(char *data, len_t len){
    ::ssize_t ct;
    
    while(len){
        ct = read(data, len);
        len -= ct;
        data += ct;
    }
}

::ssize_t File::write(const char *data, len_t len){
    ::ssize_t result = ::write(m_state.m_fd, data, len);
    if(result == -1) throw posix_exception("Error writing file");
    
    if(m_state.m_debug)
        std::cerr << "W(" << m_state.m_fd << "): " << util::hex_format(std::string(data, result)) << std::endl;
    
    return result;
}

void File::write_exactly(const char *data, len_t len){
    
    ::ssize_t ct=0;
    while(len){
        try{
            ct = write(data, len);
        }
        catch(const posix_exception &ex){
            //Avoid eating CPU time
            if(ex.code().value() == EWOULDBLOCK){
                file::select( file::FDSet(), file::FDSet(*this), file::FDSet(*this) );
            }
            else
                throw;
        }
        len -= ct;
        data += ct;
    }
}

std::string File::read_until(char delimiter){
    std::vector<char> buf;
    char ch;
    
    while(true){
        read_exactly(&ch, 1);
        if(ch == delimiter) break;
        buf.push_back(ch);
    }
    
    return { buf.data(), buf.size() };
}

StdFile StdFile::stdin(0);
StdFile StdFile::stdout(1);
StdFile StdFile::stderr(2);

::size_t File::available() const{
    int result;
    const_ioctl(FIONREAD, &result);
    return result;
}

void File::flush(){
    if( ::fsync(m_state.m_fd) == -1 )
        throw posix_exception("Failed flushing file");
}

bool File::debug() const{ return m_state.m_debug; }
void File::debug(bool v){ m_state.m_debug = v; }

//NOPs for standard streams
//You can still close() them explicitly. They just don't close themselves because
//we let the runtime manage their lifetime by default
void StdFile::cleanup(){}

void StdFile::flush(){
    //Invalid argument
    //File::flush();
    
    //How do you flush stdio?
}

FDSet::FDSet(){
    zero();
}

void FDSet::set_impl(File::fd_t fd){
    
    
    //Null fd is NOP
    if(fd == File())
        return;
    
    FD_SET(fd, &m_fds);
    if(fd > max_fd())
        m_fd_max = fd;
}

void FDSet::clear_impl(File::fd_t fd){
    
    //NOP for null fd
    if(fd == File())
        return;
    
    FD_CLR(fd, &m_fds);
}

void FDSet::find_max() const{
    File::fd_t result = File::null_fd;
    for(File::fd_t fd = 0; fd < FD_SETSIZE; ++fd){
        if(isset_impl(fd) && fd > result)
            result = fd;
    }
    m_fd_max = result;
    m_max_valid = true;
}

void FDSet::zero(){
    FD_ZERO(&m_fds);
    m_fd_max = File::null_fd;
    m_max_valid = true;
}

int FDSet::isset_impl(File::fd_t fd) const{ return FD_ISSET(fd, &m_fds) ? 1 : 0; }

File::fd_t FDSet::max_fd() const{
    if(!m_max_valid)
        find_max();
    
    return m_fd_max;
}

const ::fd_set *FDSet::fds() const{ return &m_fds; }

::fd_set *FDSet::invalidate_max(){
    m_max_valid = false;
    return &m_fds; 
}

int jab::file::select(FDSet &rfds, FDSet &wfds, FDSet &efds, ::timeval *timeout){
    int nfds = va::max(rfds.max_fd(), wfds.max_fd(), efds.max_fd()) + 1;
    int result = ::select(nfds, rfds.invalidate_max(), wfds.invalidate_max(), efds.invalidate_max(), timeout);
    if(result == -1)
        throw posix_exception("select() call failed waiting for I/O");
    
    return result;
}

/*
int jab::file::select(FDSet &rfds, FDSet &wfds, FDSet &efds, ::timeval &tv){
    return jab::file::select(rfds, wfds, efds, &tv);
}

int jab::file::select(FDSet &rfds, FDSet &wfds, FDSet &efds){
    return jab::file::select(rfds, wfds, efds, NULL);
}
*/

jab::file::timeval::timeval(sec_t sec, usec_t usec): ::timeval{0}{
    tv_sec = sec;
    tv_usec = usec;
}