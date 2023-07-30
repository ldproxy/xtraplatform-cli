package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.JacksonSubTypes;
import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.JacksonProvider;
import de.ii.xtraplatform.cli.Entities.Type;
import de.ii.xtraplatform.store.app.ValueEncodingJackson;
import de.ii.xtraplatform.store.domain.ValueEncoding;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import shadow.com.fasterxml.jackson.core.JsonProcessingException;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;
import shadow.com.google.common.base.Strings;
import shadow.org.apache.http.NameValuePair;
import shadow.org.apache.http.client.utils.URLEncodedUtils;

public class CommandHandler {

  enum Command {
    connect,
    check,
    pre_upgrade,
    upgrade
  }

  private final Jackson jackson;
  private final ObjectMapper jsonMapper;

  private LdproxyCfg ldproxyCfg;

  public CommandHandler() {
    this.jackson = new JacksonProvider(JacksonSubTypes::ids, false);
    this.jsonMapper =
        (new ValueEncodingJackson(jackson, false)).getMapper(ValueEncoding.FORMAT.JSON);
  }

  public String handleCommand(String command) {
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
    boolean verbose = Objects.equals(parameters.getOrDefault("verbose", "false"), "true");
    boolean ignoreRedundant =
        Objects.equals(parameters.getOrDefault("ignoreRedundant", "false"), "true");
    boolean onlyEntities = Objects.equals(parameters.getOrDefault("onlyEntities", "false"), "true");
    boolean onlyLayout = Objects.equals(parameters.getOrDefault("onlyLayout", "false"), "true");
    Optional<String> path = Optional.ofNullable(Strings.emptyToNull(parameters.get("path")));

    // System.out.println("J - COMMAND " + cmd + " " + parameters);

    Result result = new Result();

    switch (cmd) {
      case connect:
        result = connect(parameters);
        break;
      case check:
        result = Entities.check(ldproxyCfg, Type.Entity, path, ignoreRedundant, verbose);
        break;
      case pre_upgrade:
        result = Entities.preUpgrade(ldproxyCfg, Type.Entity, path, ignoreRedundant, verbose);
        break;
      case upgrade:
        boolean backup = Objects.equals(parameters.getOrDefault("backup", "false"), "true");
        result = Entities.upgrade(ldproxyCfg, Type.Entity, path, backup, ignoreRedundant, verbose);
        break;
    }

    try {
      // System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results));
      return jsonMapper.writeValueAsString(result.asMap());
    } catch (JsonProcessingException e) {
      return String.format("{\"error\": %s}", e.getMessage());
    }
  }

  private Result connect(Map<String, String> parameters) {
    try {
      this.ldproxyCfg = new LdproxyCfg(Path.of(parameters.get("source")), true);
      ldproxyCfg.init();
    } catch (Throwable e) {
      return Result.failure(e.getMessage());
    }

    return new Result();
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
