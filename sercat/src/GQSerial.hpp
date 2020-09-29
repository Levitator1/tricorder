/*
 * Specialize serial device initialization for the GQ EMF-390 or...
 * 8N1 115200
 * 
 */

#include <filesystem>
#include "Serial.hpp"

namespace sercat{

//A serial stream, in or out
class GQSerial:public jab::serial::Serial{
public:
    GQSerial();
    GQSerial(const std::filesystem::path &, int flags);
};

}
