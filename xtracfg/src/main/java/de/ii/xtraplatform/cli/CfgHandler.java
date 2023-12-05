package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import shadow.com.fasterxml.jackson.annotation.JsonInclude;
import shadow.com.fasterxml.jackson.core.type.TypeReference;

public class CfgHandler {

  static final TypeReference<LinkedHashMap<String, Object>> AS_MAP =
      new TypeReference<LinkedHashMap<String, Object>>() {};

  static Result check(
      LdproxyCfg ldproxyCfg, boolean ignoreRedundant, boolean verbose, boolean debug) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();
    Map<Path, CfgValidation> validations = new LinkedHashMap<>();
    Map<Path, Migration> migrations = new LinkedHashMap<>();

    for (CfgValidation validation : getValidations(ldproxyCfg)) {
      validations.put(validation.getPath(), validation);
    }

    /*for (Upgrade upgrade : getUpgrades(ldproxyCfg, ignoreRedundant, false, debug)) {
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
    }*/

    for (CfgValidation validation : validations.values()) {
      validation.log(result, verbose);

      if (migrations.containsKey(validation.getPath())) {
        migrations.get(validation.getPath()).log(result, verbose);
      }
    }

    return result;
  }

  static Result preUpgrade(
      LdproxyCfg ldproxyCfg,
      boolean ignoreRedundant,
      boolean force,
      boolean verbose,
      boolean debug) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();

    for (CfgValidation validation : getValidations(ldproxyCfg)) {
      if (validation.getError().isPresent() || validation.hasErrors()) {
        validation.logErrors(result, verbose);
      }
    }

    int i = 0;
    for (Upgrade upgrade : getUpgrades(ldproxyCfg, ignoreRedundant, force, debug)) {
      if (upgrade.getError().isPresent()) {
        result.error(
            String.format("Could not read %s: %s", upgrade.getPath(), upgrade.getError().get()));
      } else if (upgrade.getUpgrade().isPresent()) {
        if (i++ == 0) {
          result.info("The following global configurations will be upgraded:");
        }
        result.info("  - " + upgrade.getPath());
      }
    }

    if (result.has(Result.Status.INFO)) {
      result.confirmation("Are you sure?");
    }

    return result;
  }

  static Result upgrade(
      LdproxyCfg ldproxyCfg,
      boolean doBackup,
      boolean ignoreRedundant,
      boolean force,
      boolean verbose,
      boolean debug) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();

    for (Upgrade upgrade : getUpgrades(ldproxyCfg, ignoreRedundant, force, debug)) {
      Path upgradePath = ldproxyCfg.getDataDirectory().resolve(upgrade.getPath());

      if (upgrade.getUpgrade().isPresent()) {
        boolean error = false;

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
                      "Global configuration backup created: %s",
                      ldproxyCfg.getDataDirectory().relativize(backup)));
            }
          } catch (IOException e) {
            error = true;
            result.error(String.format("Could not create backup %s: %s", backup, e.getMessage()));
          }
        }

        if (!error) {
          try {
            Map<String, Object> upgraded = upgrade.getUpgrade().get();

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
          result.success(String.format("Global configuration upgraded: %s", upgrade.getPath()));
        }
      }

      if (upgrade.getError().isPresent()) {
        result.error(String.format("Could not read %s: %s", upgradePath, upgrade.getError().get()));
      }
    }

    return result;
  }

  private static List<CfgValidation> getValidations(LdproxyCfg ldproxyCfg) {
    return List.of(getValidation(ldproxyCfg, ldproxyCfg.getDataDirectory().resolve("cfg.yml")));
  }

  private static List<Upgrade> getUpgrades(
      LdproxyCfg ldproxyCfg, boolean ignoreRedundant, boolean force, boolean debug) {
    Path yml = ldproxyCfg.getDataDirectory().resolve("cfg.yml");
    try {
      return getUpgrade(ldproxyCfg, yml, ignoreRedundant, force, debug)
          .map(List::of)
          .orElse(List.of());
    } catch (Throwable e) {
      if (debug) {
        System.err.println("Could not read " + yml);
        e.printStackTrace(System.err);
      }
      return List.of(
          new Upgrade(null, ldproxyCfg.getDataDirectory().relativize(yml), e.getMessage()));
    }
  }

  private static CfgValidation getValidation(LdproxyCfg ldproxyCfg, Path yml) {
    CfgValidation validation = new CfgValidation(yml);

    validation.validate(ldproxyCfg, yml);

    return validation;
  }

  private static Optional<Upgrade> getUpgrade(
      LdproxyCfg ldproxyCfg, Path yml, boolean ignoreRedundant, boolean force, boolean debug)
      throws IOException {
    Map<String, Object> original = ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);
    Map<String, Object> upgraded = ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);

    boolean isUpgraded = false;
    Map<String, Object> store = (Map<String, Object>) upgraded.getOrDefault("store", Map.of());
    Map<String, Object> proj = (Map<String, Object>) upgraded.getOrDefault("proj", Map.of());

    if (store.containsKey("additionalLocations")
        && store.get("additionalLocations") instanceof List
        && !((List<?>) store.get("additionalLocations")).isEmpty()) {

      List<String> additionalLocations = (List<String>) store.get("additionalLocations");
      List<Map<String, Object>> sources =
          (List<Map<String, Object>>)
              store.computeIfAbsent("sources", ignore -> new ArrayList<Map<String, Object>>());

      for (String additionalLocation : additionalLocations) {
        sources.add(Map.of("type", "FS", "content", "ENTITIES", "src", additionalLocation));
      }

      store.remove("additionalLocations");

      isUpgraded = true;
    }

    if (proj.containsKey("location")
        && proj.get("location") instanceof String
        && !((String) proj.get("location")).isBlank()) {

      String location = ((String) proj.get("location")).trim();

      if (!Objects.equals(location, "proj")) {
        List<Map<String, Object>> sources =
            (List<Map<String, Object>>)
                store.computeIfAbsent("sources", ignore -> new ArrayList<Map<String, Object>>());

        sources.add(
            Map.of("type", "FS", "content", "RESOURCES", "src", location, "prefix", "proj"));
      }

      upgraded.remove("proj");

      isUpgraded = true;
    }

    if (isUpgraded) {
      return Optional.of(
          new Upgrade(
              EntitiesHandler.Type.Entity,
              ldproxyCfg.getDataDirectory().relativize(yml),
              original,
              upgraded,
              null,
              Map.of()));
    }

    return Optional.empty();
  }
}
