package handler

import (
	"fmt"
	"testing"

	"github.com/interactive-instruments/xtraplatform-cli/libxtracfg/go/gen"
	"github.com/stretchr/testify/assert"
)

type notACommand struct {
}

func (c *notACommand) IsCommand() bool {
	return false
}

func TestInvalid(t *testing.T) {
	result, err := defaultClient.Execute(&notACommand{})

	assert.NotNil(t, err)
	assert.Nil(t, result)
}

func TestFailure(t *testing.T) {
	command := gen.NewConnect(gen.NewBase(nil, nil, nil))

	client := Client{
		Send: func(request []byte) (response []byte) {
			request2 := string(request)

			t.Logf("-> %v", request2)

			response = []byte(`{"error":"something went wrong"}`)

			t.Logf("<- %v", string(response))

			return response
		},
	}

	result, err := client.Execute(command)

	assert.Nil(t, err)
	assert.NotNil(t, result)
	assert.NotNil(t, result.Failure, "result.Failure")
	assert.Nil(t, result.Regular, "result.Regular")
	assert.Equal(t, "something went wrong", result.Error, "result.Error")

	t.Logf("regular: %v, failure: %v", result.Regular, result.Failure)
}

func TestSuccess(t *testing.T) {
	command := gen.NewConnect(gen.NewBase(nil, nil, nil))

	client := Client{
		Send: func(request []byte) (response []byte) {
			request2 := string(request)

			t.Logf("-> %v", request2)

			response = []byte(`{"messages":[{"status":"SUCCESS","text":"everything is fine"}],"details":{"foo":"bar"}}`)

			t.Logf("<- %v", string(response))

			return response
		},
	}

	result, err := client.Execute(command)

	assert.Nil(t, err)
	assert.NotNil(t, result)
	assert.Nil(t, result.Failure, "result.Failure")
	assert.NotNil(t, result.Regular, "result.Regular")
	assert.NotEmpty(t, result.Messages, "result.Messages")
	assert.Equal(t, "everything is fine", result.Messages[0].Text, "result.Messages[0].Text")
	assert.Equal(t, gen.MessageTypeSUCCESS, result.Messages[0].Status, "result.Messages[0].Status")
	assert.Contains(t, result.Details, "foo", "result.Details")
	assert.Equal(t, "bar", result.Details["foo"], "result.Details[\"foo\"]")

	t.Logf("regular: %v, failure: %v", result.Regular, result.Failure)
}

func TestValid(t *testing.T) {
	command := gen.NewConnect(gen.NewBase(nil, nil, nil))
	commandJson := `{"command":"Connect","options":{"source":"./","verbose":false,"debug":false}}`

	client := Client{
		Send: func(request []byte) (response []byte) {
			request2 := string(request)

			t.Logf("-> %v", request2)

			assert.Equal(t, commandJson, request2)

			response = []byte("{}")

			t.Logf("<- %v", string(response))

			return response
		},
	}

	result, err := client.Execute(command)

	if err != nil {
		t.Fatalf("expected result, got error: %v", err)
	}

	t.Logf("result: %v", result)
}

var defaultClient = Client{
	Send: request,
}

func request(request []byte) (response []byte) {
	request2 := string(request)

	fmt.Println("->", request2)

	response = []byte("{}")

	fmt.Println("<-", string(response))

	return response
}
