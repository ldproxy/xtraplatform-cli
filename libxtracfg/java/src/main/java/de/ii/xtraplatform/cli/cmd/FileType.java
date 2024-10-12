package de.ii.xtraplatform.cli.cmd;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.Result;
import de.ii.xtraplatform.entities.domain.EntityFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import shadow.com.google.common.io.Files;

public class FileType extends Common<LdproxyCfg> {

  private static final List<String> CONTENT_TYPES =
      List.of("defaults", "entities", "instances", "overrides");
  private static final List<String> ENTITY_TYPES = List.of("providers", "services");

  public static final class FileInfo {
    public final String entityType;
    public final Optional<String> entitySubType;
    public final Optional<String> subProperty;
    public final Optional<String> discriminatorKey;
    public final Optional<String> discriminatorValue;

    public FileInfo(
        String entityType,
        String entitySubType,
        String subProperty,
        String discriminatorKey,
        String discriminatorValue) {
      this.entityType = entityType;
      this.entitySubType = Optional.ofNullable(entitySubType);
      this.subProperty = Optional.ofNullable(subProperty);
      this.discriminatorKey = Optional.ofNullable(discriminatorKey);
      this.discriminatorValue = Optional.ofNullable(discriminatorValue);
    }

    public boolean isValid() {
      return entityType != null;
    }
  }

  public final String fullPathString;
  public final Path fullPath;

  public final Path path;
  public final String fileName;
  public final String fileExtension;

  public FileType(Map<String, Object> parameters) {
    super(parameters);
    this.fullPathString = string(parameters, "path");
    this.fullPath = Path.of(fullPathString);

    String file = Optional.ofNullable(fullPath.getFileName()).map(Path::toString).orElse("");

    this.path = fullPath.getParent();
    this.fileName = Files.getNameWithoutExtension(file);
    this.fileExtension = Files.getFileExtension(file);
  }

  public FileInfo get(LdproxyCfg ldproxyCfg) {
    Result result = run(ldproxyCfg);
    Map<String, String> details =
        (Map<String, String>) result.asMap().getOrDefault("details", Map.of());

    return new FileInfo(
        details.getOrDefault("entityType", null),
        details.getOrDefault("entitySubType", null),
        details.getOrDefault("subProperty", null),
        details.getOrDefault("discriminatorKey", null),
        details.getOrDefault("discriminatorValue", null));
  }

  @Override
  public Result run(LdproxyCfg ldproxyCfg) {

    if (Objects.isNull(path)
        || path.getNameCount() < 2
        || !Objects.equals(fileExtension, "yml")
        || (!path.startsWith("entities") && !path.startsWith("store"))) {
      return Result.empty();
    }

    String type = path.getName(1).toString();

    // TODO: multi-file overrides
    if (path.getNameCount() >= 3
        && CONTENT_TYPES.contains(type)
        && ENTITY_TYPES.contains(path.getFileName().toString())) {

      if (Objects.equals(type, "overrides")) {
        String entityType = path.getFileName().toString();
        Path entityPath =
            ldproxyCfg.getDataDirectory().resolve(fullPathString.replace("overrides", "entities"));

        if (entityPath.toFile().exists()) {
          try {
            Optional<String> subTypeLine =
                Files.readLines(entityPath.toFile(), StandardCharsets.UTF_8).stream()
                    .filter(
                        line ->
                            line.startsWith(
                                entityType.substring(0, entityType.length() - 1) + "Type:"))
                    .findFirst();

            if (subTypeLine.isPresent()) {
              String subType =
                  subTypeLine.get().substring(subTypeLine.get().indexOf(':') + 1).trim();

              return found(entityType, subType);
            }
          } catch (IOException e) {
            // ignore
          }
        }

        return Result.empty();
      }

      return found(path.getFileName().toString());
    }

    if (Objects.equals(type, "defaults")) {
      if (ENTITY_TYPES.contains(fileName)) {
        String subType = "services".equals(fileName) ? "OGC_API" : "FEATURE";
        return found(fileName, subType);
      } else if (ENTITY_TYPES.stream().anyMatch(et -> fileName.startsWith(et + "."))) {
        return found(
            fileName.substring(0, fileName.indexOf('.')),
            fileName.substring(fileName.indexOf('.') + 1));
      }

      if (path.getNameCount() >= 4 && ENTITY_TYPES.contains(path.getName(2).toString())) {
        try {
          EntityFactory entityFactory =
              ldproxyCfg
                  .getEntityFactories()
                  .get(path.getName(2).toString(), path.getName(3).toString());

          Optional<Set<Map.Entry<String, Object>>> keyPathAlias =
              entityFactory
                  .getKeyPathAlias(fileName)
                  .map(keyPathAlias1 -> keyPathAlias1.wrapMap(Map.of()).entrySet());

          if (keyPathAlias.isPresent() && !keyPathAlias.get().isEmpty()) {
            Map.Entry<String, Object> next = keyPathAlias.get().iterator().next();
            String property = next.getKey();
            String discriminatorKey = null;
            String discriminatorValue = null;
            if (next.getValue() instanceof List
                && !((List<?>) next.getValue()).isEmpty()
                && ((List<?>) next.getValue()).get(0) instanceof Map) {
              Map.Entry<String, Object> disc =
                  ((Map<String, Object>) ((List<?>) next.getValue()).get(0))
                      .entrySet()
                      .iterator()
                      .next();
              discriminatorKey = disc.getKey();
              if (disc.getValue() instanceof String) {
                discriminatorValue = (String) disc.getValue();
              }
            }

            return found(
                path.getName(2).toString(),
                path.getName(3).toString(),
                property,
                discriminatorKey,
                discriminatorValue);
          } else if (keyPathAlias.isEmpty()) {
            return found(
                path.getName(2).toString(), path.getName(3).toString(), fileName, null, null);
          }
        } catch (Throwable e) {
          System.out.println("ERR " + e.getMessage());
        }
      }
    }

    return Result.empty();
  }

  private Result found(String entityType) {
    return Result.ok(
        "found",
        Map.of(
            "path",
            fullPathString,
            "fileType",
            "entities/" + entityType,
            "entityType",
            entityType));
  }

  private Result found(String entityType, String entitySubType) {
    return Result.ok(
        "found",
        Map.of(
            "path",
            fullPathString,
            "fileType",
            "entities/" + entityType,
            "entityType",
            entityType,
            "entitySubType",
            entitySubType));
  }

  private Result found(
      String entityType,
      String entitySubType,
      String subProperty,
      String discriminatorKey,
      String discriminatorValue) {
    if (Objects.nonNull(discriminatorKey) && Objects.nonNull(discriminatorValue)) {
      return Result.ok(
          "found",
          Map.of(
              "path",
              fullPathString,
              "fileType",
              "entities/" + entityType,
              "entityType",
              entityType,
              "entitySubType",
              entitySubType,
              "subProperty",
              subProperty,
              "discriminatorKey",
              discriminatorKey,
              "discriminatorValue",
              discriminatorValue));
    }
    return Result.ok(
        "found",
        Map.of(
            "path",
            fullPathString,
            "fileType",
            "entities/" + entityType,
            "entityType",
            entityType,
            "entitySubType",
            entitySubType,
            "subProperty",
            subProperty));
  }
}
