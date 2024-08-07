## build go library

docker run -it --rm -v /Users/az/development/xtraplatform-cli/xtracfg:/src -w /src --platform=linux/arm64 ghcr.io/ldproxy/golang-jdk:1.2 /bin/bash

CGO_CFLAGS="-I$JAVA_HOME/include -I$JAVA_HOME/include/linux" GOOS=linux GOARCH=arm64 go build -ldflags="-s -w '-extldflags=-z noexecstack'" -buildmode c-archive -o dist/libxtracfg.a

## build native binary

docker run -it --rm -v /Users/az/development/xtraplatform-cli/xtracfg:/src -v /Users/az/development/configs-ldproxy:/cfg --platform=linux/arm64 ghcr.io/ldproxy/liberica-nik:22-jdk11 /bin/bash

./gradlew nativeCompile --rerun-tasks

./build/native/nativeCompile/xtracfg info --src /cfg/demo


