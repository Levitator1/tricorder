#include <filesystem>
#include "config.hpp"

using namespace sercat;


//
// Setting defaults
//

bool sercat::Config::debug_mode = false;
bool sercat::Config::no_interrupt = false;
std::filesystem::path sercat::Config::device_path = "/dev/ttyUSB0";
