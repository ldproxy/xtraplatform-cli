import { Mutex } from "async-mutex";
import {
  Transport,
  TransportCreator,
  TransportOptions,
  Response,
  Listener,
} from "@xtracfg/core";
import WebSocket from "isomorphic-ws";

const listeners: Listener[] = [];

const broadcast = (response: Response) => {
  listeners.forEach((listener) => listener(response));
};

export const transport: TransportCreator = ({ debug }: TransportOptions) => {
  return async (): Promise<Transport> => {
    const socket = getSocket(debug);

    socket.then((s) => {
      s?.addEventListener("message", (event) => {
        if (typeof event.data !== "string") {
          return;
        }
        const response = JSON.parse(event.data);

        broadcast(response);
      });
    });

    return {
      send: async (request) =>
        socket.then((s) => {
          s?.send(JSON.stringify(request));
        }),
      listen: async (listener) => {
        listeners.push(listener);
      },
    };
  };
};

export default transport;

const mutex = new Mutex();
let _socket: WebSocket;

const getSocket = async (dev?: boolean): Promise<WebSocket | null> => {
  const release = await mutex.acquire();

  if (_socket && _socket.readyState === _socket.OPEN) {
    release();

    return Promise.resolve(_socket);
  }

  if (
    !_socket ||
    _socket.readyState === _socket.CLOSED ||
    _socket.readyState === _socket.CLOSING
  ) {
    if (dev) {
      console.log("CONNECTING to websocket", "ws://localhost:8081/sock");
      _socket = new WebSocket("ws://localhost:8081/sock");
    } else {
      //console.log("CONNECTING to websocket", `ws://${self.location.host}/proxy/8081/`);
      const protocol = self.location.protocol === "https:" ? "wss" : "ws";
      _socket = new WebSocket(
        `${protocol}://${self.location.host}/proxy/8081/`
      );
    }
  }

  return new Promise((resolve, reject) => {
    _socket.addEventListener("open", () => {
      resolve(_socket);
      release();
    });
    _socket.addEventListener("error", () => {
      reject("websocket error");
      release();
    });
    _socket.addEventListener("close", (event) => {
      if (dev && event.wasClean) {
        console.log("websocket was closed", event.code, event.reason);
      } else if (!event.wasClean) {
        console.error(
          "websocket was closed unexpectedly",
          event.code,
          event.reason
        );
      }
    });
  });
};
