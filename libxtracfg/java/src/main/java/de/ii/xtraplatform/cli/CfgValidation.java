package de.ii.xtraplatform.cli;

import static de.ii.xtraplatform.cli.CfgHandler.AS_MAP;

import de.ii.ldproxy.cfg.DeprecatedKeyword;
import de.ii.ldproxy.cfg.LdproxyCfg;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import shadow.com.networknt.schema.ValidationMessage;
import shadow.com.networknt.schema.ValidatorTypeCode;

public class CfgValidation extends Messages {

  public CfgValidation(Path path) {
    super(null, null, path);
  }

  public CfgValidation(Path path, String error) {
    super(null, null, path, error);
  }

  @Override
  public String getSummary() {
    return String.format("Global configuration %s: %s", getQualifiers(), getPath());
  }

  private String getQualifiers() {
    if (hasErrors()) {
      return "has errors";
    } else if (hasWarnings()) {
      List<String> kinds = new ArrayList<>();
      if (getMessages().stream().anyMatch(CfgValidation::isUnknown)) {
        kinds.add("unknown");
      }
      if (getMessages().stream().anyMatch(DeprecatedKeyword::isDeprecated)) {
        kinds.add("deprecated");
      }
      if (getMessages().stream().anyMatch(CfgValidation::isRedundant)) {
        kinds.add("redundant");
      }

      return String.format("has %s settings", String.join(" and ", kinds));
    }
    return "is fine";
  }

  public void validate(LdproxyCfg ldproxyCfg, Path yml) {
    try {
      Map<String, Object> original = ldproxyCfg.getObjectMapper().readValue(yml.toFile(), AS_MAP);

      if (original.containsKey("store") && (original.get("store") instanceof Map)) {
        Map<String, Object> store = (Map<String, Object>) original.get("store");

        if (store.containsKey("additionalLocations")
            && store.get("additionalLocations") instanceof List
            && !((List<?>) store.get("additionalLocations")).isEmpty()) {
          addMessage(deprecated("store.additionalLocations"));
        }
      }

      if (original.containsKey("proj") && (original.get("proj") instanceof Map)) {
        Map<String, Object> proj = (Map<String, Object>) original.get("proj");

        if (proj.containsKey("location")
                && proj.get("location") instanceof String
                && !((String) proj.get("location")).isBlank()) {
          addMessage(deprecated("proj.location"));
        }
      }
    } catch (IOException e) {
      setError(e.getMessage());
    }
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

  static ValidationMessage deprecated(String path) {
    return new ValidationMessage.Builder()
        .code(DeprecatedKeyword.KEYWORD)
        .path(path)
        .format(new MessageFormat("$.{0}: is deprecated and should be upgraded"))
        .build();
  }
}
