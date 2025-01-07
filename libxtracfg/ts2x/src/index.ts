import { Result, write } from "./common/io.ts";
import { constsNs, enumsNs } from "./common/schema.ts";
import { generateGo } from "./go/index.ts";
import { ClassGenerators, generateJava, Hooks } from "./java/index.ts";
import { generateJsValidators } from "./js/index.ts";
import { generateJsonSchema, SchemaResult } from "./json-schema/index.ts";

export {
  type Definition,
  type DefinitionOrBoolean,
} from "typescript-json-schema";

export {
  type ClassGenerators,
  type ClassGenerator,
  type Hooks,
  type OnNamespace,
  type OnClass,
} from "./java/index.ts";

export { type Generator } from "./common/index.ts";
export { write, type File, type Result } from "./common/io.ts";

export type GenCfg = {
  basePath: string;
  label?: string;
};

export type GenLangCfg = GenCfg & {
  pkg: string;
};

export type GoCfg = GenLangCfg & {
  filePrefixes: Record<string, string>;
};

export type JavaCfg = GenLangCfg & {
  classSuffixes?: string[];
  additionalClasses?: ClassGenerators;
  hooks?: Hooks;
};

export type Cfg = {
  source: string;
  go?: GoCfg;
  java?: JavaCfg;
  ts?: GenCfg;
  schema?: GenCfg;
  verbose?: boolean;
};

export const generate = (cfg: Cfg) => {
  console.log("Generating code from TypeScript definitions");

  const { source, verbose, schema: schemaCfg, java, go, ts } = cfg;

  const schema: SchemaResult = generateJsonSchema(
    schemaCfg?.label || "JSON Schema",
    source
  );

  if (schemaCfg) {
    write(schema, schemaCfg.basePath, verbose);
  }

  //TODO: do we need dedicated namespaces for consts and enums?
  const dataNs = Array.from(schema.namespaces).filter(
    (ns) => ns !== constsNs && ns !== enumsNs
  );
  //console.log("Data namespaces:", dataNs);

  if (ts) {
    const code: Result = generateJsValidators(
      schema.obj,
      ts.label || "Typescript code"
    );

    write(code, ts.basePath, verbose);
  }

  if (java) {
    const code: Result = generateJava(
      java.label || "Java code",
      schema.obj,
      java.pkg,
      dataNs,
      java.classSuffixes || [],
      java.additionalClasses || [],
      java.hooks
    );

    write(code, java.basePath, verbose);
  }

  if (go) {
    const code: Result = generateGo(
      go.label || "Go code",
      schema.obj,
      go.pkg,
      dataNs,
      go.filePrefixes
    );

    write(code, go.basePath, verbose);
  }
};
