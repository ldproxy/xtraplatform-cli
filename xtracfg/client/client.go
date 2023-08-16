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
	source  *string
	driver  *string
	verbose *bool
	debug   *bool
}

var handle CommandHandler

// New is
func New(source *string, driver *string, verbose *bool, debug *bool) *Store {
	return &Store{source: source, driver: driver, debug: debug, verbose: verbose}
}

func Init(handle_command CommandHandler) {
	handle = handle_command
}

// Label is
func (store Store) Label() string {
	label := fmt.Sprintf("%s(%s)", *store.driver, *store.source)

	return label
}

func params(store Store) map[string]string {
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

	return map[string]string{"source": src, "driver": *store.driver, "verbose": strconv.FormatBool(*store.verbose), "debug": strconv.FormatBool(*store.debug)}
}

// Connect is
func (store Store) Connect() error {
	params := params(store)

	response, err := store.Request(params, "connect")

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
func (store Store) Handle(parameters map[string]string, command string, subcommands ...string) ([]Result, error) {
	params := params(store)
	for key, value := range parameters {
		params[key] = value
	}

	response, err := store.Request(params, command, subcommands...)

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

func (store Store) Request(parameters map[string]string, command string, subcommands ...string) (response *Response, err error) {

	var uri = fmt.Sprintf("/%s", command)
	var first = true

	for key, val := range parameters {
		if first {
			uri += "?"
			first = false
		} else {
			uri += "&"
		}
		uri += fmt.Sprintf("%s=%s", key, val)
	}

	if *store.debug {
		fmt.Println("->", uri)
	}

	resp := requestC(uri)

	response = &Response{}
	err = json.Unmarshal(resp, response)

	if err != nil {
		return nil, fmt.Errorf("Error: Failed to read the response body. %s", err)
	}

	return response, nil
}

func requestC(command string) (response []byte) {
	if handle == nil {
		handle = mockHandler
	}

	response = []byte(handle(command))

	return response
}
