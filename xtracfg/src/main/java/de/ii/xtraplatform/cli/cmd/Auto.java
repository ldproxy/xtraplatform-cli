package de.ii.xtraplatform.cli.cmd;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.AutoHandler;
import de.ii.xtraplatform.cli.Result;
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
  public final Consumer<Result> tracker;

  public Auto(
      Optional<String> subcommand, Map<String, Object> parameters, Consumer<Result> tracker) {
    super(parameters);

    this.subcommand = requiredSubcommand(subcommand, Subcommand::valueOf);
    this.path = optionalString(parameters, "path");
    this.parameters =
        stringMap(
            parameters,
            "id",
            "types",
            "featureProviderType",
            "host",
            "database",
            "user",
            "password",
            "url");
    this.tracker = tracker;
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
          return AutoHandler.generate(parameters, ldproxyCfg, path, verbose, debug, tracker);
        default:
          throw new IllegalStateException("Unexpected subcommand: " + subcommand);
      }
    } catch (Throwable e) {
      // e.printStackTrace();
      return Result.failure("Unexpected error: " + e.getMessage());
    }
  }
}
