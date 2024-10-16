import { connect, Transport, TransportCreator } from "../src";

let listener: (response: Response) => void;

const transport: TransportCreator = () => {
  return async (): Promise<Transport> => {
    return {
      send: (request) => {
        console.log("sending", request);
        if (listener) {
          const response =
            request.command === "hello"
              ? { results: [{ status: "SUCCESS", message: "world" }] }
              : { error: "unknown command " + request.command };

          listener.call(null, response);
        }
      },
      listen: (handler) => {
        console.log("listening", handler);
        listener = handler;
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
