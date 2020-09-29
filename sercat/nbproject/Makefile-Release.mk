#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=gfortran
AS=as

# Macros
CND_PLATFORM=GNU-Linux
CND_DLIB_EXT=so
CND_CONF=Release
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/src/GQDevice.o \
	${OBJECTDIR}/src/GQSerial.o \
	${OBJECTDIR}/src/config.o \
	${OBJECTDIR}/src/main.o \
	${OBJECTDIR}/utillib/File.o \
	${OBJECTDIR}/utillib/Serial.o \
	${OBJECTDIR}/utillib/exception.o \
	${OBJECTDIR}/utillib/util.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=-std=c++17 -Wall -Wpedantic
CXXFLAGS=-std=c++17 -Wall -Wpedantic

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=-lstdc++fs

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ../bin/sercat

../bin/sercat: ${OBJECTFILES}
	${MKDIR} -p ../bin
	${LINK.cc} -o ../bin/sercat ${OBJECTFILES} ${LDLIBSOPTIONS} -s

${OBJECTDIR}/src/GQDevice.o: src/GQDevice.cpp nbproject/Makefile-${CND_CONF}.mk
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Werror -s -Iutillib/include -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/GQDevice.o src/GQDevice.cpp

${OBJECTDIR}/src/GQSerial.o: src/GQSerial.cpp nbproject/Makefile-${CND_CONF}.mk
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Werror -s -Iutillib/include -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/GQSerial.o src/GQSerial.cpp

${OBJECTDIR}/src/GQSerial.hpp.gch: src/GQSerial.hpp nbproject/Makefile-${CND_CONF}.mk
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Werror -s -Iutillib/include -MMD -MP -MF "$@.d" -o "$@" src/GQSerial.hpp

${OBJECTDIR}/src/config.o: src/config.cpp nbproject/Makefile-${CND_CONF}.mk
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Werror -s -Iutillib/include -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/config.o src/config.cpp

${OBJECTDIR}/src/main.o: src/main.cpp nbproject/Makefile-${CND_CONF}.mk
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Werror -s -Iutillib/include -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/src/main.o src/main.cpp

${OBJECTDIR}/utillib/File.o: utillib/File.cpp nbproject/Makefile-${CND_CONF}.mk
	${MKDIR} -p ${OBJECTDIR}/utillib
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Werror -s -Iutillib/include -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/utillib/File.o utillib/File.cpp

${OBJECTDIR}/utillib/Serial.o: utillib/Serial.cpp nbproject/Makefile-${CND_CONF}.mk
	${MKDIR} -p ${OBJECTDIR}/utillib
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Werror -s -Iutillib/include -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/utillib/Serial.o utillib/Serial.cpp

${OBJECTDIR}/utillib/exception.o: utillib/exception.cpp nbproject/Makefile-${CND_CONF}.mk
	${MKDIR} -p ${OBJECTDIR}/utillib
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Werror -s -Iutillib/include -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/utillib/exception.o utillib/exception.cpp

${OBJECTDIR}/utillib/util.o: utillib/util.cpp nbproject/Makefile-${CND_CONF}.mk
	${MKDIR} -p ${OBJECTDIR}/utillib
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Werror -s -Iutillib/include -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/utillib/util.o utillib/util.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
