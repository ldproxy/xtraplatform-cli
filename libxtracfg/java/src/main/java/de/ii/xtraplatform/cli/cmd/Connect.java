package de.ii.xtraplatform.cli.cmd;

import de.ii.ldproxy.cfg.Layout;
import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.Result;
import java.nio.file.Path;
import java.util.Map;

public class Connect extends Common<Context.Builder> {

  public final Path source;

  public Connect(Map<String, Object> parameters) {
    super(parameters);

    this.source = Path.of(string(parameters, "source", "."));
  }

  @Override
  public Result run(Context.Builder context) {
    try {
      context.ldproxyCfg = LdproxyCfg.create(source);
      context.layout = Layout.of(source);

      if (verbose) {
        return Result.ok(String.format("Store source: %s", context.layout.info().label()));
      }
    } catch (Throwable e) {
      if (debug && verbose) {
        e.printStackTrace();
      }
      return Result.failure(e.getMessage());
    }
    return Result.empty();
  }
}
