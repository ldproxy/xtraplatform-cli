package de.ii.xtraplatform.cli.cmd;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.Result;
import java.util.Map;

public class Schemas extends Common<LdproxyCfg> {

  public Schemas(Map<String, Object> parameters) {
    super(parameters);
  }

  @Override
  public Result run(LdproxyCfg ldproxyCfg) {
    return Result.ok("schemas", ldproxyCfg.getRawSchemas());
  }
}
