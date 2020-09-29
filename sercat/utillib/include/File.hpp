#pragma once
#include <sys/ioctl.h>
#include <type_traits>
#include <string>
#include <filesystem>
#include "exception.hpp"
#include "util.hpp"
#include "meta.hpp"
#include "varargs.hpp"

namespace jab{
namespace file{
 
enum flags{
    r = 1,
    w = r << 1,
    rw = r | w,
    noctty = w << 1,
    ndelay = noctty << 1,
    nonblock = ndelay << 1
};

struct File_state{
    using fd_t = int;
    fd_t m_fd = -1;
    bool m_debug = false;
};

class File{
public:
    using fd_t = File_state::fd_t;
    using len_t = ::size_t;

private:
    File_state m_state;
    
protected:
    //Use the correct one depending on whether it can alter visible state
    template<typename... Args>
    int const_ioctl(unsigned long request, Args... args) const{
        int result = ::ioctl(*this, request, std::forward<Args>(args)... );
        if(result == -1) throw jab::exception::posix_exception("ioctl() failed");
        return result;
    }
    
    template<typename... Args>
    int ioctl(unsigned long request, Args... args){
        return const_ioctl(request, std::forward<Args>(args)...);
    }
    
    //What we do upon destruction
    //When implementing from a derived class remember that all derived classes
    //have been torn down when this get called, so "this" should be treated as File *
    virtual void cleanup();
    
public:
    static constexpr fd_t null_fd = -1;
    File();
    File(fd_t fd);
    File(const std::filesystem::path &path, int fl);
    ~File();
    File(File &&);
    
    //Could be implemented with dup(), but I don't feel like it yet
    File &operator=(const File &) = delete;
    
    //Fundamental I/O operations
    virtual void close();
    virtual ::ssize_t read(char *data, len_t len);
    virtual ::ssize_t write(const char *data, len_t len);
    virtual size_t available() const;
    virtual void flush();
    
    operator bool() const;
    operator fd_t() const;
    File &operator=(File &&);
    void read_exactly(char *data, len_t len);
    void write_exactly(const char *data, len_t len);
    std::string read_until(char c);
    void debug(bool v);
    bool debug() const;
    
    template<typename T>
    void write( const jab::util::as_bin<T> &obj ){
        using type = typename jab::util::as_bin<T>::type;
        using traits = jab::util::binary_traits<type>;
        write_exactly( traits::data(obj), traits::length(obj) );
    }
};

//stdin, stdout, stderr
class StdFile:public File{
public:
    static StdFile stdin, stdout, stderr;

    using File::File;
    
    //flush is NOP on standard streams?
    void flush() override;
    
    //Let the runtime or someone else close these
    void cleanup() override;
};


class FDSet{
  ::fd_set m_fds;
  mutable File::fd_t m_fd_max;
  mutable bool m_max_valid;
  
  //Find out if any argument is equal to m_fd_max
  template<typename... Args>
  bool has_max(Args... arg){
      return logical_or_varargs( arg == m_fd_max... );
  }
  
  void set_impl(File::fd_t );
  void clear_impl(File::fd_t);
  int isset_impl(File::fd_t) const;
  void find_max() const;

  template<typename T>
  using converts_to_fd = typename std::enable_if< std::is_convertible<T, File::fd_t>::value, T >::type;
  
protected:
    friend int select(FDSet &, FDSet &, FDSet&, ::timeval *);
    
    //max fds is no longer known while a non-const pointer is out for use
    ::fd_set *invalidate_max();
 
public:
    FDSet();
    
    template<typename... Args, typename = std::void_t<converts_to_fd<Args>...> >
    FDSet(Args &...  args):FDSet(){
        set(args...);
    }
    
    template<typename... Args, typename = std::void_t<converts_to_fd<Args>...>>
    void set(Args &... args){
        jab::meta::null_function((set_impl(args), 0)... );
    }
    
    template<typename... Args, typename = std::void_t<converts_to_fd<Args>...>>
    void clear(Args &... args){
        
        jab::meta::null_function((clear_impl(args), 0)...);
        
        //If we cleared the max fd, then find the new max fd. Something like 1k comparisons.
        if(has_max(args...))
            m_max_valid = false; //lazy
    }
    
    //Count of fds present
    template<typename... Args, typename = std::void_t<converts_to_fd<Args>...>>
    int isset(Args &... args){
        return jab::va::sum( isset_impl(args) ... );
    }
    
    void zero();
    File::fd_t max_fd() const;
    
    //fd_set *fds();
    const fd_set *fds() const;
};


//Maybe unnecessary? What if timeval isn't always just two fields?
struct timeval:public ::timeval{
    using sec_t = decltype(::timeval::tv_sec);
    using usec_t = decltype(::timeval::tv_usec);
    
    timeval(sec_t sec, usec_t usec);
};

int select(FDSet &rfds, FDSet &wfds, FDSet &efds, ::timeval *timeout);

template<class Rfds, class Wfds, class Efds, class TV, typename = 
    std::void_t<
        meta::const_optional< typename std::remove_reference<Rfds>::type, FDSet>,
        meta::const_optional< typename std::remove_reference<Wfds>::type, FDSet>,
        meta::const_optional< typename std::remove_reference<Efds>::type, FDSet>,
        meta::permit_if_converts_to<TV, ::timeval>
>>
int select(Rfds &&rfds, Wfds &&wfds, Efds &&efds, TV &&timeout){
    
    util::deconst rfdsd(std::forward<Rfds>(rfds)), wfdsd(std::forward<Wfds>(wfds)), efdsd(std::forward<Wfds>(efds));
    util::deconst tvd(std::forward<TV>(timeout));
    
    return file::select(rfdsd.get(), wfdsd.get(), efdsd.get(), &tvd.get());
}

template<class Rfds, class Wfds, class Efds, typename = 
    std::void_t<
        meta::const_optional< typename std::remove_reference<Rfds>::type, FDSet>,
        meta::const_optional< typename std::remove_reference<Wfds>::type, FDSet>,
        meta::const_optional< typename std::remove_reference<Efds>::type, FDSet>
>>
int select(Rfds &&rfds, Wfds &&wfds, Efds &&efds){
    util::deconst rfdsd(std::forward<Rfds>(rfds)), wfdsd(std::forward<Wfds>(wfds)), efdsd(std::forward<Wfds>(efds));
    return file::select(rfdsd.get(), wfdsd.get(), efdsd.get(), NULL);
}


}
}
