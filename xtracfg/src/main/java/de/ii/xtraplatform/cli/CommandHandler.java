package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.JacksonSubTypes;
import de.ii.ldproxy.cfg.Layout;
import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.JacksonProvider;
import de.ii.xtraplatform.cli.Entities.Type;
import de.ii.xtraplatform.store.app.ValueEncodingJackson;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.LongStream;

import shadow.com.fasterxml.jackson.core.JsonProcessingException;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;
import shadow.com.google.common.base.Strings;
import shadow.org.apache.http.NameValuePair;
import shadow.org.apache.http.client.utils.URLEncodedUtils;

public class CommandHandler {

  enum Command {
    connect,
    info,
    check,
    pre_upgrade,
    upgrade
  }

  private final Jackson jackson;
  private final ObjectMapper jsonMapper;

  private LdproxyCfg ldproxyCfg;
  private Layout layout;

  public CommandHandler() {
    this.jackson = new JacksonProvider(JacksonSubTypes::ids, false);
    this.jsonMapper =
        (new ValueEncodingJackson(jackson, false)).getMapper(ValueEncoding.FORMAT.JSON);
  }

  public String handle(String command) {
    URI uri;
    try {
      uri = new URI(command);
    } catch (URISyntaxException e) {
      return String.format("{\"error\": \"Could not parse command: %s\"}", e.getMessage());
    }

    Command cmd;
    String cmdString = uri.getPath().substring(1);
    try {
      cmd = Command.valueOf(cmdString);
    } catch (Throwable e) {
      return String.format("{\"error\": \"Unknown command: %s\"}", cmdString);
    }

    Map<String, String> parameters = parseParameters(uri.getQuery());

    Result result = handle(cmd, parameters);

    try {
      // System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results));
      return jsonMapper.writeValueAsString(result.asMap());
    } catch (JsonProcessingException e) {
      return String.format("{\"error\": \"Could not serialize result: %s\"}", e.getMessage());
    }
  }

  private Result handle(Command cmd, Map<String, String> parameters) {
    boolean verbose = Objects.equals(parameters.getOrDefault("verbose", "false"), "true");
    boolean ignoreRedundant =
        Objects.equals(parameters.getOrDefault("ignoreRedundant", "false"), "true");
    boolean onlyEntities = Objects.equals(parameters.getOrDefault("onlyEntities", "false"), "true");
    boolean onlyLayout = Objects.equals(parameters.getOrDefault("onlyLayout", "false"), "true");
    Optional<String> path = Optional.ofNullable(Strings.emptyToNull(parameters.get("path")));

    // System.out.println("J - COMMAND " + cmd + " " + parameters);

    if (cmd != Command.connect && (Objects.isNull(ldproxyCfg) || Objects.isNull(layout))) {
      return Result.failure("Not connected to store");
    }

    switch (cmd) {
      case connect:
        return connect(parameters);
      case info:
        return info();
      case check:
        return Entities.check(ldproxyCfg, Type.Entity, path, ignoreRedundant, verbose);
      case pre_upgrade:
        return Entities.preUpgrade(ldproxyCfg, Type.Entity, path, ignoreRedundant, verbose);
      case upgrade:
        boolean backup = Objects.equals(parameters.getOrDefault("backup", "false"), "true");
        return Entities.upgrade(ldproxyCfg, Type.Entity, path, backup, ignoreRedundant, verbose);
      default:
        return Result.failure("Unknown command: " + cmd);
    }
  }

  private Result connect(Map<String, String> parameters) {
    try {
      this.ldproxyCfg = new LdproxyCfg(Path.of(parameters.get("source")), true);
      ldproxyCfg.init();
      this.layout = Layout.of(Path.of(parameters.get("source")));
    } catch (Throwable e) {
      return Result.failure(e.getMessage());
    }

    return new Result();
  }

  private Result info() {
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

      Map<String, Long> resources = info.resources();
      long all2 = resources.values().stream().mapToLong(l -> l).sum();
      result.info("Resources: " + all2);

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

  private static Map<String, String> parseParameters(String query) {
    List<NameValuePair> parameters = URLEncodedUtils.parse(query, StandardCharsets.UTF_8);
    Map<String, String> params = new LinkedHashMap<>();

    for (NameValuePair nvp : parameters) {
      if (params.containsKey(nvp.getName())) {
        params.put(nvp.getName(), params.get(nvp.getName()) + "|" + nvp.getValue());
      } else {
        params.put(nvp.getName(), nvp.getValue());
      }
    }

    return params;
  }
}
