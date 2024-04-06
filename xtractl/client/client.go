package client

import (
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"sort"
	"strings"
)

// Client is
type Client struct {
	protocol   string
	host       *string
	port       *int
	password   *string
	debug      *bool
	httpClient *http.Client
}

// Entity is
type Entity struct {
	Id, Typ, Status string
}

type entity struct {
	Id     string
	Status string
}

// Value is
type Value struct {
	Path, Typ string
}

type value struct {
	Path string
}

// LogStatus is
type LogStatus struct {
	Level  string
	Filter map[string]bool
}

// New is
func New(host *string, port *int, password *string, debug *bool) *Client {
	return &Client{protocol: "http", host: host, port: port, password: password, debug: debug, httpClient: &http.Client{}}
}

// Login is
func (client Client) Login() error {

	_, err := client.Request("/ping", "GET", true, nil, "")

	if err == nil {
		if *client.debug {
			fmt.Printf("Connected to %s://%s:%d\n\n", client.protocol, *client.host, *client.port)
		}
		return nil
	}

	return fmt.Errorf("could not connect to %s://%s:%d\n", client.protocol, *client.host, *client.port)
}

// Entities is
func (client Client) Entities() ([]Entity, error) {

	body, err := client.Request("/entities", "GET", false, nil, "")

	if err != nil {
		return nil, err
	}

	if body != nil {
		var result map[string][]entity
		err := json.Unmarshal(body, &result)

		if err != nil {
			return nil, err
		}

		var entities []Entity

		// needed to preserve order
		types := make([]string, 0)
		for typ := range result {
			types = append(types, typ)
		}
		sort.Strings(types)

		for _, typ := range types {
			for _, entity := range result[typ] {
				entities = append(entities, Entity{entity.Id, typ, entity.Status})
			}
		}

		return entities, nil
	}

	return nil, nil
}

// EntitiesJson is
func (client Client) EntitiesJson() ([]byte, error) {

	body, err := client.Request("/entities", "GET", false, nil, "")

	if err != nil {
		return nil, err
	}
	return body, nil
}

// Reload is
func (client Client) Reload(ids []string, types []string) error {

	query := fmt.Sprintf("?ids=%s&types=%s", strings.Join(ids, ","), strings.Join(types, ","))

	_, err := client.Request("/tasks/reload-entities"+query, "POST", true, nil, "")

	return err
}

// Values is
func (client Client) Values() ([]Value, []string, error) {

	body, err := client.Request("/values", "GET", false, nil, "")

	if err != nil {
		return nil, nil, err
	}

	if body != nil {
		var result map[string][]value
		err := json.Unmarshal(body, &result)

		if err != nil {
			return nil, nil, err
		}

		var values []Value

		// needed to preserve order
		types := make([]string, 0)
		for typ := range result {
			types = append(types, typ)
		}
		sort.Strings(types)

		for _, typ := range types {
			for _, value := range result[typ] {
				values = append(values, Value{value.Path, typ})
			}
		}

		return values, types, nil
	}

	return nil, nil, nil
}

// ValuesJson is
func (client Client) ValuesJson() ([]byte, error) {

	body, err := client.Request("/values", "GET", false, nil, "")

	if err != nil {
		return nil, err
	}
	return body, nil
}

// ReloadValue is
func (client Client) ReloadValue(paths []string) error {

	query := fmt.Sprintf("?paths=%s", strings.Join(paths, ","))

	_, err := client.Request("/tasks/reload-values"+query, "POST", true, nil, "")

	return err
}

// LogStatus is
func (client Client) LogStatusJson() ([]byte, error) {

	body, err := client.Request("/logs", "GET", false, nil, "")

	if err != nil {
		return nil, err
	}

	return body, nil
}

// LogStatus is
func (client Client) LogStatus() (*LogStatus, error) {

	body, err := client.Request("/logs", "GET", false, nil, "")

	if err != nil {
		return nil, err
	}

	if body != nil {
		var result LogStatus
		err := json.Unmarshal(body, &result)

		if err != nil {
			return nil, err
		}

		return &result, nil
	}

	return nil, nil
}

// LogLevel is
func (client Client) LogLevel(level string) error {

	query := fmt.Sprintf("?logger=ROOT&level=%s", level)

	_, err := client.Request("/tasks/log-level"+query, "POST", true, nil, "")

	return err
}

// LogFilter is
func (client Client) LogFilter(filters []string, disable bool) error {

	var op string
	if disable {
		op = "disable"
	} else {
		op = "enable"
	}

	query := fmt.Sprintf("?%s=%s", op, strings.Join(filters, ","))

	_, err := client.Request("/tasks/log-filter"+query, "POST", true, nil, "")

	return err
}

// PurgeTileCache is
func (client Client) PurgeTileCache(id string, collection string, tileMatrixSet string, bbox []string) error {

	query := fmt.Sprintf("?api=%s", id)

	if collection != "" {
		query += fmt.Sprintf("&collection=%s", collection)
	}
	if tileMatrixSet != "" {
		query += fmt.Sprintf("&tileMatrixSet=%s", tileMatrixSet)
	}
	if bbox != nil {
		query += fmt.Sprintf("&bbox=%s", strings.Join(bbox, ","))
	}

	body, err := client.Request("/tasks/purge-tile-cache"+query, "POST", false, nil, "")

	if len(body) > 0 {
		return fmt.Errorf("Error: %s", body)
	}

	return err
}

func (client Client) Request(path string, method string, ignoreBody bool, reqBody io.Reader, token string) (body []byte, err error) {

	var uri = fmt.Sprintf("%s://%s:%d/api%s", client.protocol, *client.host, *client.port, path)

	if *client.debug {
		fmt.Println("->", uri)
	}

	req, err := http.NewRequest(method, uri, reqBody)

	if err != nil {
		return nil, fmt.Errorf("Error: Failed to create the HTTP request. %s", err)
	}

	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	if len(token) > 0 {
		req.SetBasicAuth("ignored", token)
	}

	resp, err := client.httpClient.Do(req)

	if err != nil {
		return nil, fmt.Errorf("Error: Failed to execute the HTTP request. %s", err)
	}

	defer resp.Body.Close()

	if *client.debug {
		fmt.Println("  ", resp.Status)
	}

	if resp.StatusCode >= http.StatusBadRequest {
		return nil, fmt.Errorf("Error: HTTP request returned status code %d", resp.StatusCode)
	}

	if !ignoreBody {
		body, err = ioutil.ReadAll(resp.Body)

		if err != nil {
			return nil, fmt.Errorf("Error: Failed to read the HTTP response body. %s", err)
		}
	}

	if *client.debug {
		fmt.Println("   METHOD:", req.Method)
		fmt.Println("   RESPONSE HEADERS:", resp.Header)
		fmt.Println("   RESPONSE BODY:", string(body))
		fmt.Println("   REQUEST HEADERS:", req.Header)
	}

	return body, nil
}
