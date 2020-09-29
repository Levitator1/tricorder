tricorder
=========


**V 1.0RC1**  
This is a tool for accessing a GQ EMF-390 RF counter from a Linux host over USB.
This is the first release candidate.
  
- Dump the onboard measurement log (Timestamp, EMF, EF, RF) to CSV format suitable for a spreadsheet
- Schedule automatic daily dumps in perpetuity, either at a regular interval, at set times of day, or both
- Clear/erase the onboard log
- Set the device onboard RTC time either to a specific time, or based on the current systsem time
- Enable or disable logging to device memory
  
**sercat**  
sercat connects to the USB/serial device specified on the command-line, initializes it for communication with the EMF-390, and forwards stdio back and forth
so that you can pipeline commands and replies through the process, with the device. It is written in C++ with only standard library dependencies.
  
**gqlib**  
This is a Java library for communicating with GQ devices. It is based on testing with an EMF-390. It uses sercat as its back end.
Porting this library and tricorder to a different operating system is purely an exercise in porting sercat.
  
**building**  
  
`git clone https://github.com/squirrel1776/tricorder`  
`cd tricorder`  
  
`gradle build`  
*or*  
`gradle -PCONFIG=Debug build`  

