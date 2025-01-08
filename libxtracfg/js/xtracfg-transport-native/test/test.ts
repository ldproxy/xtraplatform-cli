import { connect } from "@xtracfg/core";
import transport from "../src";

const xtracfg = connect(transport, { debug: true });

xtracfg.listen(
  (response) => {
    console.log("success", response);
  },
  (error) => {
    console.error("error", error);
  }
);

xtracfg.send({
  command: "info",
  source: "/Users/pascal/Documents/GitHub/demo",
});

//TODO: proper unit test that checks the response
