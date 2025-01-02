import { Definition } from "typescript-json-schema";
import { Result } from "./generate.ts";
import { Generator } from "./common/index.ts";
import { generateClass, generateJava } from "./java/index.ts";

const dataNs = ["Command", "Options", "Result"];
const suffixNs = ["Command", "Options"];
const commandNs = "Command";
const baseOptions = "BaseOptions";
const baseResult = "BaseResult";
const failureResult = "FailureResult";

export const generateJavaClasses = (
  schema: Definition,
  pkg: string
): Result => {
  const commands: string[] = [];
  let initCommand: string | undefined;
  let baseCommand: string | undefined;

  const onNs = (
    ns: string,
    nsInterface?: string,
    nsInterfaceDef?: Definition
  ) => {
    if (ns === commandNs) {
      baseCommand = nsInterface;
    }
  };

  const onClass = (ns: string, name: string, def?: any) => {
    if (ns === commandNs) {
      if (def?.javaContextInit) {
        initCommand = name;
      } else {
        commands.push(name);
      }
    }
  };

  const result = generateJava(
    "Java Backend",
    schema,
    pkg,
    dataNs,
    suffixNs,
    onNs,
    onClass
  );

  if (!initCommand) {
    throw new Error("No init command found");
  }
  if (!baseCommand) {
    throw new Error("No base command found");
  }

  result.files.push(
    generateClass(
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

  return result;
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

      ${result} result = handle(new ${initCommand}((${baseOptions})command.options()), false, ignore -> {});

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
    return new ${failure}("Unknown command: " + command.command());
  }
}
    `;

    return code;
  };
