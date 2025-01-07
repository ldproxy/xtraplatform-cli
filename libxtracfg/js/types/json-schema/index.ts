import { resolve } from "path";
import * as TJS from "typescript-json-schema";

//TODO: configurable settings
export const generateJsonSchema = (name: string, dataNs: string[]) => {
  const schema = generate(dataNs);
  console.log(schema.string);

  return {
    name,
    obj: schema.js,
    files: [{ path: "api-schema.json", content: schema.string }],
  };
};

export const validationKeywordsBoolean = [
  "hide",
  "optional",
  "interface",
  "javaContextInit",
];
export const validationKeywordsString = ["discriminator"];

const constNs = ["Enums", "Consts"];

const generate = (dataNs: string[]) => {
  const namespaces = [...constNs, ...dataNs];

  // optionally pass argument to schema generator
  const settings: TJS.PartialArgs = {
    required: true,
    noExtraProps: true,
    //id: "Xtracfg",
    validationKeywords: [
      ...validationKeywordsBoolean,
      ...validationKeywordsString,
    ],
    ref: true,
    aliasRef: true,
    //titles: true,
    //constAsEnum: true,
  };

  // optionally pass ts compiler options
  const compilerOptions = {
    strictNullChecks: true,
  };

  const program = TJS.getProgramFromFiles(
    [resolve("./src/index.ts")],
    compilerOptions,
    "./"
  );

  const generator = TJS.buildGenerator(program, settings);

  if (!generator) {
    throw new Error("Failed to create schema generator");
  }

  const symbols = generator
    .getUserSymbols()
    .filter(
      (s) => !s.endsWith("_1") && namespaces.some((ns) => s.startsWith(ns))
    );

  const schema = generator.getSchemaForSymbols(symbols, true, true);

  const fixedSchema = fix(schema, namespaces);

  return {
    js: fixedSchema,
    string: JSON.stringify(fixedSchema, null, 2),
  };
};

const fix = (schema: TJS.Definition, namespaces: string[]) => {
  const definitions = schema.definitions || {};
  const newDefinitions: { [key: string]: any } = {};
  namespaces.forEach((ns) => {
    newDefinitions[ns] = {};
  });

  for (const [key, value] of Object.entries(definitions)) {
    handleKey(key, value, newDefinitions, namespaces);
  }

  const newSchema = { ...schema, definitions: newDefinitions };

  return JSON.parse(
    JSON.stringify(newSchema)
      .replaceAll(new RegExp(`(${namespaces.join("|")})\\.`, "g"), "$1/")
      .replaceAll("_1", "")
  );
};

function handleKey(
  key: string,
  value: any,
  newDefinitions: any,
  namespaces: string[]
) {
  let handled = false;

  for (const namespace of namespaces) {
    if (key.startsWith(`${namespace}.`)) {
      newDefinitions[namespace][key.replace(`${namespace}.`, "")] = value;
      handled = true;
      break;
    }
  }

  if (!handled) {
    newDefinitions[key] = value;
  }
}
