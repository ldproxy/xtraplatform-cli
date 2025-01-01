import { Definition, DefinitionOrBoolean } from "typescript-json-schema";
import { Result } from "./generate.ts";

type Defs = [string, Definition][];
type Generator = (name: string, pkg: string) => string;

const defs = (entry: DefinitionOrBoolean): Defs => {
  return typeof entry !== "boolean" ? Object.entries(entry as Definition) : [];
};

const generate = (name: string, pkg: string, generate: Generator) => {
  const dir = pkg.replaceAll(".", "/");
  const code = generate(name, pkg);

  return { path: `${dir}/${name}.java`, content: code };
};

const dataNs = ["Command", "Options", "Result"];
const suffixNs = ["Command", "Options"];
const commandNs = "Command";
const baseResult = "BaseResult";
const failureResult = "FailureResult";

const getName = (name: string, ns: string) => {
  return suffixNs.includes(ns) && name !== ns ? name + ns : name;
};

export const generateJavaClasses = (schema: Definition, pkg: string) => {
  const definitions = schema.definitions || {};
  let consts: Defs = [];
  let enums: Defs = [];
  const commands: string[] = [];
  let initCommand: string | undefined;
  let baseCommand: string | undefined;
  const result: Result = { name: "Java Backend", files: [] };

  for (const [ns, entries] of Object.entries(definitions)) {
    if (ns === "Consts") {
      consts = defs(entries);
      continue;
    }
    if (ns === "Enums") {
      enums = defs(entries);
      continue;
    }
    let nsInterface: string | undefined;
    let nsInterfaceDef: Definition | undefined;
    let nsDiscriminator: string | undefined;
    let nsDiscriminators: { [key: string]: string } = {};

    for (const [key, def] of Object.entries(entries)) {
      if (dataNs.includes(ns) && def && def.interface) {
        nsInterface = getName(key, ns);
        nsInterfaceDef = def as Definition;
        if (def.discriminator) {
          nsDiscriminator = def.discriminator as string;
        }
        if (ns === commandNs) {
          baseCommand = getName(key, ns);
        }
      }
    }
    for (const [key, def] of Object.entries(entries)) {
      if (dataNs.includes(ns) && def && !def.interface) {
        if (
          nsDiscriminator &&
          def.properties &&
          def.properties[nsDiscriminator]
        ) {
          nsDiscriminators[getName(key, ns)] = getValue(
            def.properties[nsDiscriminator],
            true
          );
        }
        if (ns === commandNs) {
          if (def.javaContextInit) {
            initCommand = getName(key, ns);
          } else {
            commands.push(getName(key, ns));
          }
        }
        result.files.push(
          generate(getName(key, ns), pkg, generateDataClass(def, nsInterface))
        );
      }
    }
    if (nsInterface) {
      result.files.push(
        generate(
          nsInterface,
          pkg,
          generateInterface(
            nsInterfaceDef as Definition,
            nsDiscriminators,
            nsDiscriminator
          )
        )
      );
    }
    if (ns === commandNs) {
      if (!initCommand) {
        throw new Error("No init command found");
      }
      if (!baseCommand) {
        throw new Error("No base command found");
      }
      result.files.push(
        generate(
          "Handler",
          pkg,
          generateHandler(
            commands,
            initCommand,
            baseCommand,
            baseResult,
            failureResult
          )
        )
      );
    }
  }

  result.files.push(
    generate("Identifiers", pkg, generateIdentifiersClass(consts, enums))
  );

  return result;
};

const getType = (schema: DefinitionOrBoolean): string => {
  if (typeof schema === "boolean") {
    return "boolean";
  }
  if (schema.type) {
    if (schema.type === "object" && !schema.properties) {
      return "java.util.Map<String, Object>";
    }
    if (schema.type === "array" && schema.items) {
      return `java.util.List<${getType(schema.items as DefinitionOrBoolean)}>`;
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
    if (schema.$ref.includes("/Consts/")) {
      return "String";
    }
    let type = schema.$ref.substring(schema.$ref.lastIndexOf("/") + 1);
    if (type.includes(".")) {
      type = type.substring(0, type.lastIndexOf("."));
    }
    const prefix = schema.$ref.includes("/Enums/") ? "Identifiers." : "";
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

const getValue = (
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
    (schema.$ref.includes("/Consts/") || schema.$ref.includes("/Enums/"))
  ) {
    const name =
      stringNotEnum && schema.$ref.includes(".")
        ? schema.$ref.substring(schema.$ref.lastIndexOf(".") + 1)
        : schema.$ref.substring(schema.$ref.lastIndexOf("/") + 1);
    return `Identifiers.${name}`;
  }
  return "";
};

const getDefault = (schema: DefinitionOrBoolean): string => {
  if (typeof schema === "object" && Object.hasOwn(schema, "default")) {
    if (schema.type === "string") {
      return `"${schema.default}"`;
    }
    return `${schema.default}`;
  }
  return "";
};

const isConst = (schema: DefinitionOrBoolean) => {
  return (
    typeof schema === "object" &&
    (Object.hasOwn(schema, "const") ||
      Object.hasOwn(schema, "hide") ||
      (Object.hasOwn(schema, "$ref") &&
        schema.$ref &&
        (schema.$ref.includes("/Consts/") ||
          (schema.$ref.includes("/Enums/") && schema.$ref.includes(".")))))
  );
};

const isEnum = (schema: DefinitionOrBoolean) => {
  return (
    typeof schema === "object" &&
    (Object.hasOwn(schema, "enum") ||
      (Object.hasOwn(schema, "$ref") &&
        schema.$ref &&
        schema.$ref.includes("/Enums/")))
  );
};

const firstLetterUpperCase = (str: string) => {
  return str.charAt(0).toUpperCase() + str.slice(1);
};

const generateDataClass =
  (schema: Definition, intface?: string): Generator =>
  (name: string, pkg: string): string => {
    const properties = schema.properties || {};

    let code = `
package ${pkg};

public class ${name}${intface ? ` implements ${intface}` : ""} {
    `;
    for (const [key, entry] of Object.entries(properties)) {
      code += `
  private final ${getType(entry)} ${key};`;
    }

    code += `

  public ${name}(`;
    let i = 0;
    for (const [key, entry] of Object.entries(properties)) {
      i++;
      if (isConst(entry)) {
        continue;
      }
      const comma = i <= Object.keys(properties).length - 1 ? "," : "";
      code += `
    ${getType(entry)} ${key}${comma}`;
    }

    code += `
  ) {`;
    for (const [key, entry] of Object.entries(properties)) {
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
    for (const [key, entry] of Object.entries(properties)) {
      code += `
  public ${getType(entry)} get${firstLetterUpperCase(key)}() {
    return ${key};
  }
    `;
    }

    code += `
}

    `;

    return code;
  };

const generateIdentifiersClass =
  (consts: Defs, enums: Defs): Generator =>
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
    ${getType(entry)} ${key} = ${getValue(entry)};`;
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

const generateInterface =
  (
    schema: Definition,
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
  ${getType(entry)} get${firstLetterUpperCase(key)}();
    `;
    }

    code += `
}
    `;

    return code;
  };

const generateHandler =
  (
    commands: string[],
    initCommand: string,
    baseCommand: string,
    result: string,
    failure: string
  ): Generator =>
  (name: string, pkg: string): string => {
    const initName = initCommand.replace("Command", "");
    let code = `
package ${pkg};

import java.util.Objects;
import java.util.function.Consumer;

public abstract class Handler<T extends Handler.Context, U extends Handler.ContextBuilder<T>> {

  public interface Context {
    boolean isConnected();
  }

  public interface ContextBuilder<T extends Context> {
    T build();
  }

  private T context;

  public Handler() {
    this.context = null;
  }

  private boolean isConnected() {
    return Objects.nonNull(context) && context.isConnected();
  }

  protected ${result} notConnected() {
    return new ${failure}("Not connected to store");
  }

  protected abstract U createContextBuilder();

  protected abstract ${result} run${initName}(${initCommand} command, U builder, Consumer<${result}> tracker);
    `;
    for (const command of commands) {
      const commandName = command.replace("Command", "");
      code += `
  protected abstract ${result} run${commandName}(${command} command, T context, Consumer<${result}> tracker);
  `;
    }

    code += `
  public final ${result} handle(${baseCommand} command, boolean autoConnect, Consumer<${result}> tracker) {
    if ((!(command instanceof ${initCommand})) && !isConnected()) {
      if (!autoConnect) {
        return notConnected();
      }

      ${result} result = handle(new ${initCommand}(command.getOptions()), false, ignore -> {});

      if (result instanceof ${failure}) {
        return result;
      }
    }

    if (command instanceof ${initCommand}) {
      U builder = createContextBuilder();
      ${result} result = run${initName}(((${initCommand}) command), builder, tracker);

      context = builder.build();

      return result;
    }
    `;
    for (const command of commands) {
      const commandName = command.replace("Command", "");
      code += `
    if (command instanceof ${command}) {
      return run${commandName}(((${command}) command), context, tracker);
    }
      `;
    }

    code += `
    return new ${failure}("Unknown command: " + command.getCommand());
  }
}
    `;

    return code;
  };
