#pragma once
#include <filesystem>

#ifndef SERCAT_RINGBUFFER_STDIN_SIZE
#   define SERCAT_RINGBUFFER_STDIN_SIZE 1024
#endif

#ifndef SERCAT_RINGBUFFER_DEVIN_SIZE
#   define SERCAT_RINGBUFFER_DEVIN_SIZE 4096
#endif

//It's a seemingly undocumented limitation of the GQ EMF-390
//that it will drop commands issued too close together, or perhaps too close
//to the completion of a previous one, so we set a delay
#ifndef SERCAT_COMMAND_DELAY
#   define SERCAT_COMMAND_DELAY 200000
#endif

namespace sercat{
    
struct Config{
    static constexpr ::size_t ringbuffer_stdin_size = SERCAT_RINGBUFFER_STDIN_SIZE;
    static constexpr ::size_t ringbuffer_devin_size = SERCAT_RINGBUFFER_DEVIN_SIZE;
    static constexpr ::size_t command_delay = SERCAT_COMMAND_DELAY;
    
#ifdef NDEBUG
    static constexpr bool debug_build = false;
#else
    static constexpr bool debug_build = true;
#endif
    
    //
    // Runtime settings
    //
    
    //Whether to print diagnostics
    static bool debug_mode;
    
    //Whether to ignore interrupt signals. SIGINT, in particular
    static bool no_interrupt;
    
    //Device path
    static std::filesystem::path device_path;
};

}
