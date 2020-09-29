#pragma once
#include <filesystem>
#include "GQSerial.hpp"

namespace sercat{
    
class GQDevice{
public:
    GQSerial in, out;
    GQDevice();
    GQDevice(const std::filesystem::path &p);
};
    
}