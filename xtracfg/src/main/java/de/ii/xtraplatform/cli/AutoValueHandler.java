package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.ogcapi.styles.domain.MbStyleStylesheet;
import de.ii.xtraplatform.values.domain.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import shadow.com.fasterxml.jackson.core.type.TypeReference;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;
import shadow.com.google.common.base.Strings;

public class AutoValueHandler {

  private static final Set<String> ALLOWED_TYPES = Set.of("maplibre-styles");

  public static Result preCheck(Map<String, String> parameters, LdproxyCfg ldproxyCfg) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    ldproxyCfg.initStore();

    String type = parameters.get("type");
    String name = parameters.get("name");
    String apiId = parameters.get("apiId");

    if (Strings.isNullOrEmpty(type)) {
      return Result.failure("No value type given");
    }

    if (!ALLOWED_TYPES.contains(type)) {
      return Result.failure(String.format("Value type '%s' is not supported", type));
    }

    if (Strings.isNullOrEmpty(name)) {
      return Result.failure("No name given");
    }

    if (Strings.isNullOrEmpty(apiId)) {
      return Result.failure("No API id given");
    }

    if (ldproxyCfg.hasValue(type, name, apiId)) {
      return Result.failure(
          String.format(
              "Value of type '%s' with name '%s' already exists for API '%s'", type, name, apiId));
    }

    return Result.empty();
  }

  public static Result analyze(Map<String, String> parameters, LdproxyCfg ldproxyCfg) {
    Result result = new Result();

    try {
      String apiId = parameters.get("apiId");

      AutoValueFactory<MbStyleStylesheet, String, Map<String, String>> valueFactory =
          (AutoValueFactory<MbStyleStylesheet, String, Map<String, String>>)
              getValueFactory(ldproxyCfg, "maplibre-styles");

      Map<String, String> collectionColors = valueFactory.analyze(apiId);

      if (!collectionColors.isEmpty()) {
        result.success("All good");
        result.details("Collection Colors", collectionColors);
      } else {
        result.success("No stylesheet information found");
      }
    } catch (Throwable e) {
      e.printStackTrace();
      if (Objects.nonNull(e.getMessage())) {
        result.error(e.getMessage());
      }
    }

    return result;
  }

  public static Result generate(
      Map<String, String> parameters,
      LdproxyCfg ldproxyCfg,
      Consumer<Result> tracker,
      ObjectMapper jsonMapper) {
    AutoValueFactory<MbStyleStylesheet, String, Map<String, String>> valueFactory =
        (AutoValueFactory<MbStyleStylesheet, String, Map<String, String>>)
            getValueFactory(ldproxyCfg, "maplibre-styles");
    Result result = new Result();

    try {
      String type = parameters.get("type");
      String apiId = parameters.get("apiId");
      String name = parameters.get("name");
      String collectionColorsString = parameters.get("collectionColors");
      boolean isTest = Objects.equals(collectionColorsString, "TEST");

      if (Strings.isNullOrEmpty(collectionColorsString) && !isTest) {
        return Result.failure("No collections given");
      }

      Map<String, String> collectionColors =
          isTest
              ? valueFactory.analyze(apiId)
              : jsonMapper.readValue(
                  collectionColorsString, new TypeReference<Map<String, String>>() {});

      MbStyleStylesheet stylesheet = valueFactory.generate(apiId, collectionColors);

      ldproxyCfg.writeValue(stylesheet, name, apiId);

      Path newPath = ldproxyCfg.getValuesPath().resolve(Path.of(type, apiId, name + ".json"));

      result.success(
          String.format("Value of type '%s' was created successfully: %s", type, newPath));
    } catch (Throwable e) {
      e.printStackTrace();
      if (Objects.nonNull(e.getMessage())) {
        result.error(e.getMessage());
      }
    }

    return result;
  }

  private static AutoValueFactory<?, ?, ?> getValueFactory(
      LdproxyCfg ldproxyCfg, String valueType) {
    ValueFactory factory = ldproxyCfg.getValueFactories().get(valueType);

    AutoValueFactory<?, ?, ?> valueFactory =
        factory
            .auto()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No value factory found for value type " + valueType));
    return valueFactory;
  }
}
