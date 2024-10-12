package de.ii.xtraplatform.cli.cmd;

import de.ii.ldproxy.cfg.Layout;
import de.ii.ldproxy.cfg.LdproxyCfg;

public class Context {
  public final LdproxyCfg ldproxyCfg;
  public final Layout layout;

  private Context(LdproxyCfg ldproxyCfg, Layout layout) {
    this.ldproxyCfg = ldproxyCfg;
    this.layout = layout;
  }

  public static class Builder {
    public LdproxyCfg ldproxyCfg;
    public Layout layout;

    public Context build() {
      return new Context(ldproxyCfg, layout);
    }
  }
}
