package handler

import (
	"encoding/json"
	"fmt"

	"github.com/interactive-instruments/xtraplatform-cli/libxtracfg/go/gen"
)

type Client struct {
	Send func(request []byte) (response []byte)
}

func (client Client) Execute(command gen.Command) (result *gen.Result, err error) {
	if !command.IsCommand() {
		return nil, fmt.Errorf("invalid command: %v", command)
	}

	request, err := json.Marshal(command)

	if err != nil {
		return nil, fmt.Errorf("failed to marshal the request body: %w", err)
	}

	response := client.Send(request)

	result = &gen.Result{}
	err = json.Unmarshal(response, result)

	if err != nil {
		return nil, fmt.Errorf("failed to read the response body: %w", err)
	}

	return result, nil
}
