package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.store.app.entities.EntityDataStoreImpl;
import de.ii.xtraplatform.store.app.entities.MapAligner;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import shadow.com.fasterxml.jackson.core.type.TypeReference;
import shadow.com.google.common.collect.ImmutableList;

public class Entities {

  enum Type {
    Entity,
    Default,
    All
  }

  private static final TypeReference<LinkedHashMap<String, Object>> AS_MAP =
      new TypeReference<LinkedHashMap<String, Object>>() {};

  static Result check(LdproxyCfg ldproxyCfg, Type type, boolean ignoreRedundant) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();
    List<Upgrade> upgrades = getUpgrades(ldproxyCfg, type, ignoreRedundant);

    upgrades.forEach(
        upgrade -> {
          if (upgrade.getUpgrade().isPresent()) {
            result.warning(
                String.format(
                    "%s configuration has deprecated or redundant settings: %s",
                    upgrade.getType().name(),
                    ldproxyCfg.getDataDirectory().relativize(upgrade.getPath())));
          }

          if (upgrade.getError().isPresent()) {
            result.error(
                String.format(
                    "Could not read %s: %s", upgrade.getPath(), upgrade.getError().get()));
          }
        });

    if (result.isEmpty()) {
      result.success("Everything is fine");
    }

    return result;
  }

  static Result preUpgrade(LdproxyCfg ldproxyCfg, Type type, boolean ignoreRedundant) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();
    List<Upgrade> upgrades = getUpgrades(ldproxyCfg, type, ignoreRedundant);

    upgrades.forEach(
        upgrade -> {
          if (upgrade.getUpgrade().isPresent()) {
            if (result.isEmpty()) {
              result.info(
                  String.format(
                      "The following %s configurations will be upgraded:",
                      upgrade.getType().name().toLowerCase()));
            }
            result.info("- " + ldproxyCfg.getDataDirectory().relativize(upgrade.getPath()));
          }

          if (upgrade.getError().isPresent()) {
            result.error(
                String.format(
                    "Could not read %s: %s", upgrade.getPath(), upgrade.getError().get()));
          }
        });

    if (result.isEmpty()) {
      result.success("Nothing to do");
    } else if (result.has(Result.Status.INFO)) {
      result.confirmation("Are you sure?");
    }

    return result;
  }

  static Result upgrade(
      LdproxyCfg ldproxyCfg,
      Type type,
      boolean doBackup,
      boolean ignoreRedundant,
      boolean verbose) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();
    List<Upgrade> upgrades = getUpgrades(ldproxyCfg, type, ignoreRedundant);

    upgrades.forEach(
        upgrade -> {
          if (upgrade.getUpgrade().isPresent()) {
            boolean error = false;
            if (doBackup) {
              Path backup =
                  upgrade
                      .getPath()
                      .getParent()
                      .resolve(upgrade.getPath().getFileName().toString() + ".backup");
              try {
                Files.copy(
                    upgrade.getPath(),
                    backup,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);

                if (verbose) {
                  result.success(
                      String.format(
                          "%s configuration backup created: %s",
                          upgrade.getType().name(),
                          ldproxyCfg.getDataDirectory().relativize(backup)));
                }
              } catch (IOException e) {
                error = true;
                result.error(
                    String.format("Could not create backup %s: %s", backup, e.getMessage()));
              }
            }
            if (!error) {
              try {
                Map<String, Object> upgraded = upgrade.getUpgrade().get();
                if (upgraded.containsKey("lastModified")) {
                  upgraded.put("lastModified", Instant.now().toEpochMilli());
                }

                ldproxyCfg.getObjectMapper().writeValue(upgrade.getPath().toFile(), upgraded);
              } catch (IOException e) {
                error = true;
                result.error(
                    String.format("Could not upgrade %s: %s", upgrade.getPath(), e.getMessage()));
              }
            }
            if (!error) {
              result.success(
                  String.format(
                      "%s configuration upgraded: %s",
                      upgrade.getType().name(),
                      ldproxyCfg.getDataDirectory().relativize(upgrade.getPath())));
            }
          }

          if (upgrade.getError().isPresent()) {
            result.error(
                String.format(
                    "Could not read %s: %s", upgrade.getPath(), upgrade.getError().get()));
          }
        });

    if (result.isEmpty()) {
      result.success("Nothing to do");
    }

    return result;
  }

  private static List<Upgrade> getUpgrades(
      LdproxyCfg ldproxyCfg, Type type, boolean ignoreRedundant) {
    Path store = ldproxyCfg.getDataDirectory().resolve("store");
    Path entities = store.resolve("entities");
    Path defaults = store.resolve("defaults");

    List<Identifier> entityIdentifiers =
        type == Type.Entity || type == Type.All
            ? ldproxyCfg.getEntityDataStore().identifiers()
            : List.of();
    List<Identifier> defaultIdentifiers =
        type == Type.Default || type == Type.All
            ? ldproxyCfg.getEntityDataDefaultsStore().identifiers()
            : List.of();

    // TODO: optionally compare ordering of elements
    return Stream.concat(
            entityIdentifiers.stream()
                .map(
                    identifier ->
                        getUpgrade(ldproxyCfg, Type.Entity, entities, identifier, ignoreRedundant)),
            defaultIdentifiers.stream()
                .map(
                    identifier ->
                        getUpgrade(
                            ldproxyCfg, Type.Default, defaults, identifier, ignoreRedundant)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private static Optional<Upgrade> getUpgrade(
      LdproxyCfg ldproxyCfg, Type type, Path root, Identifier identifier, boolean ignoreRedundant) {
    Path base = root.resolve(identifier.asPath());
    Path yml = base.getParent().resolve(base.getFileName().toString() + ".yml");

    try {
      Optional<Tuple<Map<String, Object>, Map<String, Object>>> upgrade =
          type == Type.Entity
              ? getEntityUpgrade(ldproxyCfg, yml, identifier, ignoreRedundant)
              : type == Type.Default
                  ? getDefaultUpgrade(ldproxyCfg, yml, identifier)
                  : Optional.empty();

      return upgrade.map(up -> new Upgrade(type, yml, up.first(), up.second()));
    } catch (IOException e) {
      return Optional.of(new Upgrade(type, yml, e.getMessage()));
    }
  }

  private static Optional<Tuple<Map<String, Object>, Map<String, Object>>> getEntityUpgrade(
      LdproxyCfg ldproxyCfg, Path yml, Identifier identifier, boolean ignoreRedundant)
      throws IOException {
    LinkedHashMap<String, Object> original =
        ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);
    EntityData entityData = ldproxyCfg.getEntityDataStore().get(identifier);
    Map<String, Object> upgraded = ldproxyCfg.getEntityDataStore().asMap(identifier, entityData);

    if (!ignoreRedundant) {
      upgraded =
          ldproxyCfg
              .getEntityDataDefaultsStore()
              .subtractDefaults(
                  identifier, entityData.getEntitySubType(), upgraded, ImmutableList.of("enabled"));
    }

    // carry over substitutions
    upgraded =
        MapAligner.align(
            upgraded,
            original,
            value -> value instanceof String && ((String) value).startsWith("${"),
            ldproxyCfg
                .getEntityFactories()
                .get(EntityDataStoreImpl.entityType(identifier), entityData.getEntitySubType()));

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

  private static Optional<Tuple<Map<String, Object>, Map<String, Object>>> getDefaultUpgrade(
      LdproxyCfg ldproxyCfg, Path yml, Identifier identifier) throws IOException {
    LinkedHashMap<String, Object> original =
        ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);
    Map<String, Object> upgraded = ldproxyCfg.getEntityDataDefaultsStore().get(identifier);

    if (!Objects.equals(original, upgraded)) {
      return Optional.of(Tuple.of(original, upgraded));
    }

    return Optional.empty();
  }
}
