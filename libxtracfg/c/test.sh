#!/bin/sh

DIR=$(dirname "$(readlink -f "$0")")

./build/test "{\"command\": \"info\", \"source\": \"$DIR\", \"debug\": \"true\", \"verbose\": \"true\"}"
