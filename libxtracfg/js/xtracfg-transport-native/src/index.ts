import { Transport, TransportCreator } from "xtracfg";
import addon from "xtracfg-native-binding";

let listener: (response: Response) => void;

const transport: TransportCreator = () => {
  return async (): Promise<Transport> => {
    return {
      send: (request) => {
        if (listener) {
          const response = addon.execute(JSON.stringify(request));

          listener.call(null, JSON.parse(response));
        }
      },
      listen: (handler) => {
        listener = handler;
      },
    };
  };
};

export default transport;
