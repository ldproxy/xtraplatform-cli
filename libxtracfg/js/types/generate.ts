import { existsSync, mkdirSync, readFileSync, writeFileSync } from "fs";
import { dirname } from "path";

import { generateJsonSchema } from "./generate-json-schema.ts";
import { generateJsValidators } from "./generate-js-validators.ts";
import { generateJavaClasses } from "./generate-java-classes.ts";
import { generateGoStructs } from "./generate-go-structs.ts";

const basePath = "./src/gen";
const javaBasePath = "../../java/src/main/java";
const javaPkg = "de.ii.xtraplatform.cli.gen";

type Files = { path: string; content: string }[];
export type Result = { name: string; files: Files };
type SchemaResult = Result & { obj: any };

//TODO: move writing here, compare with existing before writing
//TODO: restructure -> xtracfg/bin, xtracfg/lib/*, gen in root?
const generateAll = (verbose?: boolean) => {
  console.log("Generating code from TypeScript definitions");

  mkdirSync(basePath, { recursive: true });

  const schema: SchemaResult = generateJsonSchema();

  write(schema, basePath, verbose);

  const js: Result = generateJsValidators(schema.obj);

  write(js, basePath, verbose);

  const java: Result = generateJavaClasses(schema.obj, javaPkg);

  write(java, javaBasePath, verbose);

  const go: Result = generateGoStructs(schema.obj);

  write(go, basePath, verbose);
};

const write = (
  result: Result,
  basePath: string,
  verbose?: boolean
): boolean => {
  let noChanges = true;
  const messages = [];

  for (const { path, content } of result.files) {
    const fullPath = `${basePath}/${path}`;
    const oldContent = existsSync(fullPath)
      ? readFileSync(fullPath, "utf8")
      : "";

    if (oldContent === content) {
      if (verbose) messages.push(`  - ${path}: NO CHANGES`);
      continue;
    }

    mkdirSync(dirname(fullPath), { recursive: true });
    writeFileSync(fullPath, content);
    noChanges = false;

    if (verbose) messages.push(`  - ${path}: UPDATED`);
  }

  console.log(`- ${result.name}: ${noChanges ? "NO CHANGES" : "UPDATED"}`);
  messages.forEach((msg) => console.log(msg));

  return noChanges;
};

generateAll(false);
