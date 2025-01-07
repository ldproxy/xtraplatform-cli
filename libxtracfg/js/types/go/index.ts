import {
  constsNs,
  DataType,
  defs,
  Definition,
  DefinitionOrBoolean,
  Defs,
  enumsNs,
  firstLetterUpperCase,
  Generator,
  Result,
  getDefault,
  getType,
  getValue,
  isConst,
  isEnum,
} from "../common/index.ts";

//TODO: move
const prefixNs = { Command: "cmd-", Options: "opts-", Result: "res-" };

export const generateGo = (
  name: string,
  schema: Definition,
  pkg: string,
  dataNs: string[]
) => {
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

    for (const [key, def] of Object.entries(entries)) {
      if (dataNs.includes(ns) && def && def.interface) {
        const intface = getName(key, ns);

        if (!def.anyOf) {
          nsInterface = intface;
        }

        result.files.push(
          generateFile(intface, ns, pkg, generateInterface(def))
        );
      }
    }

    for (const [key, def] of Object.entries(entries)) {
      if (dataNs.includes(ns) && def && !def.interface) {
        result.files.push(
          generateFile(key, ns, pkg, generateDataStruct(def, nsInterface))
        );
      }
    }
  }

  result.files.push(
    generateFile("consts", "", pkg + "/consts", generateConsts(consts))
  );

  result.files.push(generateFile("enums", "", pkg, generateEnums(enums)));

  result.files.push(generateFile("util", "", pkg, generateUtil()));

  return result;
};

const getName = (
  name: string,
  ns?: string,
  prefixNs?: Record<string, string>
) => {
  return ns && prefixNs && prefixNs[ns]
    ? `${prefixNs[ns]}${name.toLowerCase()}`
    : name.toLowerCase();
};

const generateFile = (
  name: string,
  ns: string,
  pkg: string,
  generate: Generator
) => {
  const dir = pkg.replaceAll(".", "/");
  const file = getName(name, ns, prefixNs);
  const code = generate(getName(name), pkg);

  return { path: `${dir}/${file}.go`, content: code };
};

const getLanguageType = (type: DataType, innerType?: string): string => {
  switch (type) {
    case DataType.STRING:
      return "string";
    case DataType.NUMBER:
      return "float64";
    case DataType.INTEGER:
      return "int";
    case DataType.BOOLEAN:
      return "bool";
    case DataType.OBJECT:
      return innerType || "map[string]interface{}";
    case DataType.ARRAY:
      return `[]${innerType || "interface{}"}`;
    case DataType.OPTIONAL:
      return `*${innerType || "interface{}"}`;
  }
};

const generateDataStruct =
  (schema: Definition, intface?: string): Generator =>
  (nam: string, pkg: string): string => {
    const properties = schema.properties || {};
    const props = defs(properties);
    const toType = (entry: Definition) => getType(entry, getLanguageType);
    const name = firstLetterUpperCase(nam);

    let code = `
package ${pkg}

type ${name} struct {`;
    for (const [key, entry] of props) {
      code += `
  ${firstLetterUpperCase(key)} ${toType(entry)} \`json:"${key}"\``;
    }

    code += `
}
`;

    if (intface) {
      code += `
func (self *${name}) Is${firstLetterUpperCase(intface)}() bool {
  return true
}
`;
    }

    code += `
func New${name}(`;
    for (const [key, entry] of props) {
      if (isConst(entry)) {
        continue;
      }
      const pointer = Object.hasOwn(entry, "default") ? "*" : "";
      code += `
  ${key} ${pointer}${toType(entry)},`;
    }

    code += `
) ${intface ? firstLetterUpperCase(intface) : `*${name}`} {`;

    for (const [key, entry] of props) {
      if (Object.hasOwn(entry, "default")) {
        code += `
  if ${key} == nil { ${key} = ptr(${getDefault(entry)}) }`;
      }
    }

    code += `

  return &${name}{`;
    for (const [key, entry] of props) {
      if (isConst(entry)) {
        code += `
    ${firstLetterUpperCase(key)}: ${getValue(entry).replace(".", "")},`;
        continue;
      }
      const pointer = Object.hasOwn(entry, "default") ? "*" : "";
      code += `
    ${firstLetterUpperCase(key)}: ${pointer}${key},`;
    }

    code += `
  }
}
  `;

    return code;
  };

const generateUtil =
  (): Generator =>
  (name: string, pkg: string): string => {
    let code = `
package ${pkg}  

func ptr[T any](t T) *T { return &t }
      `;

    return code;
  };

const generateInterface =
  (schema: Definition): Generator =>
  (nam: string, pkg: string): string => {
    const toType = (entry: DefinitionOrBoolean) =>
      getType(entry, getLanguageType);
    const name = firstLetterUpperCase(nam);

    let code = `
package ${pkg}  
    `;

    if (schema.anyOf) {
      code += `
type ${name} struct {`;

      for (const def of schema.anyOf) {
        code += `
  *${toType(def)}`;
      }

      code += `
}
    `;
    } else {
      code += `
type ${name} interface {
  Is${name}() bool
}
    `;
    }

    return code;
  };

const generateConsts =
  (consts: Defs): Generator =>
  (name: string, pkg: string): string => {
    const toType = (entry: Definition) => getType(entry, getLanguageType);

    let code = `
package ${name}
      `;

    for (const [key, entry] of consts) {
      if (!isConst(entry)) {
        continue;
      }
      code += `
const ${key} ${toType(entry)} = ${getValue(entry)}`;
    }

    code += `
  `;

    return code;
  };

const generateEnums =
  (enums: Defs): Generator =>
  (name: string, pkg: string): string => {
    const toType = (entry: Definition) => getType(entry, getLanguageType);

    let code = `
package ${pkg}  
        `;

    for (const [key, entry] of enums) {
      if (!isEnum(entry)) {
        continue;
      }
      code += `
type ${key} string
  
const (
  ${entry["enum"]
    ?.map((name) => `${key}${name} ${key} = "${name}"`)
    .join("\n  ")}
)
    `;
    }

    return code;
  };
