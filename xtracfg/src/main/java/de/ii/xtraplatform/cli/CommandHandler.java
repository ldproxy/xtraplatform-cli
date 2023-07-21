package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.Entities.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import shadow.com.fasterxml.jackson.core.JsonProcessingException;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;
import shadow.org.apache.http.NameValuePair;
import shadow.org.apache.http.client.utils.URLEncodedUtils;

public class CommandHandler {

  enum Command {
    connect,
    check,
    pre_upgrade,
    upgrade
  }

  private final ObjectMapper mapper;

  private LdproxyCfg ldproxyCfg;

  public CommandHandler(ObjectMapper mapper) {
    this.mapper = mapper;
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
    boolean ignoreRedundant = Objects.equals(parameters.getOrDefault("ignoreRedundant", "false"), "true");

    // System.out.println("J - COMMAND " + cmd + " " + parameters);

    Result result = new Result();

    switch (cmd) {
      case connect:
        result = connect(parameters);
        break;
      case check:
        result = Entities.check(ldproxyCfg, Type.Entity, ignoreRedundant);
        break;
      case pre_upgrade:
        result = Entities.preUpgrade(ldproxyCfg, Type.Entity, ignoreRedundant);
        break;
      case upgrade:
        boolean backup = Objects.equals(parameters.getOrDefault("backup", "false"), "true");
        result = Entities.upgrade(ldproxyCfg, Type.Entity, backup, ignoreRedundant, verbose);
        break;
    }

    try {
      // System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results));
      return mapper.writeValueAsString(result.asMap());
    } catch (JsonProcessingException e) {
      return String.format("{\"error\": %s}", e.getMessage());
    }
  }

  private Result connect(Map<String, String> parameters) {
    try {
      this.ldproxyCfg = new LdproxyCfg(Path.of(parameters.get("source")));
    } catch (Throwable e) {
      return Result.failure(e.getMessage());
    }

    return new Result();
  }

  private static Map<String, String> parseParameters(String query) {
    List<NameValuePair> parameters = URLEncodedUtils.parse(query, StandardCharsets.UTF_8);
    Map<String, String> params = new LinkedHashMap<>();

    for (NameValuePair nvp : parameters) {
      params.put(nvp.getName(), nvp.getValue());
    }

    return params;
  }
}
