import { connect } from "xtracfg";
import transport from "../src";

const xtracfg = connect(transport);

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
