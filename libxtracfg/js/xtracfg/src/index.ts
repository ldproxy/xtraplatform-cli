export type Request = {
  command: string;
  subcommand?: string;
  source?: string;
  verbose?: boolean;
  debug?: boolean;
  [key: string]: any;
};

export type Response = {
  error?: string;
  details?: { [key: string]: any };
  results?: Array<{ status: string; message: string }>;
};

export type Error = {
  error?: string;
  status?: string;
  message?: string;
  notification?: string;
  fields?: {
    [key: string]: string;
  };
};

export type Listener = (response: Response) => void;

export type Xtracfg = {
  send: (request: Request) => Promise<void>;
  listen: (
    successHandler: (response: Response) => void,
    errorHandler: (error: Error) => void
  ) => Promise<void>;
  disconnect: () => Promise<void>;
};

export type TransportCreator = (
  options: TransportOptions
) => TransportConnector;

export type TransportConnector = () => Promise<Transport>;

export type TransportOptions = {
  debug?: boolean;
};

export type Transport = {
  send: (request: Request) => Promise<void>;
  listen: (listener: Listener) => Promise<void>;
  stop: () => Promise<void>;
};

export const connect = (
  transport: TransportCreator,
  options: TransportOptions = {}
): Xtracfg => {
  const ensureOpen = transport(options);

  return {
    send: send(options, ensureOpen),
    listen: listen(options, ensureOpen),
    disconnect: disconnect(options, ensureOpen),
  };
};

const send = ({ debug }: TransportOptions, ensureOpen: TransportConnector) => {
  return (request: Request) =>
    ensureOpen()
      .then((transport) => {
        const cmd = JSON.stringify(request);

        if (debug) {
          console.log("sending to xtracfg", cmd);
        }

        transport.send(request);
      })
      .catch((error: Error) => {
        console.error(
          "Could not send command to xtracfg",
          error.message || error
        );
      });
};

const listen = (
  { debug }: TransportOptions,
  ensureOpen: TransportConnector
) => {
  return (
    successHandler: (response: Response) => void,
    errorHandler: (error: Error) => void
  ) =>
    ensureOpen()
      .then((transport) => {
        transport.listen((response) => {
          if (debug) {
            console.log("received from xtracfg", response);
          }

          const error = parseError(response);

          if (!error) {
            successHandler(response);
          } else {
            errorHandler(error);
          }
        });
      })
      .catch((error: Error) => {
        console.error("Could not listen to xtracfg", error.message || error);
      });
};

const disconnect = (
  { debug }: TransportOptions,
  ensureOpen: TransportConnector
) => {
  return () =>
    ensureOpen()
      .then((transport) => {
        transport.stop();
      })
      .catch((error: Error) => {
        console.error(
          "Could not disconnect from xtracfg",
          error.message || error
        );
      });
};

const parseError = (response: Response): Error | undefined => {
  const error = response.error || "";
  const status =
    response.results && response.results.length > 0
      ? response.results[0].status
      : "";
  const message =
    response.results && response.results.length > 0
      ? response.results[0].message
      : "";

  if (error.length === 0 && status !== "ERROR") {
    return undefined;
  }

  if (error === "No 'command' given: {}") {
    return { notification: "Empty Fields" };
  }

  if (message.includes("host") && !message.includes("refused")) {
    return { fields: { host: message.split(",")[0] } };
  } else if (error.includes("Host") && !message.includes("refused")) {
    return { fields: { host: error } };
  } else if (message.includes("database")) {
    return { fields: { database: message } };
  } else if (message.includes("user name")) {
    return { fields: { user: message } };
  } else if (message.includes("password")) {
    return { fields: { user: message, password: message } };
  } else if (error.includes("No id given")) {
    return { fields: { id: error } };
  } else if (error.includes("Id has to")) {
    return { fields: { id: error } };
  } else if (error.includes("with id")) {
    return { fields: { id: error } };
  } else if (message.includes("url")) {
    return { fields: { url: message } };
  } else if (message.includes("URL")) {
    return { fields: { url: message } };
  }

  if (
    (!message.includes("host") &&
      !message.includes("Host") &&
      !message.includes("url") &&
      !message.includes("URL") &&
      !message.includes("database") &&
      !message.includes("user") &&
      !message.includes("password")) ||
    message.includes("refused")
  ) {
    return { notification: error.length > 0 ? error : message };
  }

  return { error, status, message };
};
