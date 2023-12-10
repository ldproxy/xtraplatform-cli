package de.ii.xtraplatform.cli.cmd;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.AutoHandler;
import de.ii.xtraplatform.cli.Result;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class Auto extends Common<LdproxyCfg> {

  public final Optional<String> path;
  public final Consumer<Result> tracker;

  public Auto(
      Optional<String> subcommand, Map<String, Object> parameters, Consumer<Result> tracker) {
    super(parameters);

    this.path = optionalString(parameters, "path");
    this.tracker = tracker;
  }

  @Override
  public Result run(LdproxyCfg context) {
    // TODO
    return AutoHandler.handle(Map.of(), context, path, verbose, debug, tracker);
  }
}
