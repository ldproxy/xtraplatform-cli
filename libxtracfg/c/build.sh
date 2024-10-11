#!/bin/sh

OUT_DIR=build
PLATFORM=$(uname -s |  tr '[:upper:]' '[:lower:]' )
EXT=$([ "$PLATFORM" = "darwin" ] && echo "dylib" || echo "so")

echo "platform: ${PLATFORM}"
echo $(whereis ar)
echo $(clang --version)

mkdir -p ${OUT_DIR}

cd ${OUT_DIR}

echo "lib"

# shared
#clang -shared -fpic -Wall -I./ -L./ -I$JAVA_HOME/include -I$JAVA_HOME/include/${PLATFORM} -lxtracfgjni -o libxtracfg.${EXT} ../wrapper/libxtracfg.c 

# static
clang -c -Wall -I./ -I$JAVA_HOME/include -I$JAVA_HOME/include/${PLATFORM} -o libxtracfg.o ../wrapper/libxtracfg.c
cp libxtracfgjni.a libxtracfg.a
ar -rv libxtracfg.a libxtracfg.o

echo "test"

# shared
#clang -I./ -L./ -lxtracfgjni -lxtracfg -o test ../test/main.c

# static
if [ "$PLATFORM" = "darwin" ]; then
clang -I./ -L./ -Wl,-framework,CoreServices -ldl -lpthread -Wl,-framework,Foundation -o test ../test/main.c ./libxtracfg.a
else
clang -I./ -L./ -ldl -lpthread -o test ../test/main.c ./libxtracfg.a
fi
