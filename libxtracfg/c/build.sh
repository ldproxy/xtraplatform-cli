#!/bin/sh

#TODO: use gradle?
#https://docs.gradle.org/current/userguide/building_cpp_projects.html#sec:cpp_supported_tool_chain
#https://github.com/gradle/native-samples/blob/master/c/application/build.gradle

OUT_DIR=build
PLATFORM=$(uname -s |  tr '[:upper:]' '[:lower:]' )
EXT=$([ "$PLATFORM" = "darwin" ] && echo "dylib" || echo "so")

mkdir -p ${OUT_DIR}

cd ${OUT_DIR}

echo "lib"

# shared
#clang -shared -fpic -Wall -I./ -L./ -I$JAVA_HOME/include -I$JAVA_HOME/include/${PLATFORM} -lxtracfgjni -o libxtracfg.${EXT} ../wrapper/libxtracfg.c 

# static
clang -c -Wall -fPIC -I./ -I$JAVA_HOME/include -I$JAVA_HOME/include/${PLATFORM} -o libxtracfg.o ../wrapper/libxtracfg.c
cp libxtracfgjni.a libxtracfg.a
ar -rv libxtracfg.a libxtracfg.o
if [ "$PLATFORM" = "darwin" ]; then
shasum libxtracfg.a > libxtracfg.sha1sum
else
sha1sum libxtracfg.a > libxtracfg.sha1sum
fi
cp libxtracfg.sha1sum ../../go/xtracfg/

echo "test"

# shared
#clang -I./ -L./ -lxtracfgjni -lxtracfg -o test ../test/main.c

# static
if [ "$PLATFORM" = "darwin" ]; then
clang -I./ -I$JAVA_HOME/include -I$JAVA_HOME/include/${PLATFORM} -L./ -Wl,-framework,CoreServices -ldl -lpthread -Wl,-framework,Foundation -o test ../test/main.c ./libxtracfg.a
else
clang -I./ -I$JAVA_HOME/include -I$JAVA_HOME/include/${PLATFORM} -L./ -ldl -lpthread -o test ../test/main.c ./libxtracfg.a
fi
