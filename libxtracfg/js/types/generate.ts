import { mkdirSync } from "fs";

import { generateJsonSchema } from "./json-schema/index.ts";
import { generateJsValidators } from "./js/index.ts";
import { generateJavaClasses } from "./generate-java-classes.ts";
import { generateGo } from "./go/index.ts";
import { Result, write } from "./common/index.ts";

const basePath = "./src/gen";
const javaBasePath = "../../java/src/main/java";
const javaPkg = "de.ii.xtraplatform.cli.gen";
const goBasePath = "../../go";
const goPkg = "gen";

const dataNs = ["Command", "Options", "Result", "Misc"];

type SchemaResult = Result & { obj: any };

type GenCfg = {
  basePath: string;
};

type GenLangCfg = GenCfg & {
  pkg: string;
};

type Cfg = {
  java?: GenLangCfg;
  go?: GenLangCfg;
  verbose?: boolean;
};

//TODO: restructure -> xtracfg/bin, xtracfg/lib/*, gen in root?
const generateAll = (cfg: Cfg) => {
  console.log("Generating code from TypeScript definitions");

  const { verbose, java: javaCfg, go: goCfg } = cfg;

  mkdirSync(basePath, { recursive: true });

  const schema: SchemaResult = generateJsonSchema(
    "JSON Schema",
    "./src/index.ts",
    dataNs
  );

  write(schema, basePath, verbose);

  const js: Result = generateJsValidators(schema.obj);

  write(js, basePath, verbose);

  const java: Result = generateJavaClasses(schema.obj, javaPkg, dataNs);

  write(java, javaBasePath, verbose);

  const go: Result = generateGo("Go Client", schema.obj, goPkg, dataNs);

  write(go, goBasePath, verbose);
};

generateAll({
  verbose: true,
  java: {
    basePath: "../../java/src/main/java",
    pkg: "de.ii.xtraplatform.cli.gen",
  },
  go: {
    basePath: "../../go",
    pkg: "gen",
  },
});
