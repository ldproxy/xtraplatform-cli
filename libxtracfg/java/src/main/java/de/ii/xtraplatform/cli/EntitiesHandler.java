package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.cmd.FileType;
import de.ii.xtraplatform.cli.cmd.FileType.FileInfo;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import shadow.com.fasterxml.jackson.annotation.JsonInclude;
import shadow.com.fasterxml.jackson.core.type.TypeReference;

public class EntitiesHandler {

  public enum Type {
    Entity,
    Defaults,
    Overrides,
    All
  }

  public static boolean DEV = false;

  private static final org.slf4j.Logger LOGGER =
      org.slf4j.LoggerFactory.getLogger(EntitiesHandler.class);

  private static final TypeReference<LinkedHashMap<String, Object>> AS_MAP =
      new TypeReference<LinkedHashMap<String, Object>>() {};

  private static class CommentInfo {
    final String comment;
    final int lineNumber;
    final boolean isStandalone;

    public CommentInfo(String comment, int lineNumber, boolean isStandalone) {
      this.comment = comment;
      this.lineNumber = lineNumber;
      this.isStandalone = isStandalone;
    }
  }

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

    List<Upgrade> upgrades = getUpgrades(ldproxyCfg, type, path, ignoreRedundant, force, debug);

    int i = 0;
    for (Upgrade upgrade : upgrades) {
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
    for (Upgrade upgrade : upgrades) {
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

    for (Validation validation : getValidations(ldproxyCfg, type, path)) {
      if (validation.getError().isPresent() || validation.hasErrors()) {
        ldproxyCfg.ignoreEventsFor(EntityDataDefaultsStore.EVENT_TYPE, validation.getIdentifier());
        ldproxyCfg.ignoreEventsFor(EntityDataStore.EVENT_TYPE_ENTITIES, validation.getIdentifier());
        ldproxyCfg.ignoreEventsFor(
            EntityDataStore.EVENT_TYPE_OVERRIDES, validation.getIdentifier());
      }
    }

    ldproxyCfg.initStore();

    List<Upgrade> upgrades = getUpgrades(ldproxyCfg, type, path, ignoreRedundant, force, debug);

    for (Upgrade upgrade : upgrades) {
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
            List<CommentInfo> comments = new ArrayList<>();

            if (upgraded.containsKey("lastModified")) {
              upgraded.put("lastModified", Instant.now().toEpochMilli());
            }

            try (Stream<String> lines = Files.lines(upgradePath)) {
              int[] lineNumber = {0}; // array is used to allow modification in lambda expression
              lines.forEach(
                  line -> {
                    int commentIndex = line.indexOf("#");
                    if (commentIndex != -1) {
                      boolean isStandalone = line.trim().indexOf("#") == 0;
                      String comment = isStandalone ? line : line.substring(commentIndex);
                      comments.add(new CommentInfo(comment, lineNumber[0], isStandalone));
                    }
                    lineNumber[0]++;
                  });
            }

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
                (commentInfo) -> {
                  if (commentInfo.lineNumber < upgradedLines.size()) {
                    if (commentInfo.isStandalone) {
                      // If the comment is standalone, add it in a new line
                      upgradedLines.add(commentInfo.lineNumber, commentInfo.comment);
                    } else {
                      // If the comment is not standalone, add it at the end of the line
                      upgradedLines.set(
                          commentInfo.lineNumber,
                          upgradedLines.get(commentInfo.lineNumber) + " " + commentInfo.comment);
                    }
                  } else {
                    upgradedLines.add(commentInfo.comment);
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

    if (debug && LOGGER.isDebugEnabled()) {
      LOGGER.debug("defaultIdentifiers: {}", defaultIdentifiers);
    }

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
      Path parentPath,
      Path parentPathRel) {
    return identifiers.stream()
        .filter(
            identifier ->
                path.isEmpty()
                    || Objects.equals(
                        normalize(path.get()), parentPathRel.resolve(identifier.asPath()) + ".yml"))
        .map(
            identifier ->
                getUpgrade(
                    ldproxyCfg, type, parentPath, identifier, ignoreRedundant, force, debug));
  }

  private static Validation getValidation(
      LdproxyCfg ldproxyCfg, Path root, Identifier identifier, Type type) {
    Path base = root.resolve(identifier.asPath());
    Path yml = base.getParent().resolve(base.getFileName().toString() + ".yml");
    Path relYml = ldproxyCfg.getDataDirectory().relativize(yml);

    FileInfo fileInfo = new FileType(Map.of("path", relYml.toString())).get(ldproxyCfg);

    Validation validation = new Validation(type, identifier, relYml, fileInfo);

    if (fileInfo.isValid()) {
      validation.validate(ldproxyCfg, fileInfo);
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
              .subtractDefaults(identifier, entityData.getEntitySubType(), upgraded);
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
    FileInfo fileInfo = new FileType(Map.of("path", relYml.toString())).get(ldproxyCfg);

    if (debug && LOGGER.isDebugEnabled()) {
      LOGGER.debug("fileInfo: {} {}", fileInfo.isValid(), fileInfo.entitySubType);
    }

    if (!fileInfo.isValid() || fileInfo.entitySubType.isEmpty()) {
      return Optional.empty();
    }

    Identifier storeIdentifier =
        Identifier.from("defaults", fileInfo.entityType, fileInfo.entitySubType.get());

    Map<String, Object> original = ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);
    Map<String, Object> upgraded =
        getUpgradedDefaultsOrOverrides(
            ldproxyCfg, yml, storeIdentifier, Type.Defaults, fileInfo, original, debug);

    if (debug && LOGGER.isDebugEnabled()) {
      LOGGER.debug("original1: {}", original);
      LOGGER.debug("upgraded1: {}", upgraded);
    }

    if (fileInfo.subProperty.isPresent()) {
      try {
        if (fileInfo.discriminatorKey.isPresent() && fileInfo.discriminatorValue.isPresent()) {
          List<Map<String, Object>> sub =
              (List<Map<String, Object>>) upgraded.get(fileInfo.subProperty.get());
          upgraded =
              sub.stream()
                  .filter(
                      m ->
                          Objects.equals(
                              m.get(fileInfo.discriminatorKey.get()),
                              fileInfo.discriminatorValue.get()))
                  .findFirst()
                  .orElse(new LinkedHashMap<>(original));
          upgraded.remove(fileInfo.discriminatorKey.get());
        } else {
          upgraded = (Map<String, Object>) upgraded.get(fileInfo.subProperty.get());
        }
      } catch (Throwable e) {
        if (debug) {
          System.out.println("Error reading '" + relYml + "': " + e.getMessage());
        }
        upgraded = new LinkedHashMap<>(original);
      }
    }

    Map<String, String> diff = MapDiffer.diff(original, upgraded, true);

    if (debug) {
      System.out.println("DIFF " + diff);
    }

    if (force || !diff.isEmpty()) {
      return Optional.of(new Upgrade(Type.Defaults, relYml, original, upgraded, null, Map.of()));
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
    FileInfo fileInfo = new FileType(Map.of("path", relYml.toString())).get(ldproxyCfg);

    if (!fileInfo.isValid() || fileInfo.entitySubType.isEmpty()) {
      return Optional.empty();
    }

    Map<String, Object> original = ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);
    Map<String, Object> upgraded =
        getUpgradedDefaultsOrOverrides(
            ldproxyCfg, yml, identifier, Type.Overrides, fileInfo, original, debug);

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
      FileInfo fileInfo,
      Map<String, Object> original,
      boolean debug)
      throws IOException {
    String fileContent = Validation.loadFileContent(yml, type, fileInfo);

    if (debug && LOGGER.isDebugEnabled()) {
      LOGGER.debug("fileInfo: {} {}", fileInfo.entityType, fileInfo.entitySubType);
      LOGGER.debug("fileContent: {}", fileContent);
    }

    EntityDataBuilder<? extends EntityData> builder =
        ldproxyCfg
            .getEntityFactories()
            .get(fileInfo.entityType, fileInfo.entitySubType)
            .emptyDataBuilder()
            .fillRequiredFieldsWithPlaceholders();
    ldproxyCfg
        .getEntityDataStore()
        .getValueEncoding()
        .getMapper(ValueEncoding.FORMAT.YML)
        .readerForUpdating(builder)
        .readValue(fileContent);

    if (debug && LOGGER.isDebugEnabled()) {
      LOGGER.debug("builder: {}", builder.build());
    }

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
        if (entry.getKey().equals("providerType") && !original.containsKey("providerType")) {
          continue;
        }
        if (entry.getKey().equals("providerSubType") && !original.containsKey("providerSubType")) {
          continue;
        }
        if (entry.getKey().equals("serviceType") && !original.containsKey("serviceType")) {
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
