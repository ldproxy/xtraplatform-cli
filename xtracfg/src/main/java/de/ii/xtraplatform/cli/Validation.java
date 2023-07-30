package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.DeprecatedKeyword;
import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.store.app.entities.MapSubtractor;
import de.ii.xtraplatform.store.domain.Identifier;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import shadow.com.networknt.schema.ValidationMessage;
import shadow.com.networknt.schema.ValidatorTypeCode;

public class Validation {

  private final Entities.Type type;
  private final Path path;
  private final Identifier identifier;
  private String error;
  private final Set<ValidationMessage> validationMessages;

  public Validation(Entities.Type type, Identifier identifier, Path path) {
    this(type, identifier, path, null);
  }

  public Validation(Entities.Type type, Identifier identifier, Path path, String error) {
    this.type = type;
    this.identifier = identifier;
    this.path = path;
    this.error = error;
    this.validationMessages = new LinkedHashSet<>();
  }

  public Entities.Type getType() {
    return type;
  }

  public Path getPath() {
    return path;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public Optional<String> getError() {
    return Optional.ofNullable(error);
  }

  public List<String> getValidationErrors() {
    return validationMessages.stream()
        .filter(Validation::isError)
        .map(Validation::getMessage)
        .collect(Collectors.toList());
  }

  public List<String> getValidationWarnings() {
    return validationMessages.stream()
        .filter(Validation::isWarning)
        .map(Validation::getMessage)
        .collect(Collectors.toList());
  }

  public boolean hasValidationErrors() {
    return !getValidationErrors().isEmpty();
  }

  public boolean hasValidationWarnings() {
    return !getValidationWarnings().isEmpty();
  }

  public void addValidationMessages(Set<ValidationMessage> validationMessages) {
    this.validationMessages.addAll(validationMessages);
  }

  private String getQualifiers() {
    if (hasValidationErrors()) {
      return "has errors";
    } else if (hasValidationWarnings()) {
      List<String> kinds = new ArrayList<>();
      if (validationMessages.stream().anyMatch(Validation::isUnknown)) {
        kinds.add("unknown");
      }
      if (validationMessages.stream()
          .anyMatch(vm -> DeprecatedKeyword.isDeprecated(vm) || isMigration(vm))) {
        kinds.add("deprecated");
      }
      if (validationMessages.stream().anyMatch(Validation::isRedundant)) {
        kinds.add("redundant");
      }

      return String.format("has %s settings", String.join(" and ", kinds));
    }
    return "is fine";
  }

  public String getSummary() {
    return String.format("%s configuration %s: %s", getType().name(), getQualifiers(), getPath());
  }

  public void validate(LdproxyCfg ldproxyCfg) {
    try {
      if (getType() == Entities.Type.Entity) {
        validationMessages.addAll(
            ldproxyCfg.validateEntity(
                ldproxyCfg.getDataDirectory().resolve(getPath()),
                identifier.path().get(identifier.path().size() - 1)));
      }
    } catch (IOException e) {
      this.error = e.getMessage();
    }
  }

  public void validateRedundant(Map<String, Object> original, Map<String, Object> upgraded) {
    Map<String, Object> redundant = MapSubtractor.subtract(original, upgraded, List.of(), true);
    List<String> paths = paths(redundant, "");

    List<String> previousPaths =
        this.validationMessages.stream()
            .flatMap(
                vm -> {
                  String path = vm.getMessage().substring(2, vm.getMessage().indexOf(":"));
                  // allow single values instead of list like yaml parser
                  return Stream.of(path, path.replaceAll("\\[0\\]", ""));
                })
            .collect(Collectors.toList());

    for (String path : paths) {
      if (!previousPaths.contains(path)
          && !previousPaths.contains(path.replaceAll("\\[0\\]", ""))) {
        validationMessages.add(redundant(path));
      }
    }
  }

  public void log(Result result, boolean verbose) {
    if (getError().isPresent()) {
      result.error(String.format("Could not read %s: %s", getPath(), getError().get()));
      return;
    }

    if (hasValidationErrors()) {
      result.error(getSummary());
      getValidationErrors().forEach(message -> result.error("  - " + message));
    } else if (hasValidationWarnings()) {
      result.warning(getSummary());

      if (verbose) {
        getValidationWarnings().forEach(message -> result.warning("  - " + message));
      }
    } else if (verbose) {
      result.success(getSummary());
    }
  }

  public void logErrors(Result result, boolean verbose) {
    if (getError().isPresent()) {
      result.error(String.format("Could not read %s: %s", getPath(), getError().get()));
      return;
    }

    if (hasValidationErrors()) {
      result.error(getSummary());
      if (verbose) {
        getValidationErrors().forEach(message -> result.error("  - " + message));
      }
    }
  }

  private static List<String> paths(Map<String, Object> map, String prefix) {
    List<String> paths = new ArrayList<>();

    for (String key : map.keySet()) {
      String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
      if (map.get(key) instanceof Map) {
        paths.addAll(paths((Map<String, Object>) map.get(key), newPrefix));
      } else if (map.get(key) instanceof List) {
        for (int i = 0; i < ((List<Object>) map.get(key)).size(); i++) {
          Object item = ((List<Object>) map.get(key)).get(i);
          if (item instanceof Map) {
            paths.addAll(paths((Map<String, Object>) item, newPrefix + "[" + i + "]"));
          } else if (Objects.nonNull(item)) {
            paths.add(newPrefix);
          }
        }
      } else if (!newPrefix.endsWith("buildingBlock") && Objects.nonNull(map.get(key))) {
        paths.add(newPrefix);
      }
    }

    return paths;
  }

  static boolean isError(ValidationMessage vm) {
    return !isWarning(vm);
  }

  private static boolean isWarning(ValidationMessage vm) {
    return DeprecatedKeyword.isDeprecated(vm)
        || isUnknown(vm)
        || isRedundant(vm)
        || isMigration(vm);
  }

  private static boolean isUnknown(ValidationMessage vm) {
    return Objects.equals(vm.getCode(), ValidatorTypeCode.ADDITIONAL_PROPERTIES.getErrorCode());
  }

  private static boolean isRedundant(ValidationMessage vm) {
    return Objects.equals(vm.getCode(), REDUNDANT);
  }

  private static boolean isMigration(ValidationMessage vm) {
    return Objects.equals(vm.getCode(), MIGRATION);
  }

  private static String getMessage(ValidationMessage vm) {
    if (isUnknown(vm)) {
      return String.format(
          "%s is unknown for type %s",
          vm.getMessage().substring(0, vm.getMessage().indexOf(":") + 1),
          vm.getSchemaPath().replace("#/$defs/", "").replace("/additionalProperties", ""));
    }

    return vm.getMessage();
  }

  private static final String REDUNDANT = "redundant";

  static ValidationMessage redundant(String path) {
    return new ValidationMessage.Builder()
        .code(REDUNDANT)
        .path(path)
        .format(new MessageFormat("$.{0}: is redundant and can be removed"))
        .build();
  }

  private static final String MIGRATION = "migration";

  static ValidationMessage migration(String path, String message) {
    return new ValidationMessage.Builder()
        .code(REDUNDANT)
        .path(path)
        .arguments(message)
        .format(new MessageFormat("$.{0}: a migration is available - {1}"))
        .build();
  }
}
