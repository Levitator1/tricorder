#include <cerrno>
#include "include/exception.hpp"

using namespace jab::exception;

posix_exception::posix_exception(const std::string &wha) : std::system_error( errno, std::generic_category(),  wha) {
}

