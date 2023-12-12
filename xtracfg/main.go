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
var progress_chan chan string

func main() {
	cmd.Execute()
}

//export cmd_execute
func cmd_execute(handle_command C.handle_command_func) {
	handleC = handle_command
	progress_chan = make(chan string, 16)

	client.Init(handle, progress_chan)

	cmd.Execute()
}

//export progress
func progress(msg *C.char) {
	progress_chan <- C.GoString(msg)
}

func handle(command string) string {
	//fmt.Printf("JNI PASS: %s\n", command)

	messages := make(chan string)

	// has to run concurrently since we are being called from java and calling into java at the same time
	go func() {
		r := C.handle_command_2(handleC, C.CString(command), C.progress_func(C.progress))
		messages <- C.GoString(r)
	}()

	msg := <-messages

	return msg
}
