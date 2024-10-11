#!/bin/bash
# see https://github.com/oracle/graal/issues/3053#issuecomment-1866735057
# This script intercepts compiler arguments from graalvm native shared images
# and additionally generates a static library. The script was tested with
# GraalVM jdk21+35.1 on macOS and may need to be modified for other versions.
#
# Use with --native-compiler-path=${pathToThisScript}

# Determine the project name and output path based on the output .dylib argument
for arg in "$@"; do
  if [[ "$arg" == *.dylib ]]; then
    OUTPUT_PATH=$(dirname "$arg")
    LIB_NAME=$(basename "${arg%.dylib}")
    break
  fi
done

# Do a simple forward for any calls that are used to compile individual C files
if [[ -z $LIB_NAME ]]; then
    cc $*
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
echo "                  SHARED LIBRARY                     " >> ${LOG_FILE}
echo "=====================================================" >> ${LOG_FILE}
# Modify the arguments if needed
CC_ARGS=$*
echo "cc $CC_ARGS" >> ${LOG_FILE}
cc $CC_ARGS

echo "=====================================================" >> ${LOG_FILE}
echo "                   STATIC LIBRARY                    " >> ${LOG_FILE}
echo "=====================================================" >> ${LOG_FILE}
# To create a single static library on macos we need to call 'ar -r' on all .o files.
# In order to also include all static library dependencies, we can first extract the
# .o files and then include them as well.
echo "======= Source archives"  >> ${LOG_FILE}
OBJECTS=${OUTPUT_PATH}/objects
rm -rf ${OBJECTS}
mkdir ${OBJECTS}
AR_ARGS="-rcs ${OUTPUT_PATH}/${LIB_NAME}.a ${OBJECTS}/*.o"
for arg in $*
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
