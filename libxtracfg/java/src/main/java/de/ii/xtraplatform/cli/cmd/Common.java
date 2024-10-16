package de.ii.xtraplatform.cli.cmd;

import de.ii.xtraplatform.cli.Result;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import shadow.com.google.common.base.Strings;

public abstract class Common<T> {
  public final boolean verbose;
  public final boolean debug;

  protected Common(Common<T> common) {
    this.verbose = common.verbose;
    this.debug = common.debug;
  }

  protected Common(Map<String, Object> parameters) {
    this.verbose = flag(parameters, "verbose");
    this.debug = flag(parameters, "debug");

    if (verbose) {
      System.out.println("Running " + this.getClass().getSimpleName() + " with parameters: " + parameters);
    }
  }

  protected final boolean flag(Map<String, Object> parameters, String name) {
    Object value = parameters.getOrDefault(name, "false");

    return Objects.equals(value, "true") || Objects.equals(value, true);
  }

  protected String string(Map<String, Object> parameters, String name) {
    Object value = parameters.getOrDefault(name, "");

    if (!(value instanceof String)) {
      return null;
    }

    return Strings.emptyToNull((String) value);
  }

  protected String string(Map<String, Object> parameters, String name, String defaultValue) {
    return Objects.requireNonNullElse(string(parameters, name), defaultValue);
  }

  protected Optional<String> optionalString(Map<String, Object> parameters, String name) {
    return Optional.ofNullable(string(parameters, name));
  }

  protected Map<String, String> stringMap(Map<String, Object> parameters, String... names) {
    Map<String, String> stringMap = new LinkedHashMap<>();

    for (String name : names) {
      stringMap.put(name, string(parameters, name, ""));
    }

    return stringMap;
  }

  protected final <T extends Enum<T>> Optional<T> subcommand(
      Optional<String> subcommand, Function<String, T> valueOf) {
    if (subcommand.isPresent()) {
      return Optional.ofNullable(valueOf.apply(subcommand.get()));
    }

    return Optional.empty();
  }

  protected final <T extends Enum<T>> T requiredSubcommand(
      Optional<String> subcommand, Function<String, T> valueOf) {
    return subcommand(subcommand, valueOf)
        .orElseThrow(() -> new IllegalArgumentException("No subcommand given"));
  }

  public abstract Result run(T context);
}
