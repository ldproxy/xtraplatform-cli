package de.ii.xtraplatform.cli;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import shadow.com.google.common.base.Strings;

public class Call {

  public enum Command {
    connect,
    info,
    check,
    pre_upgrade,
    upgrade,
    auto,
    schemas,
    file_type,
    unknown,
    autoValue
  }

  final Command command;

  final Optional<String> subcommand;
  final Map<String, Object> parameters;

  private Call(Command command, Optional<String> subcommand, Map<String, Object> parameters) {
    this.command = command;
    this.subcommand = subcommand;
    this.parameters = parameters;
  }

  public Call with(Command newCommand) {
    return new Call(newCommand, subcommand, parameters);
  }

  public static Call parse(Map<String, Object> call) {
    Command command = Command.unknown;
    Optional<String> subcommand = Optional.empty();
    Map<String, Object> parameters = new LinkedHashMap<>();

    for (Map.Entry<String, Object> entry : call.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (Objects.equals(key.toLowerCase(), "command")) {
        command = Command.valueOf(string(value));
      } else if (Objects.equals(key.toLowerCase(), "subcommand")) {
        subcommand = Optional.ofNullable(string(value));
      } else {
        parameters.put(key, value);
      }
    }

    return new Call(command, subcommand, parameters);
  }

  private static String string(Object value) {
    if (!(value instanceof String)) {
      return null;
    }
    return Strings.emptyToNull((String) value);
  }
}
