package client

import (
	"log"
	"net/http"

	"github.com/gorilla/websocket"
)

var store Store

func OpenWebsocket(store2 Store) {
	store = store2

	http.HandleFunc("/sock", wsEndpoint)

	log.Fatal(http.ListenAndServe(":8080", nil))
}

// We'll need to define an Upgrader
// this will require a Read and Write buffer size
var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

func wsEndpoint(w http.ResponseWriter, r *http.Request) {
	upgrader.CheckOrigin = func(r *http.Request) bool { return true }

	// upgrade this connection to a WebSocket
	// connection
	ws, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println(err)
	}

	log.Println("Client Connected")
	err = ws.WriteMessage(1, []byte("Hi Client!"))
	if err != nil {
		log.Println(err)
	}

	// listen indefinitely for new  progress messages coming from the store and pass them to the websocket
	progress_writer(ws)

	// listen indefinitely for new messages coming
	// through on our WebSocket connection
	reader(ws)
}

// define a reader which will listen for
// new messages being sent to our WebSocket
// endpoint
func reader(conn *websocket.Conn) {
	for {
		// read in a message
		messageType, request, err := conn.ReadMessage()

		if err != nil {
			log.Println(err)
			return
		}

		response := store.Request(request)

		if err := conn.WriteMessage(messageType, response); err != nil {
			log.Println(err)
			return
		}

	}
}

func progress_writer(conn *websocket.Conn) {
	go func() {
		for {
			msg, more := <-*store.progress
			if more {
				if err := conn.WriteMessage(websocket.TextMessage, []byte(msg)); err != nil {
					log.Println(err)
				}
			} else {
				return
			}
		}
	}()
}
