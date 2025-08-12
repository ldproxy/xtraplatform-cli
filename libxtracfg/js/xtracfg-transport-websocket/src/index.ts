import { Mutex } from "async-mutex";
import {
  Transport,
  TransportCreator,
  TransportOptions,
  Response,
  Listener,
} from "@xtracfg/core";
import WebSocket from "isomorphic-ws";

const allListeners: Listener[][] = [];

const broadcast = (response: Response) => {
  allListeners.flat().forEach((listener) => listener(response));
};

export const transport: TransportCreator = ({
  specific,
  debug,
}: TransportOptions) => {
  return async (): Promise<Transport> => {
    const listeners: Listener[] = [];
    allListeners.push(listeners);

    const socket = getSocket(specific?.url, debug);

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
      stop: async () => {
        listeners.length = 0;
        socket.then((s) => s?.close());
      },
    };
  };
};

export default transport;

const mutex = new Mutex();
let _socket: WebSocket;

const getSocket = async (
  url?: string,
  dev?: boolean
): Promise<WebSocket | null> => {
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
    if (!url) {
      console.error("No websocket url given");
      return Promise.reject("No websocket url given");
    }
    if (dev) {
      console.log("CONNECTING to websocket", url);
    }
    _socket = new WebSocket(url);
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
