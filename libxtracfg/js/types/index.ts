import { resolve } from "path";

import { generate } from "./ts2x/index.ts";
import { onClass, onNamespace, getAdditional } from "./java.ts";

//TODO: restructure -> xtracfg/bin, xtracfg/lib/*, gen in root?

generate({
  source: resolve("./src/index.ts"),
  verbose: false,
  schema: {
    basePath: "./src/gen",
  },
  go: {
    basePath: "../../go",
    pkg: "gen",
    filePrefixes: { Command: "cmd-", Options: "opts-", Result: "res-" },
  },
  java: {
    basePath: "../../java/src/main/java",
    pkg: "de.ii.xtraplatform.cli.gen",
    classSuffixes: ["Command", "Options", "Result"],
    additionalClasses: getAdditional("de.ii.xtraplatform.cli.gen"),
    hooks: {
      onNamespace,
      onClass,
    },
  },
  ts: {
    basePath: "./src/gen",
  },
});
