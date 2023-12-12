package client

import (
	"encoding/json"
	"fmt"
	"path/filepath"
	"strconv"
	"strings"
)

type CommandHandler func(command string) string

// Store is
type Store struct {
	source   *string
	driver   *string
	verbose  *bool
	debug    *bool
	progress *chan string
}

var handle CommandHandler
var progress chan string

// New is
func New(source *string, driver *string, verbose *bool, debug *bool) *Store {
	return &Store{source: source, driver: driver, debug: debug, verbose: verbose, progress: &progress}
}

func Init(handle_command CommandHandler, progress_chan chan string) {
	handle = handle_command
	progress = progress_chan
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
			PrintResults(*response.Results, err)
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

	return requestC(request2)
}

func requestC(command string) (response []byte) {
	if handle == nil {
		handle = mockHandler
	}

	response = []byte(handle(command))

	return response
}
