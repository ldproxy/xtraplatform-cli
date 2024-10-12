package util

import (
	"log"
	"net/http"

	"github.com/gorilla/websocket"
	"github.com/interactive-instruments/xtraplatform-cli/libxtracfg/go/xtracfg"
)

var store xtracfg.Store

func OpenWebsocket(store2 xtracfg.Store, port string) {
	store = store2

	handler := newLimitHandler(1, http.HandlerFunc(wsEndpoint))

	log.Fatal(http.ListenAndServe(port, handler))
}

// We'll need to define an Upgrader
// this will require a Read and Write buffer size
var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

const WS_CLOSED = "WS_CLOSED"

func wsEndpoint(w http.ResponseWriter, r *http.Request) {
	upgrader.CheckOrigin = func(r *http.Request) bool { return true }

	// upgrade this connection to a WebSocket
	// connection
	ws, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println(err)
	}

	log.Println("Client Connected")

	// listen indefinitely for new  progress messages coming from the store and pass them to the websocket
	progress_writer(ws)

	log.Println("reading")

	// listen indefinitely for new messages coming through on our WebSocket connection
	reader(ws)

	*store.Progress <- WS_CLOSED

	log.Println("done reading")
}

func reader(conn *websocket.Conn) {
	for {
		// read in a message
		_, request, err := conn.ReadMessage()

		if err != nil {
			log.Println("ERR1", err)
			return
		}

		response := store.Request(request)

		if err := conn.WriteMessage(websocket.TextMessage, response); err != nil {
			log.Println("ERR2", err)
			return
		}

	}
}

func progress_writer(conn *websocket.Conn) {
	go func() {
		for {
			msg, more := <-*store.Progress
			if !more || msg == WS_CLOSED {
				log.Println("ERR5", more, msg)
				return
			}

			if err := conn.WriteMessage(websocket.TextMessage, []byte(msg)); err != nil {
				log.Println("ERR3", err)
				return
			}
		}
	}()
}

type limitHandler struct {
	connc   chan struct{}
	handler http.Handler
}

func (h *limitHandler) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	select {
	case <-h.connc:
		h.handler.ServeHTTP(w, req)
		h.connc <- struct{}{}
	default:
		http.Error(w, "503 too busy", http.StatusServiceUnavailable)
	}
}
func newLimitHandler(maxConns int, handler http.Handler) http.Handler {
	h := &limitHandler{
		connc:   make(chan struct{}, maxConns),
		handler: handler,
	}
	for i := 0; i < maxConns; i++ {
		h.connc <- struct{}{}
	}
	return h
}
