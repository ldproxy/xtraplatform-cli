package de.ii.xtraplatform.cli.cmd;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.AutoHandler;
import de.ii.xtraplatform.cli.Result;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class Auto extends Common<LdproxyCfg> {

  enum Subcommand {
    check,
    analyze,
    generate
  }

  public final Subcommand subcommand;
  public final Optional<String> path;
  public final Map<String, String> parameters;
  public final Map<String, List<String>> types;
  public final Consumer<Result> tracker;

  public Auto(
      Optional<String> subcommand, Map<String, Object> parameters, Consumer<Result> tracker) {
    super(parameters);

    this.subcommand = requiredSubcommand(subcommand, Subcommand::valueOf);
    this.path = optionalString(parameters, "path");
    this.parameters =
        stringMap(
            parameters, "id", "featureProviderType", "host", "database", "user", "password", "url", "selectedConfig", "createOption");
    if (parameters.containsKey("typeObject") && parameters.get("typeObject") instanceof Map) {
      this.parameters.put("typeObject", parameters.get("typeObject").toString());
    }
    this.types = parseTypes(parameters);
    this.tracker = tracker;
  }

  private Map<String, List<String>> parseTypes(Map<String, Object> parameters) {
    if (parameters.containsKey("types") && parameters.get("types") instanceof Map) {
      try {
        return (Map<String, List<String>>) parameters.get("types");
      } catch (ClassCastException e) {
        // continue
      }
    }
    return Map.of();
  }

  @Override
  public Result run(LdproxyCfg ldproxyCfg) {
    Result result = AutoHandler.preCheck(parameters, ldproxyCfg);

    if (result.isFailure()) {
      return result;
    }

    try {
      switch (subcommand) {
        case check:
          return AutoHandler.check(parameters, ldproxyCfg, path, verbose, debug);
        case analyze:
          return AutoHandler.analyze(parameters, ldproxyCfg, path, verbose, debug);
        case generate:
          return AutoHandler.generate(parameters, ldproxyCfg, path, verbose, debug, types, tracker);
        default:
          throw new IllegalStateException("Unexpected subcommand: " + subcommand);
      }
    } catch (Throwable e) {
      // e.printStackTrace();
      return Result.failure("Unexpected error: " + e.getMessage());
    }
  }
}
