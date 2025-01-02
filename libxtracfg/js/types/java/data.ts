import { Definition } from "typescript-json-schema";

import {
  Defs,
  defs,
  firstLetterUpperCase,
  Generator,
  getDefault,
  getValue,
  isConst,
  isEnum,
} from "../common/index.ts";
import { getType } from "./index.ts";

//TODO: use records
export const generateDataClass =
  (schema: Definition, suffixNs: string[] = [], intface?: string): Generator =>
  (name: string, pkg: string): string => {
    const properties = schema.properties || {};
    const props = defs(properties);

    let code = `
package ${pkg};

public class ${name}${intface ? ` implements ${intface}` : ""} {
    `;
    for (const [key, entry] of props) {
      code += `
  private final ${getType(entry, suffixNs)} ${key};`;
    }

    code += `

  public ${name}(`;
    let i = 0;
    for (const [key, entry] of props) {
      i++;
      if (isConst(entry)) {
        continue;
      }
      const comma = i <= Object.keys(properties).length - 1 ? "," : "";
      code += `
    ${getType(entry, suffixNs)} ${key}${comma}`;
    }

    code += `
  ) {`;
    for (const [key, entry] of props) {
      if (isConst(entry)) {
        code += `
    this.${key} = ${getValue(entry)};`;
        continue;
      }
      if (Object.hasOwn(entry, "default")) {
        code += `
    this.${key} = Object.requireNonNullElse(${key},${getDefault(entry)});`;
      } else {
        code += `
    this.${key} = ${key};`;
      }
    }

    code += `
  }
    `;
    for (const [key, entry] of props) {
      code += `
  public ${getType(entry, suffixNs)} get${firstLetterUpperCase(key)}() {
    return ${key};
  }
    `;
    }

    code += `
}

    `;

    return code;
  };

export const generateInterface =
  (
    schema: Definition,
    suffixNs: string[],
    discriminators: { [key: string]: string },
    discriminatorKey?: string
  ): Generator =>
  (name: string, pkg: string): string => {
    const properties = schema.properties || {};

    let code = `
package ${pkg};
  `;

    if (discriminatorKey) {
      code += `
import shadow.com.fasterxml.jackson.annotation.JsonSubTypes;
import shadow.com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "${discriminatorKey}", visible = true)
@JsonSubTypes({`;
      for (const [key, value] of Object.entries(discriminators)) {
        code += `
          @JsonSubTypes.Type(value = ${key}.class, name = ${value}),`;
      }

      code += `
})`;
    }

    code += `
public interface ${name} {
  `;
    for (const [key, entry] of Object.entries(properties)) {
      code += `
${getType(entry, suffixNs)} get${firstLetterUpperCase(key)}();
  `;
    }

    code += `
}
  `;

    return code;
  };

export const generateIdentifiersClass =
  (consts: Defs, enums: Defs, suffixNs: string[]): Generator =>
  (name: string, pkg: string): string => {
    //const properties = schema.properties || {};

    let code = `
package ${pkg};

public interface ${name} {
    `;
    for (const [key, entry] of consts) {
      if (!isConst(entry)) {
        continue;
      }
      code += `
  ${getType(entry, suffixNs)} ${key} = ${getValue(entry)};`;
    }

    code += `
`;

    for (const [key, entry] of enums) {
      if (!isEnum(entry)) {
        continue;
      }
      code += `
  enum ${key} { ${entry["enum"]?.join(", ")} };`;
    }

    code += `
}
`;

    return code;
  };
