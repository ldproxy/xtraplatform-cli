#!/bin/sh

DIR=$(dirname "$(readlink -f "$0")")

ls -l ./build
ldd ./build/test

./build/test "{\"command\": \"info\", \"source\": \"$DIR\", \"debug\": \"true\", \"verbose\": \"true\"}"
