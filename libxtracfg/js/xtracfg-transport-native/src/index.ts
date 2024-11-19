import { Transport, TransportCreator, Response, Listener } from "@xtracfg/core";
import addon from "@xtracfg/native";

const listeners: Listener[] = [];

const broadcast = (response: Response) => {
  listeners.forEach((listener) => listener(response));
};

const transport: TransportCreator = () => {
  return async (): Promise<Transport> => {
    addon.subscribe((response: string) => broadcast(JSON.parse(response)));

    return {
      send: async (request) => {
        if (listeners.length > 0) {
          const response = addon.execute(JSON.stringify(request));

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
  };
};

export default transport;
