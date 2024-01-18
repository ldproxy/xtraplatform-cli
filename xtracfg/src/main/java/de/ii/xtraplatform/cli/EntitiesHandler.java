package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.entities.app.MapAligner;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataDefaultsStore;
import de.ii.xtraplatform.entities.domain.EntityDataStore;
import de.ii.xtraplatform.entities.domain.EntityMigration;
import de.ii.xtraplatform.values.domain.Identifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import shadow.com.fasterxml.jackson.annotation.JsonInclude;
import shadow.com.fasterxml.jackson.core.type.TypeReference;
import shadow.com.google.common.collect.ImmutableList;

public class EntitiesHandler {

  public enum Type {
    Entity,
    Default,
    All
  }

  public static boolean DEV = false;

  private static final TypeReference<LinkedHashMap<String, Object>> AS_MAP =
      new TypeReference<LinkedHashMap<String, Object>>() {};

  public static Result check(
      LdproxyCfg ldproxyCfg,
      Type type,
      Optional<String> path,
      boolean ignoreRedundant,
      boolean verbose,
      boolean debug) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();
    result.details("path", path.orElse(""));
    Map<Path, Validation> validations = new LinkedHashMap<>();
    Map<Path, Migration> migrations = new LinkedHashMap<>();

    for (Validation validation : getValidations(ldproxyCfg, type, path)) {
      validations.put(validation.getPath(), validation);

      if (validation.getError().isPresent() || validation.hasErrors()) {
        ldproxyCfg.ignoreEventsFor(
            validation.getType() == Type.Default
                ? EntityDataDefaultsStore.EVENT_TYPE
                : EntityDataStore.EVENT_TYPE_ENTITIES,
            validation.getIdentifier());
      }
    }

    ldproxyCfg.initStore();

    for (Upgrade upgrade : getUpgrades(ldproxyCfg, type, path, ignoreRedundant, false, debug)) {
      if (upgrade.getError().isPresent()) {
        result.error(
            String.format("Could not read %s: %s", upgrade.getPath(), upgrade.getError().get()));
      }

      if (upgrade.getUpgrade().isPresent()) {
        if (!validations.containsKey(upgrade.getPath())) {
          System.out.println("NOT FOUND " + upgrade.getPath());
          continue;
        }

        if (upgrade.getMigration().isPresent()) {
          migrations.put(upgrade.getPath(), upgrade.getMigration().get());
        }

        if (!ignoreRedundant) {
          validations
              .get(upgrade.getPath())
              .validateRedundant(upgrade.getOriginal(), upgrade.getUpgrade().get());
        }
      }
    }

    for (Validation validation : validations.values()) {
      validation.log(result, verbose);

      if (migrations.containsKey(validation.getPath())) {
        migrations.get(validation.getPath()).log(result, verbose);
      }
    }

    return result;
  }

  public static Result preUpgrade(
      LdproxyCfg ldproxyCfg,
      Type type,
      Optional<String> path,
      boolean ignoreRedundant,
      boolean force,
      boolean verbose,
      boolean debug) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();

    for (Validation validation : getValidations(ldproxyCfg, type, path)) {
      if (validation.getError().isPresent() || validation.hasErrors()) {
        validation.logErrors(result, verbose);

        ldproxyCfg.ignoreEventsFor(
            validation.getType() == Type.Default
                ? EntityDataDefaultsStore.EVENT_TYPE
                : EntityDataStore.EVENT_TYPE_ENTITIES,
            validation.getIdentifier());
      }
    }

    ldproxyCfg.initStore();

    int i = 0;
    for (Upgrade upgrade : getUpgrades(ldproxyCfg, type, path, ignoreRedundant, force, debug)) {
      if (upgrade.getError().isPresent()) {
        result.error(
            String.format("Could not read %s: %s", upgrade.getPath(), upgrade.getError().get()));
      } else if (upgrade.getUpgrade().isPresent()) {
        if (i++ == 0) {
          result.info(
              String.format(
                  "The following %s configurations will be upgraded:",
                  upgrade.getType().name().toLowerCase()));
        }
        result.info("  - " + upgrade.getPath());
      }
    }

    int j = 0;
    for (Upgrade upgrade : getUpgrades(ldproxyCfg, type, path, ignoreRedundant, force, debug)) {
      if (upgrade.getUpgrade().isPresent()) {
        for (Map.Entry<Path, EntityData> entry : upgrade.getAdditionalEntities().entrySet()) {
          Path pathAdd = ldproxyCfg.getDataDirectory().relativize(entry.getKey());
          if (j++ == 0) {
            result.info(
                String.format(
                    "The following new %s configurations will be created:",
                    upgrade.getType().name().toLowerCase()));
          }
          result.info("  - " + pathAdd);
        }
      }
    }

    if (result.has(Result.Status.INFO)) {
      result.confirmation("Are you sure?");
    }

    return result;
  }

  public static Result upgrade(
      LdproxyCfg ldproxyCfg,
      Type type,
      Optional<String> path,
      boolean doBackup,
      boolean ignoreRedundant,
      boolean force,
      boolean verbose,
      boolean debug) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();

    // already happened in preUpgrade when not in dev
    if (DEV) {
      for (Validation validation : getValidations(ldproxyCfg, type, path)) {
        if (validation.getError().isPresent() || validation.hasErrors()) {
          ldproxyCfg.ignoreEventsFor(
              validation.getType() == Type.Default
                  ? EntityDataDefaultsStore.EVENT_TYPE
                  : EntityDataStore.EVENT_TYPE_ENTITIES,
              validation.getIdentifier());
        }
      }

      ldproxyCfg.initStore();
    }

    for (Upgrade upgrade : getUpgrades(ldproxyCfg, type, path, ignoreRedundant, force, debug)) {
      Path upgradePath = ldproxyCfg.getDataDirectory().resolve(upgrade.getPath());

      if (upgrade.getUpgrade().isPresent()) {
        boolean error = false;

        for (Map.Entry<Path, EntityData> entry : upgrade.getAdditionalEntities().entrySet()) {
          Path additionalPath = entry.getKey();
          if (!error) {
            try {
              ldproxyCfg.getObjectMapper().writeValue(additionalPath.toFile(), entry.getValue());
            } catch (Throwable e) {
              error = true;
              result.error(
                  String.format("Could not create %s: %s", additionalPath, e.getMessage()));
            }
            if (!error) {
              result.success(
                  String.format(
                      "%s configuration created: %s",
                      upgrade.getType().name(),
                      ldproxyCfg.getDataDirectory().relativize(additionalPath)));
            }
          }
        }

        if (!error && doBackup) {
          Path backup =
              upgradePath.getParent().resolve(upgradePath.getFileName().toString() + ".backup");
          try {
            Files.copy(
                upgradePath,
                backup,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);

            if (verbose) {
              result.success(
                  String.format(
                      "%s configuration backup created: %s",
                      upgrade.getType().name(), ldproxyCfg.getDataDirectory().relativize(backup)));
            }
          } catch (IOException e) {
            error = true;
            result.error(String.format("Could not create backup %s: %s", backup, e.getMessage()));
          }
        }

        if (!error) {
          try {
            Map<String, Object> upgraded = upgrade.getUpgrade().get();
            if (upgraded.containsKey("lastModified")) {
              upgraded.put("lastModified", Instant.now().toEpochMilli());
            }

            ldproxyCfg
                .getObjectMapper()
                .copy()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .writeValue(upgradePath.toFile(), upgraded);
          } catch (IOException e) {
            error = true;
            result.error(String.format("Could not upgrade %s: %s", upgradePath, e.getMessage()));
          }
        }

        if (!error) {
          result.success(
              String.format(
                  "%s configuration upgraded: %s", upgrade.getType().name(), upgrade.getPath()));
        }
      }

      if (upgrade.getError().isPresent()) {
        result.error(String.format("Could not read %s: %s", upgradePath, upgrade.getError().get()));
      }
    }

    return result;
  }

  private static List<Validation> getValidations(
      LdproxyCfg ldproxyCfg, Type type, Optional<String> path) {
    Path entities = ldproxyCfg.getEntitiesPath();
    Path defaults = ldproxyCfg.getEntitiesPath().getParent().resolve("defaults");
    Path entitiesRel = ldproxyCfg.getDataDirectory().relativize(entities);

    List<Identifier> entityIdentifiers =
        type == Type.Entity || type == Type.All ? ldproxyCfg.getEntityIdentifiers() : List.of();
    List<Identifier> defaultIdentifiers =
        type == Type.Default || type == Type.All
            // TODO
            ? ldproxyCfg.getEntityDataDefaultsStore().identifiers()
            : List.of();

    return Stream.concat(
            entityIdentifiers.stream()
                .filter(
                    identifier ->
                        path.isEmpty()
                            || Objects.equals(
                                normalize(path.get()),
                                entitiesRel.resolve(identifier.asPath()) + ".yml"))
                .sorted()
                .map(identifier -> getValidation(ldproxyCfg, entities, identifier)),
            defaultIdentifiers.stream()
                .map(identifier -> getValidation(ldproxyCfg, defaults, identifier)))
        .collect(Collectors.toList());
  }

  private static String normalize(String path) {
    return Path.of(path).toString();
  }

  private static List<Upgrade> getUpgrades(
      LdproxyCfg ldproxyCfg,
      Type type,
      Optional<String> path,
      boolean ignoreRedundant,
      boolean force,
      boolean debug) {
    Path entities = ldproxyCfg.getEntitiesPath();
    Path defaults = ldproxyCfg.getEntitiesPath().getParent().resolve("defaults");
    Path entitiesRel = ldproxyCfg.getDataDirectory().relativize(entities);
    Path defaultsRel = ldproxyCfg.getDataDirectory().relativize(defaults);

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
                .filter(
                    identifier ->
                        path.isEmpty()
                            || Objects.equals(
                                normalize(path.get()), entitiesRel.resolve(identifier.asPath()) + ".yml"))
                .map(
                    identifier ->
                        getUpgrade(
                            ldproxyCfg,
                            Type.Entity,
                            entities,
                            identifier,
                            ignoreRedundant,
                            force,
                            debug)),
            defaultIdentifiers.stream()
                .filter(
                    identifier ->
                        path.isEmpty()
                            || Objects.equals(
                                normalize(path.get()), defaultsRel.resolve(identifier.asPath()) + ".yml"))
                .map(
                    identifier ->
                        getUpgrade(
                            ldproxyCfg,
                            Type.Default,
                            defaults,
                            identifier,
                            ignoreRedundant,
                            force,
                            debug)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private static Validation getValidation(LdproxyCfg ldproxyCfg, Path root, Identifier identifier) {
    Path base = root.resolve(identifier.asPath());
    Path yml = base.getParent().resolve(base.getFileName().toString() + ".yml");

    Validation validation =
        new Validation(Type.Entity, identifier, ldproxyCfg.getDataDirectory().relativize(yml));

    validation.validate(ldproxyCfg);

    return validation;
  }

  private static Optional<Upgrade> getUpgrade(
      LdproxyCfg ldproxyCfg,
      Type type,
      Path root,
      Identifier identifier,
      boolean ignoreRedundant,
      boolean force,
      boolean debug) {
    Path base = root.resolve(identifier.asPath());
    Path yml = base.getParent().resolve(base.getFileName().toString() + ".yml");

    try {
      return type == Type.Entity
          ? getEntityUpgrade(ldproxyCfg, yml, identifier, ignoreRedundant, force, debug)
          : type == Type.Default
              ? getDefaultUpgrade(ldproxyCfg, yml, identifier, force, debug)
              : Optional.empty();
    } catch (Throwable e) {
      if (debug) {
        System.err.println("Could not read " + yml);
        e.printStackTrace(System.err);
      }
      return Optional.of(
          new Upgrade(type, ldproxyCfg.getDataDirectory().relativize(yml), e.getMessage()));
    }
  }

  private static Optional<Upgrade> getEntityUpgrade(
      LdproxyCfg ldproxyCfg,
      Path yml,
      Identifier identifier,
      boolean ignoreRedundant,
      boolean force,
      boolean debug)
      throws IOException {
    Map<String, Object> original = ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);
    EntityData entityData = ldproxyCfg.getEntityDataStore().get(identifier);

    Identifier defaultsIdentifier =
        EntityDataStore.defaults(identifier, entityData.getEntitySubType());
    EntityData defaults =
        ldproxyCfg
            .getEntityDataDefaultsStore()
            .getBuilder(defaultsIdentifier)
            .fillRequiredFieldsWithPlaceholders()
            .build();

    Map<Path, EntityData> additionalEntities = new LinkedHashMap<>();
    Migration migration =
        new Migration(Type.Entity, identifier, ldproxyCfg.getDataDirectory().relativize(yml));
    for (EntityMigration<?, ?> entityMigration : ldproxyCfg.migrations().entity()) {
      if (entityMigration.isApplicable(entityData, Optional.of(defaults))) {
        migration.addMessage(
            Migration.migration(entityMigration.getSubject(), entityMigration.getDescription()));

        entityMigration
            .getAdditionalEntities(entityData, Optional.of(defaults))
            .forEach(
                ((identifierAdd, entityDataAdd) -> {
                  Path ymlAdd =
                      ldproxyCfg
                          .getEntitiesPath()
                          .resolve(identifierAdd.asPath().toString() + ".yml");
                  additionalEntities.put(ymlAdd, entityDataAdd);
                }));

        entityData = entityMigration.migrateRaw(entityData, Optional.of(defaults));
      }
    }

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
                .get(EntityDataStore.entityType(identifier), entityData.getEntitySubType()));

    if (!original.containsKey("createdAt")) {
      upgraded.remove("createdAt");
    }
    if (!original.containsKey("lastModified")) {
      upgraded.remove("lastModified");
    }
    original.remove("entityStorageVersion");
    upgraded.remove("entityStorageVersion");

    Map<String, String> diff = MapDiffer.diff(original, upgraded, true);
    if (debug) {
      System.out.println("DIFF " + diff);
    }

    if (force || !diff.isEmpty()) {
      return Optional.of(
          new Upgrade(
              Type.Entity,
              ldproxyCfg.getDataDirectory().relativize(yml),
              original,
              upgraded,
              migration,
              additionalEntities));
    }

    return Optional.empty();
  }

  private static Optional<Upgrade> getDefaultUpgrade(
      LdproxyCfg ldproxyCfg, Path yml, Identifier identifier, boolean force, boolean debug)
      throws IOException {
    LinkedHashMap<String, Object> original =
        ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);
    Map<String, Object> upgraded = ldproxyCfg.getEntityDataDefaultsStore().get(identifier);

    Map<String, String> diff = MapDiffer.diff(original, upgraded, true);
    if (debug) {
      System.out.println("DIFF " + diff);
    }

    if (force || !diff.isEmpty()) {
      return Optional.of(
          new Upgrade(
              Type.Default,
              ldproxyCfg.getDataDirectory().relativize(yml),
              original,
              upgraded,
              null,
              Map.of()));
    }

    return Optional.empty();
  }
}
