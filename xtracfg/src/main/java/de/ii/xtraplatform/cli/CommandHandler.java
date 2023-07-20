package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import shadow.com.fasterxml.jackson.core.JsonProcessingException;
import shadow.com.fasterxml.jackson.core.type.TypeReference;
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

    Map<String, String> parameters =
        toMap(URLEncodedUtils.parse(uri.getQuery(), StandardCharsets.UTF_8));

    //System.out.println("J - COMMAND " + cmd + " " + parameters);

    Map<String, Object> results = Map.of();

    switch (cmd) {
      case connect:
        results = connect(parameters);
        break;
      case check:
        results = check(parameters);
        break;
      case pre_upgrade:
        results = preUpgrade(parameters);
        break;
      case upgrade:
        results = upgrade(parameters);
        break;
    }

    try {
      // System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results));
      return mapper.writeValueAsString(results);
    } catch (JsonProcessingException e) {
      return String.format("{\"error\": %s}", e.getMessage());
    }
  }

  private Map<String, Object> connect(Map<String, String> parameters) {
    try {
      this.ldproxyCfg = new LdproxyCfg(Path.of(parameters.get("source")));
    } catch (Throwable e) {
      return Map.of("error", e.getMessage());
    }

    return Map.of();
  }

  static TypeReference<LinkedHashMap<String, Object>> typeRef =
      new TypeReference<LinkedHashMap<String, Object>>() {};

  private Map<String, Object> check(Map<String, String> parameters) {
    if (Objects.isNull(ldproxyCfg)) {
      return Map.of("error", "Not connected to store");
    }

    List<Map<String, String>> results = new ArrayList<>();

    getEntityUpgrades()
        .forEach(
            upgrade -> {
              if (upgrade.getUpgrade().isPresent()) {
                results.add(
                    Map.of(
                        "status",
                        "WARNING",
                        "message",
                        "Entity configuration has deprecated or redundant settings: "
                            + ldproxyCfg.getDataDirectory().relativize(upgrade.getPath())));
              }

              if (upgrade.getError().isPresent()) {
                results.add(
                    Map.of(
                        "status",
                        "ERROR",
                        "message",
                        String.format(
                            "Could not read %s: %s", upgrade.getPath(), upgrade.getError().get())));
              }
            });

    if (results.isEmpty()) {
      results.add(Map.of("status", "SUCCESS", "message", "Everything is fine"));
    }

    return Map.of("results", results);
  }

  private Map<String, Object> preUpgrade(Map<String, String> parameters) {
    if (Objects.isNull(ldproxyCfg)) {
      return Map.of("error", "Not connected to store");
    }

    List<Map<String, String>> results = new ArrayList<>();

    getEntityUpgrades()
            .forEach(
                    upgrade -> {
                      if (upgrade.getUpgrade().isPresent()) {
                        if (results.isEmpty()) {
                          results.add(Map.of("status", "INFO", "message", "The following entity configurations will be upgraded:"));
                        }
                        results.add(
                                Map.of(
                                        "status",
                                        "INFO",
                                        "message",
                                        "- "
                                                + ldproxyCfg.getDataDirectory().relativize(upgrade.getPath())));
                      }

                      if (upgrade.getError().isPresent()) {
                        results.add(
                                Map.of(
                                        "status",
                                        "ERROR",
                                        "message",
                                        String.format(
                                                "Could not read %s: %s", upgrade.getPath(), upgrade.getError().get())));
                      }
                    });

    if (results.isEmpty()) {
      results.add(Map.of("status", "SUCCESS", "message", "Nothing to do"));
    } else if (results.stream().anyMatch(r -> Objects.equals(r.get("status"), "INFO"))) {
      results.add(Map.of("status", "CONFIRMATION", "message", "Are you sure?"));
    }

    return Map.of("results", results);
  }

  private Map<String, Object> upgrade(Map<String, String> parameters) {
    if (Objects.isNull(ldproxyCfg)) {
      return Map.of("error", "Not connected to store");
    }

    List<Map<String, String>> results = new ArrayList<>();

    getEntityUpgrades()
        .forEach(
            upgrade -> {
              if (upgrade.getUpgrade().isPresent()) {
                boolean error = false;
                if (Objects.equals(parameters.getOrDefault("backup", "true"), "true")) {
                  Path backup =
                      upgrade
                          .getPath()
                          .getParent()
                          .resolve(upgrade.getPath().getFileName().toString() + ".backup");
                  try {
                    ldproxyCfg.getObjectMapper().writeValue(backup.toFile(), upgrade.getOriginal());

                    if (Objects.equals(parameters.getOrDefault("verbose", "false"), "true")) {
                      results.add(
                          Map.of(
                              "status",
                              "SUCCESS",
                              "message",
                              "Entity configuration backup created: "
                                  + ldproxyCfg.getDataDirectory().relativize(backup)));
                    }
                  } catch (IOException e) {
                    error = true;
                    results.add(
                        Map.of(
                            "status",
                            "ERROR",
                            "message",
                            String.format(
                                "Could not create backup %s: %s", backup, e.getMessage())));
                  }
                }
                if (!error) {
                  try {
                    // TODO: change lastModified???
                    ldproxyCfg
                        .getObjectMapper()
                        .writeValue(upgrade.getPath().toFile(), upgrade.getUpgrade().get());
                  } catch (IOException e) {
                    error = true;
                    results.add(
                        Map.of(
                            "status",
                            "ERROR",
                            "message",
                            String.format(
                                "Could not upgrade %s: %s", upgrade.getPath(), e.getMessage())));
                  }
                }
                if (!error) {
                  results.add(
                      Map.of(
                          "status",
                          "SUCCESS",
                          "message",
                          "Entity configuration upgraded: "
                              + ldproxyCfg.getDataDirectory().relativize(upgrade.getPath())));
                }
              }

              if (upgrade.getError().isPresent()) {
                results.add(
                    Map.of(
                        "status",
                        "ERROR",
                        "message",
                        String.format(
                            "Could not read %s: %s", upgrade.getPath(), upgrade.getError().get())));
              }
            });

    if (results.isEmpty()) {
      results.add(Map.of("status", "SUCCESS", "message", "Nothing to do"));
    }

    return Map.of("results", results);
  }

  private List<Upgrade> getEntityUpgrades() {
    Path store = ldproxyCfg.getDataDirectory().resolve("store");
    Path entities = store.resolve("entities");

    // TODO: optionally compare ordering of elements
    return ldproxyCfg.getEntityDataStore().identifiers().stream()
        .map(
            identifier -> {
              Path base = entities.resolve(identifier.asPath());
              Path yml = base.getParent().resolve(base.getFileName().toString() + ".yml");

              try {
                return getUpgrade(yml, identifier)
                    .map(up -> new Upgrade(yml, up.first(), up.second()));
              } catch (IOException e) {
                return Optional.of(new Upgrade(yml, e.getMessage()));
              }
            })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private Optional<Tuple<Map<String, Object>, Map<String, Object>>> getUpgrade(
      Path yml, Identifier identifier) throws IOException {
    LinkedHashMap<String, Object> original =
        ldproxyCfg.getObjectMapper().readValue(yml.toFile(), typeRef);
    EntityData entityData = ldproxyCfg.getEntityDataStore().get(identifier);
    Map<String, Object> upgraded = ldproxyCfg.getEntityDataStore().asMap(identifier, entityData);

    if (!original.containsKey("createdAt")) {
      upgraded.remove("createdAt");
    }
    if (!original.containsKey("lastModified")) {
      upgraded.remove("lastModified");
    }
    if (!original.containsKey("entityStorageVersion")) {
      upgraded.remove("entityStorageVersion");
    }

    if (!Objects.equals(original, upgraded)) {
      return Optional.of(Tuple.of(original, upgraded));
    }

    return Optional.empty();
  }

  private Map<String, String> toMap(List<NameValuePair> parameters) {
    Map<String, String> params = new LinkedHashMap<>();

    for (NameValuePair nvp : parameters) {
      params.put(nvp.getName(), nvp.getValue());
    }

    return params;
  }
}
