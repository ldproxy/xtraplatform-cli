package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.store.app.entities.EntityDataStoreImpl;
import de.ii.xtraplatform.store.app.entities.MapAligner;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityMigration;
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
import shadow.com.networknt.schema.ValidationMessage;

public class Entities {

  enum Type {
    Entity,
    Default,
    All
  }

  static boolean DEV = false;

  private static final TypeReference<LinkedHashMap<String, Object>> AS_MAP =
      new TypeReference<LinkedHashMap<String, Object>>() {};

  static Result check(
      LdproxyCfg ldproxyCfg,
      Type type,
      Optional<String> path,
      boolean ignoreRedundant,
      boolean verbose) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();
    Map<Path, Validation> validations = new LinkedHashMap<>();

    for (Validation validation : getValidations(ldproxyCfg, type, path, ignoreRedundant)) {
      validations.put(validation.getPath(), validation);

      if (validation.getError().isPresent() || validation.hasValidationErrors()) {
        ldproxyCfg
            .getEventSubscriptions()
            .addIgnore(
                validation.getType() == Type.Default
                    ? EntityDataDefaultsStore.EVENT_TYPE
                    : EntityDataStore.EVENT_TYPE_ENTITIES,
                validation.getIdentifier());
      }
    }

    // TODO: need to check migrations
    // if (!ignoreRedundant) {
    ldproxyCfg.initStore();

    for (Upgrade upgrade : getUpgrades(ldproxyCfg, type, path, ignoreRedundant)) {
      if (upgrade.getError().isPresent()) {
        result.error(
            String.format("Could not read %s: %s", upgrade.getPath(), upgrade.getError().get()));
      }

      if (upgrade.getUpgrade().isPresent()) {
        if (!validations.containsKey(upgrade.getPath())) {
          System.out.println("NOT FOUND " + upgrade.getPath());
          continue;
        }

        validations.get(upgrade.getPath()).addValidationMessages(upgrade.getValidationMessages());

        if (!ignoreRedundant) {
          validations
              .get(upgrade.getPath())
              .validateRedundant(upgrade.getOriginal(), upgrade.getUpgrade().get());
        }
      }
    }
    // }

    for (Validation validation : validations.values()) {
      validation.log(result, verbose);
    }

    if (result.isEmpty()) {
      result.success("Everything is fine");
    }

    return result;
  }

  static Result preUpgrade(
      LdproxyCfg ldproxyCfg,
      Type type,
      Optional<String> path,
      boolean ignoreRedundant,
      boolean verbose) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();

    for (Validation validation : getValidations(ldproxyCfg, type, path, true)) {
      if (validation.getError().isPresent() || validation.hasValidationErrors()) {
        validation.logErrors(result, verbose);

        ldproxyCfg
            .getEventSubscriptions()
            .addIgnore(
                validation.getType() == Type.Default
                    ? EntityDataDefaultsStore.EVENT_TYPE
                    : EntityDataStore.EVENT_TYPE_ENTITIES,
                validation.getIdentifier());
      }
    }

    ldproxyCfg.initStore();

    for (Upgrade upgrade : getUpgrades(ldproxyCfg, type, path, ignoreRedundant)) {
      if (upgrade.getError().isPresent()) {
        result.error(
            String.format("Could not read %s: %s", upgrade.getPath(), upgrade.getError().get()));
      } else if (upgrade.getUpgrade().isPresent()) {
        if (!result.has(Result.Status.INFO)) {
          result.info(
              String.format(
                  "The following %s configurations will be upgraded:",
                  upgrade.getType().name().toLowerCase()));
        }
        result.info("  - " + upgrade.getPath());
        // TODO: upgrade contains migrations, apply messages if additional entities should be
        // created
        int i = 0;
        for (Map.Entry<Identifier, EntityData> entry : upgrade.getAdditionalEntities().entrySet()) {
          Identifier identifier = entry.getKey();
          if (i++ == 0) {
            result.info(
                String.format(
                    "The following new %s configurations will be created:",
                    upgrade.getType().name().toLowerCase()));
          }
          result.info("  - " + identifier.asPath());
        }
      }
    }

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
      Optional<String> path,
      boolean doBackup,
      boolean ignoreRedundant,
      boolean verbose) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();

    // already happened in preUpgrade when not in dev
    if (DEV) {
      for (Validation validation : getValidations(ldproxyCfg, type, path, true)) {
        if (validation.getError().isPresent() || validation.hasValidationErrors()) {
          ldproxyCfg
              .getEventSubscriptions()
              .addIgnore(
                  validation.getType() == Type.Default
                      ? EntityDataDefaultsStore.EVENT_TYPE
                      : EntityDataStore.EVENT_TYPE_ENTITIES,
                  validation.getIdentifier());
        }
      }

      ldproxyCfg.initStore();
    }

    for (Upgrade upgrade : getUpgrades(ldproxyCfg, type, path, ignoreRedundant)) {
      Path upgradePath = ldproxyCfg.getDataDirectory().resolve(upgrade.getPath());

      if (upgrade.getUpgrade().isPresent()) {
        boolean error = false;

        for (Map.Entry<Identifier, EntityData> entry : upgrade.getAdditionalEntities().entrySet()) {
          Path additionalPath =
              ldproxyCfg.getEntitiesPath().resolve(entry.getKey().asPath().toString() + ".yml");
          if (!error) {
            try {
              // ldproxyCfg.writeEntity(entry.getValue());
              //TODO: rm defaults
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

            ldproxyCfg.getObjectMapper().writeValue(upgradePath.toFile(), upgraded);
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

    if (result.isEmpty()) {
      result.success("Nothing to do");
    }

    return result;
  }

  private static List<Validation> getValidations(
      LdproxyCfg ldproxyCfg, Type type, Optional<String> path, boolean ignoreRedundant) {
    Path store = ldproxyCfg.getDataDirectory().resolve("store");
    Path entities = store.resolve("entities");
    Path defaults = store.resolve("defaults");
    Path entitiesRel = Path.of("store").resolve("entities");

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
                                path.get(), entitiesRel.resolve(identifier.asPath()) + ".yml"))
                .sorted()
                .map(
                    identifier ->
                        getValidation(
                            ldproxyCfg, Type.Entity, entities, identifier, ignoreRedundant)),
            defaultIdentifiers.stream()
                .map(
                    identifier ->
                        getValidation(
                            ldproxyCfg, Type.Default, defaults, identifier, ignoreRedundant)))
        .collect(Collectors.toList());
  }

  private static List<Upgrade> getUpgrades(
      LdproxyCfg ldproxyCfg, Type type, Optional<String> path, boolean ignoreRedundant) {
    Path store = ldproxyCfg.getDataDirectory().resolve("store");
    Path entities = store.resolve("entities");
    Path defaults = store.resolve("defaults");
    Path entitiesRel = Path.of("store").resolve("entities");

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
                                path.get(), entitiesRel.resolve(identifier.asPath()) + ".yml"))
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

  private static Validation getValidation(
      LdproxyCfg ldproxyCfg, Type type, Path root, Identifier identifier, boolean ignoreRedundant) {
    Path base = root.resolve(identifier.asPath());
    Path yml = base.getParent().resolve(base.getFileName().toString() + ".yml");

    Validation validation =
        new Validation(Type.Entity, identifier, ldproxyCfg.getDataDirectory().relativize(yml));

    validation.validate(ldproxyCfg);

    return validation;
  }

  private static Optional<Upgrade> getUpgrade(
      LdproxyCfg ldproxyCfg, Type type, Path root, Identifier identifier, boolean ignoreRedundant) {
    Path base = root.resolve(identifier.asPath());
    Path yml = base.getParent().resolve(base.getFileName().toString() + ".yml");

    try {
      return type == Type.Entity
          ? getEntityUpgrade(ldproxyCfg, yml, identifier, ignoreRedundant)
          : type == Type.Default
              ? getDefaultUpgrade(ldproxyCfg, yml, identifier)
              : Optional.empty();
    } catch (IOException e) {
      return Optional.of(
          new Upgrade(type, ldproxyCfg.getDataDirectory().relativize(yml), e.getMessage()));
    }
  }

  private static Optional<Upgrade> getEntityUpgrade(
      LdproxyCfg ldproxyCfg, Path yml, Identifier identifier, boolean ignoreRedundant)
      throws IOException {
    LinkedHashMap<String, Object> original =
        ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);
    EntityData entityData = ldproxyCfg.getEntityDataStore().get(identifier);

    Set<ValidationMessage> migrationMessages = new HashSet<>();
    Map<Identifier, EntityData> additionalEntities = new LinkedHashMap<>();
    for (EntityMigration<?, ?> migration : ldproxyCfg.migrations().entity()) {
      if (migration.isApplicable(entityData)) {
        migrationMessages.add(
            Validation.migration(migration.getSubject(), migration.getDescription()));
        additionalEntities.putAll(migration.getAdditionalEntities(entityData));
        entityData = migration.migrateRaw(entityData);
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
      return Optional.of(
          new Upgrade(
              Type.Entity,
              ldproxyCfg.getDataDirectory().relativize(yml),
              original,
              upgraded,
              migrationMessages,
              additionalEntities));
    }

    return Optional.empty();
  }

  private static Optional<Upgrade> getDefaultUpgrade(
      LdproxyCfg ldproxyCfg, Path yml, Identifier identifier) throws IOException {
    LinkedHashMap<String, Object> original =
        ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);
    Map<String, Object> upgraded = ldproxyCfg.getEntityDataDefaultsStore().get(identifier);

    if (!Objects.equals(original, upgraded)) {
      return Optional.of(
          new Upgrade(
              Type.Default,
              ldproxyCfg.getDataDirectory().relativize(yml),
              original,
              upgraded,
              Set.of(),
              Map.of()));
    }

    return Optional.empty();
  }
}
