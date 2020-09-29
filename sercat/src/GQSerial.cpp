#include <unistd.h>
#include <filesystem>
#include "GQSerial.hpp"

using namespace sercat;
using namespace std;
using namespace filesystem;
using namespace jab;
using namespace jab::serial;

GQSerial::GQSerial(){}


GQSerial::GQSerial(const path &p, int flags):Serial(p, flags){

    setbaud(B0);
    
    //8N1
    tios.c_cflag &= ~PARENB;
    tios.c_cflag &= ~CSTOPB;
    tios.c_cflag &= ~CSIZE;
    tios.c_cflag |= CS8;
    
    //No flow control
    tios.c_cflag &= ~CRTSCTS;
    
    //No canonical/tty mode
    tios.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
    
    //No output processing
    tios.c_oflag &= ~OPOST;
    
    //debug(true);
    //Input flags
    tios.c_iflag &= ~(BRKINT | INPCK | PARMRK | ISTRIP | IXON | IXOFF | INLCR |
            IGNCR | ICRNL | IUCLC | IMAXBEL | NL1 | CR1 | CR2 | CR3 | TAB2 | TAB3 | IUTF8);
    tios.c_iflag |= IGNPAR | IXANY | IGNBRK;
 
    //Output flags
    tios.c_oflag  &= ~(OPOST | OLCUC | ONLCR | OCRNL | ONOCR | ONLRET | 
            OFILL | OFDEL | NLDLY | CRDLY | TABDLY | BSDLY | VTDLY | FFDLY | BS1 | VT1 | FF1);
    tios.c_oflag |= NL0 | CR0 | TAB0 | VT0 | FF0;
    
    //Commit to tty device
    write_tios();
    
    //Connect at 115200
    setbaud(B115200);
}
