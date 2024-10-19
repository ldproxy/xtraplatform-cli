#!/bin/bash
# see https://github.com/oracle/graal/issues/3053#issuecomment-1866735057
# This script intercepts compiler arguments from graalvm native shared images and can replace
# select dynamically linked libraries with statically linked ones. The script was tested with
# GraalVM jdk21.0.1+12.1 on Ubuntu and may need to be modified for other versions.
#
# Use with --native-compiler-path=${pathToThisScript}.sh

LOG_PATH="/tmp/logs"
LOG_FILE="${LOG_PATH}/compiler_commands.txt"
mkdir -p $LOG_PATH

# Determine the project name and output path based on the output .so argument
for arg in "$@"; do
  if [[ "$arg" == *.so ]]; then
    OUTPUT_PATH=$(dirname "$arg")
    LIB_NAME=$(basename "${arg%.so}")
    break
  fi
done

# Do a simple forward for any calls that are used to compile individual C files
if [[ -z $LIB_NAME ]]; then
    #gcc $*
    GCC_ARGS=$*
    echo "GCCCCCCC $GCC_ARGS" >> ${LOG_FILE}
    clang $GCC_ARGS
    exit 0
fi

# Create a debug log in $output/logs
LOG_PATH="${OUTPUT_PATH}/logs"
LOG_FILE="${LOG_PATH}/compiler_commands.txt"
mkdir -p $LOG_PATH

WORKINGDIR=${PWD}
echo "Working directory: ${WORKINGDIR}" > ${LOG_FILE}
echo "Output path: ${OUTPUT_PATH}" >> ${LOG_FILE}
echo "Library name: ${LIB_NAME}" >> ${LOG_FILE}

echo "=====================================================" >> ${LOG_FILE}
echo "                    ORIGINAL ARGS                    " >> ${LOG_FILE}
echo "=====================================================" >> ${LOG_FILE}
echo "$*" >> ${LOG_FILE}

echo "=====================================================" >> ${LOG_FILE}
echo "           SHARED LIBRARY WITH STATIC LIBS           " >> ${LOG_FILE}
echo "=====================================================" >> ${LOG_FILE}
# Path to the system library files
ARCH=$(uname -m)
STATIC_LIBS_PATH="/usr/lib/${ARCH}-linux-gnu"

# Do the original call, but replace dynamic libs with static versions
GCC_ARGS=""
for arg in "$@"
do
    if [ "$arg" = "-lz" ]
    then
        GCC_ARGS+=" ${STATIC_LIBS_PATH}/libz.a"
    else
        GCC_ARGS+=" $arg"
    fi
done

echo "clang $GCC_ARGS" >> ${LOG_FILE}
clang $GCC_ARGS

echo "=====================================================" >> ${LOG_FILE}
echo "                   STATIC LIBRARY                    " >> ${LOG_FILE}
echo "=====================================================" >> ${LOG_FILE}
# To create a single static library on linux we need to call 'ar -r' on all .o files.
# In order to also include all static library dependencies, we can first extract the
# .o files and then include them as well.
echo "======= Source archives"  >> ${LOG_FILE}
OBJECTS=${OUTPUT_PATH}/objects
rm -rf ${OBJECTS}
mkdir ${OBJECTS}
AR_ARGS="-rcs ${OUTPUT_PATH}/${LIB_NAME}.a ${OBJECTS}/*.o"
for arg in $GCC_ARGS
do
    if [[ $arg =~ .*\.(a)$ ]]; then
        # extract the objects (.o) of each archive (.a) into
        # separate directories to avoid naming collisions
        echo "$arg"  >> ${LOG_FILE}
        ARCHIVE_DIR=${OBJECTS}/$(basename "${arg%.a}")
        mkdir ${ARCHIVE_DIR}
        cp $arg ${ARCHIVE_DIR}
        cd ${ARCHIVE_DIR}
        ar -x $arg
        cd ${WORKINGDIR}
        AR_ARGS+=" ${ARCHIVE_DIR}/*.o"
    fi
    if [[ $arg =~ .*\.(o)$ ]]; then
        cp $arg ${OBJECTS}
    fi
done

echo "======= Objects"  >> ${LOG_FILE}
find ${OBJECTS} -name "*.o" >> ${LOG_FILE}

echo "======= Archive command"  >> ${LOG_FILE}
echo "ar $AR_ARGS" >> ${LOG_FILE}
ar $AR_ARGS
