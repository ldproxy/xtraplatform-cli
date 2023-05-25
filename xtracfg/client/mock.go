package client

import "fmt"

func mockHandler(command string) string {
	fmt.Printf("MOCK JNI Command: %s\n", command)

	resp := `
	   	{
	   		"results": [
	   			{
	   				"status": "ERROR",
	   				"message": "Doh!"
	   			},
	   			{
	   				"status": "WARNING",
	   				"message": "Doh!"
	   			},
	   			{
	   				"status": "SUCCESS",
	   				"message": "Doh!"
	   			}
	   		]
	   	}`

	fmt.Printf("MOCK JNI Result: %s\n", resp)

	return resp
}
