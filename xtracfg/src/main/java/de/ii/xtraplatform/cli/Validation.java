package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.DeprecatedKeyword;
import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.entities.app.MapSubtractor;
import de.ii.xtraplatform.values.domain.Identifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import shadow.com.networknt.schema.ValidationMessage;
import shadow.com.networknt.schema.ValidatorTypeCode;

public class Validation extends Messages {

  public Validation(EntitiesHandler.Type type, Identifier identifier, Path path) {
    super(type, identifier, path);
  }

  public Validation(EntitiesHandler.Type type, Identifier identifier, Path path, String error) {
    super(type, identifier, path, error);
  }

  @Override
  public String getSummary() {
    return String.format("%s configuration %s: %s", getType().name(), getQualifiers(), getPath());
  }

  private String getQualifiers() {
    if (hasErrors()) {
      return "has errors";
    } else if (hasWarnings()) {
      List<String> kinds = new ArrayList<>();
      if (getMessages().stream().anyMatch(Validation::isUnknown)) {
        kinds.add("unknown");
      }
      if (getMessages().stream().anyMatch(DeprecatedKeyword::isDeprecated)) {
        kinds.add("deprecated");
      }
      if (getMessages().stream().anyMatch(Validation::isRedundant)) {
        kinds.add("redundant");
      }

      return String.format("has %s settings", String.join(" and ", kinds));
    }
    return "is fine";
  }

  public void validate(LdproxyCfg ldproxyCfg, Map<String, String> fileType) {
    if (!fileType.containsKey("entityType")) {
      return;
    }

    try {
      String fileContent =
          loadFileContent(ldproxyCfg.getDataDirectory().resolve(getPath()), getType(), fileType);

      for (ValidationMessage msg :
          ldproxyCfg.validateEntity(fileContent, fileType.get("entityType"))) {
        if ( // msg.getMessage().contains("string found, boolean expected") ||
        // msg.getMessage().contains("integer found, string expected") ||
        msg.getMessage().contains(".tileProviderId: is deprecated")
            || (getType() == EntitiesHandler.Type.Defaults
                && (
                /*msg.getMessage().contains("$.serviceType: is missing but it is required")
                || msg.getMessage().contains("$.providerType: is missing but it is required")
                ||*/ msg.getMessage()
                        .contains("$.providerSubType: is missing but it is required")
                    || msg.getMessage()
                        .contains("$.featureProviderType: is missing but it is required")))) {
          // ignore
          continue;
        }
        addMessage(msg);
      }
    } catch (IOException e) {
      setError(e.getMessage());
    }
  }

  static String loadFileContent(Path path, EntitiesHandler.Type type, Map<String, String> fileType)
      throws IOException {
    String fileContent = Files.readString(path);
    String entityType = fileType.get("entityType");

    if (type == EntitiesHandler.Type.Defaults && fileType.containsKey("entitySubType")) {

      System.out.println("fileContent: " + fileContent);

      // TODO: if first line is ---, remove
      if (fileContent.startsWith("---\n")) {
        fileContent = fileContent.substring(4);
      }

      // TODO: if fileType contains discriminatorKey/discriminatorValue, make file content array
      // entry and add the key/value pair to array
      if (fileType.containsKey("discriminatorKey") && fileType.containsKey("discriminatorValue")) {
        String discriminatorKey = fileType.get("discriminatorKey");
        String discriminatorValue = fileType.get("discriminatorValue");
        fileContent =
            "- "
                + discriminatorKey
                + ": "
                + discriminatorValue
                + "\n  "
                + fileContent.replace("\n", "\n  ");
      }

      // TODO: if fileType contains subproperty, indent all lines and prepend subProperty as key
      if (fileType.containsKey("subProperty")) {
        String subProperty = fileType.get("subProperty");
        fileContent =
            Arrays.stream(fileContent.split("\n"))
                .map(line -> "  " + line)
                .collect(Collectors.joining("\n"));
        fileContent = subProperty + ":\n" + fileContent;
      }

      fileContent =
          fileContent
              + "\n"
              + entityType.substring(0, entityType.length() - 1)
              + "Type: "
              + fileType.get("entitySubType").toUpperCase();

      System.out.println("newfileContent: " + fileContent);

      // TODO: if first line is ---, remove
      // TODO: if fileType contains subproperty, indent all lines and prepend subProperty as key
      // TODO: if fileType contains discriminatorKey/discriminatorValue, make original content
      // array entry and add the key/value pair to array
    }

    if (type == EntitiesHandler.Type.Overrides && fileType.containsKey("entitySubType")) {
      fileContent =
          fileContent
              + "\n"
              + entityType.substring(0, entityType.length() - 1)
              + "Type: "
              + fileType.get("entitySubType").toUpperCase()
              + "\n";
    }
    return fileContent;
  }

  public void validateRedundant(Map<String, Object> original, Map<String, Object> upgraded) {
    // TODO
    Map<String, Object> redundant =
        MapSubtractor.subtract(
            original,
            upgraded,
            List.of(),
            Map.of("api", "buildingBlock", "extensions", "type"),
            true);
    List<String> redundantPath = paths(redundant, "");
    List<String> upgradedPaths = paths(upgraded, "");

    List<String> coveredPaths =
        getMessages().stream()
            .flatMap(
                vm -> {
                  String path = vm.getMessage().substring(2, vm.getMessage().indexOf(":"));
                  // allow single values instead of list like yaml parser
                  return Stream.of(path, path.replaceAll("\\[0\\]", ""));
                })
            .collect(Collectors.toList());

    for (String path : redundantPath) {
      // allow single values instead of list like yaml parser
      String pathSingles = path.replaceAll("\\[0\\]", "");
      if (!coveredPaths.contains(path)
          && !coveredPaths.contains(pathSingles)
          && !upgradedPaths.contains(path)
          && !upgradedPaths.contains(pathSingles)) {

        if (coveredPaths.stream()
            .anyMatch(cp -> path.startsWith(cp + ".") || pathSingles.startsWith(cp + "."))) {
          continue;
        }

        addMessage(redundant(path));
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

  @Override
  protected boolean isWarning(ValidationMessage vm) {
    return DeprecatedKeyword.isDeprecated(vm) || isUnknown(vm) || isRedundant(vm);
  }

  @Override
  protected String getMessage(ValidationMessage vm) {
    if (isUnknown(vm)) {
      return String.format(
          "%s is unknown for type %s",
          vm.getMessage().substring(0, vm.getMessage().indexOf(":") + 1),
          vm.getSchemaPath().replace("#/$defs/", "").replace("/additionalProperties", ""));
    }

    return vm.getMessage();
  }

  private static boolean isUnknown(ValidationMessage vm) {
    return Objects.equals(vm.getCode(), ValidatorTypeCode.ADDITIONAL_PROPERTIES.getErrorCode());
  }

  private static boolean isRedundant(ValidationMessage vm) {
    return Objects.equals(vm.getCode(), REDUNDANT);
  }

  private static final String REDUNDANT = "redundant";

  static ValidationMessage redundant(String path) {
    return new ValidationMessage.Builder()
        .code(REDUNDANT)
        .path(path)
        .format(new MessageFormat("$.{0}: is redundant and can be removed"))
        .build();
  }
}
