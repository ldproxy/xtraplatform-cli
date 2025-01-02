import { Definition, DefinitionOrBoolean } from "typescript-json-schema";

import {
  firstLetterUpperCase,
  constsPath,
  enumsPath,
  Generator,
  Defs,
  constsNs,
  enumsNs,
  defs,
  getValue,
} from "../common/index.ts";
import { Result } from "../generate.ts";
import {
  generateDataRecord,
  generateIdentifiersClass,
  generateInterface,
} from "./data.ts";

//TODO: remove suffixNs
export const generateJava = (
  name: string,
  schema: Definition,
  pkg: string,
  dataNs: string[],
  suffixNs: string[],
  onNs: (ns: string, nsInterface?: string, nsInterfaceDef?: Definition) => void,
  onClass: (ns: string, name: string, def?: any) => void
): Result => {
  const definitions = schema.definitions || {};
  let consts: Defs = [];
  let enums: Defs = [];
  const result: Result = { name, files: [] };

  for (const [ns, entries] of Object.entries(definitions)) {
    if (ns === constsNs) {
      consts = defs(entries);
      continue;
    }
    if (ns === enumsNs) {
      enums = defs(entries);
      continue;
    }
    let nsInterface: string | undefined;
    let nsInterfaceDef: Definition | undefined;
    let nsDiscriminator: string | undefined;
    let nsDiscriminators: { [key: string]: string } = {};

    for (const [key, def] of Object.entries(entries)) {
      if (dataNs.includes(ns) && def && def.interface) {
        nsInterface = getName(key, ns, suffixNs);
        nsInterfaceDef = def as Definition;
        if (def.discriminator) {
          nsDiscriminator = def.discriminator as string;
        }
      }
    }

    onNs(ns, nsInterface, nsInterfaceDef);

    for (const [key, def] of Object.entries(entries)) {
      if (dataNs.includes(ns) && def && !def.interface) {
        if (
          nsDiscriminator &&
          def.properties &&
          def.properties[nsDiscriminator]
        ) {
          nsDiscriminators[getName(key, ns, suffixNs)] = getValue(
            def.properties[nsDiscriminator],
            true
          );
        }

        onClass(ns, getName(key, ns, suffixNs), def);

        result.files.push(
          generateClass(
            getName(key, ns, suffixNs),
            pkg,
            generateDataRecord(def, suffixNs, nsInterface)
          )
        );
      }
    }
    if (nsInterface) {
      result.files.push(
        generateClass(
          nsInterface,
          pkg,
          generateInterface(
            nsInterfaceDef as Definition,
            suffixNs,
            nsDiscriminators,
            nsDiscriminator
          )
        )
      );
    }
  }

  result.files.push(
    generateClass(
      "Identifiers",
      pkg,
      generateIdentifiersClass(consts, enums, suffixNs)
    )
  );

  return result;
};

const getName = (name: string, ns: string, suffixNs: string[]) => {
  return suffixNs.includes(ns) && name !== ns ? name + ns : name;
};

const identifiersPrefix = "Identifiers.";

export const generateClass = (
  name: string,
  pkg: string,
  generate: Generator
) => {
  const dir = pkg.replaceAll(".", "/");
  const code = generate(name, pkg);

  return { path: `${dir}/${name}.java`, content: code };
};

export const getType = (
  schema: DefinitionOrBoolean,
  suffixNs: string[]
): string => {
  if (typeof schema === "boolean") {
    return "boolean";
  }
  if (schema.type) {
    if (schema.type === "object" && !schema.properties) {
      return "java.util.Map<String, Object>";
    }
    if (schema.type === "array" && schema.items) {
      return `java.util.List<${getType(
        schema.items as DefinitionOrBoolean,
        suffixNs
      )}>`;
    }
    const type =
      schema.type === "integer"
        ? "int"
        : schema.type === "number"
        ? "double"
        : schema.type === "boolean"
        ? "boolean"
        : "String";
    if (Object.hasOwn(schema, "optional")) {
      return `java.util.Optional<${firstLetterUpperCase(type)}>`;
    }
    return firstLetterUpperCase(schema.type as string);
  }
  if (schema.$ref) {
    if (schema.$ref.includes(constsPath)) {
      return "String";
    }
    let type = schema.$ref.substring(schema.$ref.lastIndexOf("/") + 1);
    if (type.includes(".")) {
      type = type.substring(0, type.lastIndexOf("."));
    }
    const prefix = schema.$ref.includes(enumsPath) ? identifiersPrefix : "";
    const suffix =
      suffixNs.find((ns) => type !== ns && schema.$ref?.includes(`/${ns}/`)) ||
      "";
    type = `${prefix}${type}${suffix}`;

    if (Object.hasOwn(schema, "optional")) {
      return `java.util.Optional<${type}>`;
    }
    return type;
  }

  return "";
};
