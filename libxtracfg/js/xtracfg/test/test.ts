import { connect, Transport, TransportCreator, Listener } from "../src";

let listener: undefined | Listener;

const transport: TransportCreator = () => {
  return async (): Promise<Transport> => {
    return {
      send: async (request) => {
        if (listener) {
          console.log("sending", request);
          const response =
            request.command === "hello"
              ? { results: [{ status: "SUCCESS", message: "world" }] }
              : { error: "unknown command " + request.command };

          listener.call(null, response);
        }
      },
      listen: async (handler) => {
        console.log("listening", handler);
        listener = handler;
      },
      stop: async () => {
        listener = undefined;
      },
    };
  };
};

const xtracfg = connect(transport);

xtracfg.listen(
  (response) => {
    console.log("success", response);
  },
  (error) => {
    console.error("error", error);
  }
);

xtracfg.send({ command: "hello" });

xtracfg.send({ command: "foo" });

xtracfg.disconnect();

xtracfg.send({ command: "ERROR: SHOULD NOT BE SENT" });
