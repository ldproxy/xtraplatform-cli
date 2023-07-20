package client

import (
	"encoding/json"
	"fmt"
	"strings"
)

type Status int

const (
	Unknown Status = iota
	Error
	Warning
	Success
	Info
	Confirmation
)

type Result struct {
	Status  Status
	Message *string
}

type Response struct {
	Results *[]Result
	Error   *string
}

var (
	status_name = map[uint8]string{
		uint8(Unknown):      "UNKNOWN",
		uint8(Error):        "ERROR",
		uint8(Warning):      "WARNING",
		uint8(Success):      "SUCCESS",
		uint8(Info):         "INFO",
		uint8(Confirmation): "CONFIRMATION",
	}
	status_value = map[string]uint8{
		"UNKNOWN":      uint8(Unknown),
		"ERROR":        uint8(Error),
		"WARNING":      uint8(Warning),
		"SUCCESS":      uint8(Success),
		"INFO":         uint8(Info),
		"CONFIRMATION": uint8(Confirmation),
	}
)

func HasStatus(results []Result, status Status) bool {
	for _, r := range results {
		if r.Status == status {
			return true
		}
	}
	return false
}

// MarshalJSON must be a *value receiver* to ensure that a Suit on a parent object
// does not have to be a pointer in order to have it correctly marshaled.
func (s Status) MarshalJSON() ([]byte, error) {
	// It is assumed Suit implements fmt.Stringer.
	return json.Marshal(s.String())
}

// UnmarshalJSON must be a *pointer receiver* to ensure that the indirect from the
// parsed value can be set on the unmarshaling object. This means that the
// ParseSuit function must return a *value* and not a pointer.
func (s *Status) UnmarshalJSON(data []byte) (err error) {
	var suits string
	if err := json.Unmarshal(data, &suits); err != nil {
		return err
	}
	if *s, err = parseStatus(suits); err != nil {
		return err
	}
	return nil
}

// String allows Suit to implement fmt.Stringer
func (s Status) String() string {
	return status_name[uint8(s)]
}

// Convert a string to a Suit, returns an error if the string is unknown.
// NOTE: for JSON marshaling this must return a Suit value not a pointer, which is
// common when using integer enumerations (or any primitive type alias).
func parseStatus(s string) (Status, error) {
	s = strings.TrimSpace(strings.ToUpper(s))
	value, ok := status_value[s]
	if !ok {
		return Status(0), fmt.Errorf("%q is not a valid status", s)
	}
	return Status(value), nil
}
