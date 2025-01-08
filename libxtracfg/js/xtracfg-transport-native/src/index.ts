import {
  Transport,
  TransportCreator,
  TransportOptions,
  Response,
  Listener,
} from "@xtracfg/core";
import addon from "@xtracfg/native";

const allListeners: Listener[][] = [];

const broadcast = (response: Response) => {
  allListeners.flat().forEach((listener) => listener(response));
};

const transport: TransportCreator = ({ debug }: TransportOptions) => {
  const listeners: Listener[] = [];
  allListeners.push(listeners);

  if (debug) {
    console.log("native transport created");
  }

  addon.subscribe((response: string) => broadcast(JSON.parse(response)));

  const transport: Transport = {
    send: async (request) => {
      if (debug) {
        console.log("sending to native xtracfg", request, listeners.length);
      }

      if (listeners.length > 0) {
        const response = addon.execute(JSON.stringify(request));

        if (debug) {
          console.log("received from native xtracfg", response);
        }

        broadcast(JSON.parse(response));
      }
    },
    listen: async (listener) => {
      listeners.push(listener);
    },
    stop: async () => {
      listeners.length = 0;
    },
  };

  return async (): Promise<Transport> => transport;
};

export default transport;
