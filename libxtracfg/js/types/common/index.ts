import { Definition, DefinitionOrBoolean } from "typescript-json-schema";

export type Defs = [string, Definition][];
export type Generator = (name: string, pkg: string) => string;

export const defs = (entry: DefinitionOrBoolean): Defs => {
  return typeof entry !== "boolean" ? Object.entries(entry as Definition) : [];
};

export const constsNs = "Consts";
export const enumsNs = "Enums";
export const constsPath = "/Consts/";
export const enumsPath = "/Enums/";

export const getValue = (
  schema: DefinitionOrBoolean,
  stringNotEnum?: boolean
): string => {
  if (typeof schema === "boolean") {
    return schema ? "true" : "false";
  }
  if (schema.const) {
    return `"${schema.const}"`;
  }
  if (Object.hasOwn(schema, "hide")) {
    return `null`;
  }
  if (
    schema.$ref &&
    (schema.$ref.includes(constsPath) || schema.$ref.includes(enumsPath))
  ) {
    const name =
      stringNotEnum && schema.$ref.includes(".")
        ? schema.$ref.substring(schema.$ref.lastIndexOf(".") + 1)
        : schema.$ref.substring(schema.$ref.lastIndexOf("/") + 1);
    return `Identifiers.${name}`;
  }
  return "";
};

export const getDefault = (schema: DefinitionOrBoolean): string => {
  if (typeof schema === "object" && Object.hasOwn(schema, "default")) {
    if (schema.type === "string") {
      return `"${schema.default}"`;
    }
    return `${schema.default}`;
  }
  return "";
};

export const isConst = (schema: DefinitionOrBoolean) => {
  return (
    typeof schema === "object" &&
    (Object.hasOwn(schema, "const") ||
      Object.hasOwn(schema, "hide") ||
      (Object.hasOwn(schema, "$ref") &&
        schema.$ref &&
        (schema.$ref.includes(constsPath) ||
          (schema.$ref.includes(enumsPath) && schema.$ref.includes(".")))))
  );
};

export const isEnum = (schema: DefinitionOrBoolean) => {
  return (
    typeof schema === "object" &&
    (Object.hasOwn(schema, "enum") ||
      (Object.hasOwn(schema, "$ref") &&
        schema.$ref &&
        schema.$ref.includes(enumsPath)))
  );
};

export const firstLetterUpperCase = (str: string) => {
  return str.charAt(0).toUpperCase() + str.slice(1);
};
