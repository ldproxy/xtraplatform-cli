import { Ts2x } from "./src/index.ts";
import { validateCommand } from "./src/gen/validate.ts";

const options = { source: "libxtracfg", foo: "bar" };

const info = new Ts2x.Command.Connect(options);

//info.command = "info";
//info.options.source = "libxtracfg";

console.log(JSON.stringify(info, null, 2));

//validateOptionsStore(options);

const shouldNotValidate = { command: "info", options: {} };

validateCommand(info);
