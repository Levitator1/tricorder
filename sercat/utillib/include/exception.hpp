#pragma once
#include <system_error>

namespace jab{
namespace exception{
    
class posix_exception:public std::system_error{
public:
    posix_exception(const std::string &);
};

}    
}