package xtracfg

/*
#cgo CFLAGS: -I ../../c/include
#cgo LDFLAGS: -L../../c/build -lxtracfg
#cgo windows LDFLAGS: -L../../c/build -lxtracfg -lxtracfgjni_static_ext

#include <stdlib.h>
#include "libxtracfg.h"

void progress(char *msg);
*/
import "C"

import (
	_ "embed"
	"encoding/json"
	"fmt"
	"path/filepath"
	"strconv"
	"strings"
	"unsafe"
)

// needed to trigger a rebuild when libxtracfg.a changes
//
//go:embed libxtracfg.sha1sum
var res string

// Store is
type Store struct {
	source   *string
	driver   *string
	verbose  *bool
	debug    *bool
	Progress *chan string
}

var progress_chan chan string

//export progress
func progress(msg *C.char) {
	progress_chan <- C.GoString(msg)
}

func xtracfg_init() {
	progress_chan = make(chan string, 16)
	/*go func() {
		for {
			msg, more := <-progress_chan
			if !more {
				return
			}

			log.Println("PROGRESS", msg)
		}
	}()*/

	C.xtracfg_init()
	C.xtracfg_progress_subscribe((C.progress_callback)(unsafe.Pointer(C.progress)))
}

// New is
func New(source *string, driver *string, verbose *bool, debug *bool) *Store {
	xtracfg_init()

	return &Store{source: source, driver: driver, debug: debug, verbose: verbose, Progress: &progress_chan}
}

// Label is
func (store Store) Label() string {
	label := fmt.Sprintf("%s(%s)", *store.driver, *store.source)

	return label
}

func params(store Store) map[string]interface{} {
	var src string
	if strings.HasPrefix(*store.source, "/") {
		if *store.debug {
			fmt.Printf("Absolute path %s\n", *store.source)
		}
		src = *store.source
	} else {
		path, _ := filepath.Abs(*store.source)
		if *store.debug {
			fmt.Printf("Joined relative path %s -> %s\n", *store.source, path)
		}
		src = path
	}

	return map[string]interface{}{"source": src, "driver": *store.driver, "verbose": strconv.FormatBool(*store.verbose), "debug": strconv.FormatBool(*store.debug)}
}

// Connect is
func (store Store) Connect() error {
	params := params(store)

	response, err := store.request(params, "connect")

	if err == nil && response.Error != nil {
		err = fmt.Errorf(*response.Error)
	}

	if err == nil {
		if *store.verbose {
			//PrintResults(*response.Results, err)
			// fmt.Printf("Connected to store source %s\n\n", store.Label())
		}
		return nil
	}

	return fmt.Errorf("Could not connect to store source %s: %s\n", store.Label(), err)
}

// Handle is
func (store Store) Handle(parameters map[string]interface{}, command string, subcommands ...string) ([]Result, error) {
	params := params(store)
	for key, value := range parameters {
		params[key] = value
	}

	response, err := store.request(params, command, subcommands...)

	if err != nil {
		return nil, err
	}

	if response.Error != nil {
		return nil, fmt.Errorf("Error: %s", *response.Error)
	}

	if response.Results == nil {
		return []Result{}, nil
	}

	for i := range *response.Results {
		(*response.Results)[i].Details = response.Details
	}

	return *response.Results, nil
}

func (store Store) request(parameters map[string]interface{}, command string, subcommands ...string) (response *Response, err error) {
	parameters["command"] = command

	if len(subcommands) > 0 {
		parameters["subcommand"] = subcommands[0]
	}

	request, err := json.Marshal(parameters)

	if err != nil {
		return nil, fmt.Errorf("Error: Failed to marshal the request body. %s", err)
	}

	resp := store.Request(request)

	response = &Response{}
	err = json.Unmarshal(resp, response)

	if err != nil {
		return nil, fmt.Errorf("Error: Failed to read the response body. %s", err)
	}

	return response, nil
}

func (store Store) Request(request []byte) (response []byte) {
	request2 := string(request)

	if *store.debug {
		fmt.Println("->", request2)
	}

	var err C.int
	r := C.xtracfg_execute(C.CString(request2), &err)
	response = []byte(C.GoString(r))
	C.free(unsafe.Pointer(r))

	if *store.debug {
		fmt.Println("<-", string(response))
	}

	return response
}
