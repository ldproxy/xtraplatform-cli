package client

import (
	"encoding/json"
	"fmt"
)

type CommandHandler func(command string) string

// Store is
type Store struct {
	source *string
	driver *string
	debug  *bool
}

var handle CommandHandler

// New is
func New(source *string, driver *string, debug *bool) *Store {
	return &Store{source: source, driver: driver, debug: debug}
}

func Init(handle_command CommandHandler) {
	handle = handle_command
}

// Label is
func (store Store) Label() string {
	label := fmt.Sprintf("%s(%s)", *store.driver, *store.source)

	return label
}

// Connect is
func (store Store) Connect() error {
	response, err := store.Request(nil, "connect")

	if err == nil && response.Error != nil {
		err = fmt.Errorf(*response.Error)
	}

	if err == nil {
		if *store.debug {
			fmt.Printf("Connected to store source %s\n\n", store.Label())
		}
		return nil
	}

	return fmt.Errorf("Could not connect to store source %s: %s\n", store.Label(), err)
}

// Check is
func (store Store) Check() ([]Result, error) {
	params := map[string]string{"foo": "f", "bar": "b"}
	response, err := store.Request(params, "check")

	if err != nil {
		return nil, err
	}

	if response.Error != nil {
		return nil, fmt.Errorf("Error: Failed to read the HTTP response body. %s", *response.Error)
	}

	if response.Results == nil {
		return []Result{}, nil
	}

	return *response.Results, nil
}

func (store Store) Request(parameters map[string]string, command string, subcommands ...string) (response *Response, err error) {

	var uri = fmt.Sprintf("/%s", command)

	if *store.debug {
		fmt.Println("->", uri)
	}

	resp := requestC(uri)

	response = &Response{}
	err = json.Unmarshal(resp, response)

	if err != nil {
		return nil, fmt.Errorf("Error: Failed to read the HTTP response body. %s", err)
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
