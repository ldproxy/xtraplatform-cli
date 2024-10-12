package de.ii.xtraplatform.cli.cmd;

import de.ii.ldproxy.cfg.Layout;
import de.ii.xtraplatform.cli.Result;
import java.io.IOException;
import java.util.Map;

public class Info extends Common<Layout> {

  public Info(Map<String, Object> parameters) {
    super(parameters);
  }

  @Override
  public Result run(Layout layout) {
    try {
      Layout.Info info = layout.info();
      Result result = new Result();
      result.info("Source: " + info.label());
      result.info("Size: " + info.size());

      Map<String, Long> entities = info.entities();
      long all = entities.values().stream().mapToLong(l -> l).sum();
      result.info("Entities: " + all);
      for (Map.Entry<String, Long> entry : entities.entrySet()) {
        String type = entry.getKey();
        Long number = entry.getValue();
        result.info("  " + type + ": " + number);
      }

      Map<String, Long> values = info.values();
      long all2 = values.values().stream().mapToLong(l -> l).sum();
      result.info("Values: " + all2);
      for (Map.Entry<String, Long> entry : values.entrySet()) {
        String type = entry.getKey();
        Long number = entry.getValue();
        result.info("  " + type + ": " + number);
      }

      Map<String, Long> resources = info.resources();
      long all3 = resources.values().stream().mapToLong(l -> l).sum();
      result.info("Resources: " + all3);

      for (Map.Entry<String, Long> entry : resources.entrySet()) {
        String type = entry.getKey();
        Long number = entry.getValue();
        result.info("  " + type + ": " + number);
      }

      return result;
    } catch (IOException e) {
      return Result.failure("Could not access store source: " + e.getMessage());
    }
  }
}
