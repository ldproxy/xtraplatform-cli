package main

/*
//pass on command line with CGO_CFLAGS
//#cgo CFLAGS: -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux"
#cgo CFLAGS: -I include

#include "client.h"
*/
import "C"
import (
	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/client"
	"github.com/interactive-instruments/xtraplatform-cli/xtracfg/cmd"
)

var handleC C.handle_command_func

func main() {
	cmd.Execute()
}

//export cmd_execute
func cmd_execute(handle_command C.handle_command_func) {
	handleC = handle_command

	client.Init(handle)

	cmd.Execute()
}

func handle(command string) string {
	//fmt.Printf("JNI PASS: %s\n", command)

	messages := make(chan string)

	// has to run concurrently since we are being called from java and calling into java at the same time
	go func() {
		r := C.handle_command_2(handleC, C.CString(command))
		messages <- C.GoString(r)
	}()

	msg := <-messages

	return msg
}
