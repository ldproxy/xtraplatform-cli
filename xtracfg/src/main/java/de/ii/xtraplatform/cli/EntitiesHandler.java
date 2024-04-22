package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.cmd.FileType;
import de.ii.xtraplatform.entities.app.MapAligner;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import de.ii.xtraplatform.entities.domain.EntityDataDefaultsStore;
import de.ii.xtraplatform.entities.domain.EntityDataStore;
import de.ii.xtraplatform.entities.domain.EntityMigration;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ValueEncoding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    Defaults,
    Overrides,
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
        ldproxyCfg.ignoreEventsFor(EntityDataDefaultsStore.EVENT_TYPE, validation.getIdentifier());
        ldproxyCfg.ignoreEventsFor(EntityDataStore.EVENT_TYPE_ENTITIES, validation.getIdentifier());
        ldproxyCfg.ignoreEventsFor(
            EntityDataStore.EVENT_TYPE_OVERRIDES, validation.getIdentifier());
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

        ldproxyCfg.ignoreEventsFor(EntityDataDefaultsStore.EVENT_TYPE, validation.getIdentifier());
        ldproxyCfg.ignoreEventsFor(EntityDataStore.EVENT_TYPE_ENTITIES, validation.getIdentifier());
        ldproxyCfg.ignoreEventsFor(
            EntityDataStore.EVENT_TYPE_OVERRIDES, validation.getIdentifier());
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
              EntityDataDefaultsStore.EVENT_TYPE, validation.getIdentifier());
          ldproxyCfg.ignoreEventsFor(
              EntityDataStore.EVENT_TYPE_ENTITIES, validation.getIdentifier());
          ldproxyCfg.ignoreEventsFor(
              EntityDataStore.EVENT_TYPE_OVERRIDES, validation.getIdentifier());
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
            Map<Integer, String> comments = new HashMap<>();

            if (upgraded.containsKey("lastModified")) {
              upgraded.put("lastModified", Instant.now().toEpochMilli());
            }


            try (Stream<String> lines = Files.lines(upgradePath)) {
              int[] lineNumber = {0}; // array is used to allow modification in lambda expression
              lines.forEach(
                  line -> {
                    if (line.trim().startsWith("#")) {
                      comments.put(lineNumber[0], line);
                    }
                    lineNumber[0]++;
                  });
            }

            System.out.println("comments: " + comments);

            // 2. Convert the updated object to a string
            String upgradedStr =
                ldproxyCfg
                    .getObjectMapper()
                    .copy()
                    .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                    .writeValueAsString(upgraded);

            // 3. Split the converted string into a list of lines
            List<String> upgradedLines = new ArrayList<>(Arrays.asList(upgradedStr.split("\n")));

            // 4. Insert the stored comments at the appropriate places into the list of lines
            comments.forEach(
                (lineNum, comment) -> {
                  if (lineNum < upgradedLines.size()) {
                    upgradedLines.add(lineNum, comment);
                  } else {
                    upgradedLines.add(comment);
                  }
                });

            // 5. Write the list of lines back to the file
         Files.write(upgradePath, upgradedLines);
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
    Path overrides = ldproxyCfg.getEntitiesPath().getParent().resolve("overrides");
    Path entitiesRel = ldproxyCfg.getDataDirectory().relativize(entities);
    Path defaultsRel = ldproxyCfg.getDataDirectory().relativize(defaults);
    Path overridesRel = ldproxyCfg.getDataDirectory().relativize(overrides);

    List<Identifier> entityIdentifiers =
        type == Type.Entity || type == Type.All ? ldproxyCfg.getEntityIdentifiers() : List.of();
    List<Identifier> defaultIdentifiers =
        type == Type.Defaults || type == Type.All ? ldproxyCfg.getDefaultIdentifiers() : List.of();
    List<Identifier> overrideIdentifiers =
        type == Type.Overrides || type == Type.All
            ? ldproxyCfg.getOverrideIdentifiers()
            : List.of();

    return Stream.concat(
            Stream.concat(
                entityIdentifiers.stream()
                    .filter(
                        identifier ->
                            path.isEmpty()
                                || Objects.equals(
                                    normalize(path.get()),
                                    entitiesRel.resolve(identifier.asPath()) + ".yml"))
                    .sorted()
                    .map(
                        identifier -> getValidation(ldproxyCfg, entities, identifier, Type.Entity)),
                defaultIdentifiers.stream()
                    .filter(
                        identifier ->
                            path.isEmpty()
                                || Objects.equals(
                                    normalize(path.get()),
                                    defaultsRel.resolve(identifier.asPath()) + ".yml"))
                    .sorted()
                    .map(
                        identifier ->
                            getValidation(ldproxyCfg, defaults, identifier, Type.Defaults))),
            overrideIdentifiers.stream()
                .filter(
                    identifier ->
                        path.isEmpty()
                            || Objects.equals(
                                normalize(path.get()),
                                overridesRel.resolve(identifier.asPath()) + ".yml"))
                .sorted()
                .map(
                    identifier -> getValidation(ldproxyCfg, overrides, identifier, Type.Overrides)))
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
    Path overrides = ldproxyCfg.getEntitiesPath().getParent().resolve("overrides");
    Path entitiesRel = ldproxyCfg.getDataDirectory().relativize(entities);
    Path defaultsRel = ldproxyCfg.getDataDirectory().relativize(defaults);
    Path overridesRel = ldproxyCfg.getDataDirectory().relativize(overrides);

    List<Identifier> entityIdentifiers =
        type == Type.Entity || type == Type.All
            ? ldproxyCfg.getEntityDataStore().identifiers()
            : List.of();
    List<Identifier> defaultIdentifiers =
        type == Type.Defaults || type == Type.All ? ldproxyCfg.getDefaultIdentifiers() : List.of();
    List<Identifier> overrideIdentifiers =
        type == Type.Overrides || type == Type.All
            ? ldproxyCfg.getOverrideIdentifiers()
            : List.of();

    // TODO: optionally compare ordering of elements
    return Stream.concat(
            Stream.concat(
                getUpgrades(
                    ldproxyCfg,
                    Type.Entity,
                    path,
                    ignoreRedundant,
                    force,
                    debug,
                    entityIdentifiers,
                    entities,
                    entitiesRel),
                getUpgrades(
                    ldproxyCfg,
                    Type.Defaults,
                    path,
                    ignoreRedundant,
                    force,
                    debug,
                    defaultIdentifiers,
                    defaults,
                    defaultsRel)),
            getUpgrades(
                ldproxyCfg,
                Type.Overrides,
                path,
                ignoreRedundant,
                force,
                debug,
                overrideIdentifiers,
                overrides,
                overridesRel))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private static Stream<Optional<Upgrade>> getUpgrades(
      LdproxyCfg ldproxyCfg,
      Type type,
      Optional<String> path,
      boolean ignoreRedundant,
      boolean force,
      boolean debug,
      List<Identifier> identifiers,
      Path overridesPath,
      Path overridesPathRel) {
    return identifiers.stream()
        .filter(
            identifier ->
                path.isEmpty()
                    || Objects.equals(
                        normalize(path.get()),
                        overridesPathRel.resolve(identifier.asPath()) + ".yml"))
        .map(
            identifier ->
                getUpgrade(
                    ldproxyCfg, type, overridesPath, identifier, ignoreRedundant, force, debug));
  }

  private static Validation getValidation(
      LdproxyCfg ldproxyCfg, Path root, Identifier identifier, Type type) {
    Path base = root.resolve(identifier.asPath());
    Path yml = base.getParent().resolve(base.getFileName().toString() + ".yml");
    Path relYml = ldproxyCfg.getDataDirectory().relativize(yml);

    Map<String, String> fileType = new FileType(Map.of("path", relYml.toString())).get(ldproxyCfg);

    Validation validation = new Validation(type, identifier, relYml);

    if (fileType.containsKey("entityType")) {
      validation.validate(ldproxyCfg, fileType);
    }

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
          : type == Type.Defaults
              ? getDefaultUpgrade(ldproxyCfg, yml, identifier, force, debug)
              : type == Type.Overrides
                  ? getOverrideUpgrade(ldproxyCfg, yml, identifier, ignoreRedundant, force, debug)
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
    Path relYml = ldproxyCfg.getDataDirectory().relativize(yml);
    Map<String, String> fileType = new FileType(Map.of("path", relYml.toString())).get(ldproxyCfg);

    if (!fileType.containsKey("entityType") || !fileType.containsKey("entitySubType")) {
      return Optional.empty();
    }

    Identifier storeIdentifier =
        Identifier.from("defaults", fileType.get("entityType"), fileType.get("entitySubType"));

    Map<String, Object> original = ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);
    Map<String, Object> upgraded =
        getUpgradedDefaultsOrOverrides(
            ldproxyCfg, yml, storeIdentifier, Type.Defaults, fileType, original);

    String discriminatorKey = fileType.get("discriminatorKey");
    String discriminatorValue = fileType.get("discriminatorValue");
    String subProperty = fileType.get("subProperty");

    Map<String, Object> matchingMap = new HashMap<>();
    Map<String, Object> subPropertyMap = new HashMap<>();

    if (upgraded instanceof Map) {
      Map<String, Object> outerMap = (Map<String, Object>) upgraded;
      for (Map.Entry<String, Object> entry : outerMap.entrySet()) {
        if (discriminatorKey == null
            && discriminatorValue == null
            && subProperty != null
            && entry.getKey().equals(subProperty)
            && entry.getValue() instanceof Map) {
          subPropertyMap = (Map<String, Object>) entry.getValue();
        } else if (entry.getValue() instanceof Map) {
          Map<String, Object> innerMap = (Map<String, Object>) entry.getValue();
          Object value = innerMap.get(discriminatorKey);
          if (value != null && value.equals(discriminatorValue)) {
            matchingMap.putAll(innerMap);
            break;
          }
        } else if (entry.getValue() instanceof List) {
          List<Object> innerList = (List<Object>) entry.getValue();
          for (Object item : innerList) {
            if (item instanceof Map) {
              Map<String, Object> innerMap = (Map<String, Object>) item;
              Object value = innerMap.get(discriminatorKey);
              if (value != null && value.equals(discriminatorValue)) {
                matchingMap.putAll(innerMap);
                break;
              }
            }
          }
        }
      }
    } else {
      System.out.println("upgraded ist keine Map");
    }

    if (discriminatorKey != null && discriminatorValue != null) {
      original.put(discriminatorKey, discriminatorValue);
    }
    System.out.println("subPropertyMap: " + subPropertyMap);
    System.out.println("matchingMap: " + matchingMap);
    System.out.println("original: " + original);

    Map<String, Object> finalUpgraded =
        Objects.isNull(subProperty)
            ? upgraded
            : !subPropertyMap.isEmpty() ? subPropertyMap : matchingMap;

    Map<String, String> diff = MapDiffer.diff(original, finalUpgraded, true);

    if (debug) {
      System.out.println("DIFF " + diff);
    }

    if (force || !diff.isEmpty()) {
      return Optional.of(
          new Upgrade(Type.Defaults, relYml, original, finalUpgraded, null, Map.of()));
    }

    return Optional.empty();
  }

  private static Optional<Upgrade> getOverrideUpgrade(
      LdproxyCfg ldproxyCfg,
      Path yml,
      Identifier identifier,
      boolean ignoreRedundant,
      boolean force,
      boolean debug)
      throws IOException {
    Path relYml = ldproxyCfg.getDataDirectory().relativize(yml);
    Map<String, String> fileType = new FileType(Map.of("path", relYml.toString())).get(ldproxyCfg);

    if (!fileType.containsKey("entityType") || !fileType.containsKey("entitySubType")) {
      return Optional.empty();
    }

    Map<String, Object> original = ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);
    Map<String, Object> upgraded =
        getUpgradedDefaultsOrOverrides(
            ldproxyCfg, yml, identifier, Type.Overrides, fileType, original);

    Map<String, String> diff = MapDiffer.diff(original, upgraded, true);
    if (debug) {
      System.out.println("DIFF " + diff);
    }

    if (force || !diff.isEmpty()) {
      return Optional.of(new Upgrade(Type.Defaults, relYml, original, upgraded, null, Map.of()));
    }

    return Optional.empty();
  }

  private static Map<String, Object> getUpgradedDefaultsOrOverrides(
      LdproxyCfg ldproxyCfg,
      Path yml,
      Identifier identifier,
      Type type,
      Map<String, String> fileType,
      Map<String, Object> original)
      throws IOException {
    String fileContent = Validation.loadFileContent(yml, type, fileType);
    EntityDataBuilder<? extends EntityData> builder =
        ldproxyCfg
            .getEntityFactories()
            .get(fileType.get("entityType"), fileType.get("entitySubType"))
            .emptyDataBuilder()
            .fillRequiredFieldsWithPlaceholders();
    ldproxyCfg
        .getEntityDataStore()
        .getValueEncoding()
        .getMapper(ValueEncoding.FORMAT.YML)
        .readerForUpdating(builder)
        .readValue(fileContent);

    Map<String, Object> upgraded =
        ldproxyCfg.getEntityDataDefaultsStore().asMap(identifier, builder.build());
    Map<String, Object> cleanUpgraded = new LinkedHashMap<>();

    for (Map.Entry<String, Object> entry : upgraded.entrySet()) {
      if (!Objects.equals(entry.getValue(), "__DEFAULT__")
          && !List.of("lastModified", "createdAt", "entityStorageVersion")
              .contains(entry.getKey())) {
        if (entry.getKey().equals("label") && !original.containsKey("label")) {
          continue;
        }
        if (entry.getKey().equals("enabled")
            && !(original.containsKey("enabled") || original.containsKey("shouldStart"))) {
          continue;
        }

        cleanUpgraded.put(entry.getKey(), entry.getValue());
      }
    }

    return cleanUpgraded;
  }
}
