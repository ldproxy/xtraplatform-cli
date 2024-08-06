package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.values.domain.*;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;
import shadow.com.fasterxml.jackson.core.type.TypeReference;

import java.nio.file.Path;

import java.util.*;
import java.util.function.Consumer;



public class AutoValueHandler {
    private static AutoValueFactory getValueFactories(
            LdproxyCfg ldproxyCfg, String valueType) {
        ValueFactory factory = ldproxyCfg.getValueFactories().get(valueType);

        AutoValueFactory valueFactory =
                factory
                        .auto()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No value factory found for value type " + valueType));
        return valueFactory;
    }


    public static Result analyze(Map<String, String> parameters, LdproxyCfg ldproxyCfg) {
        ldproxyCfg.initStore();
        Result result = new Result();

        try {
            String apiId = parameters.get("apiId");

            AutoValueFactory valueFactory =
                    getValueFactories(
                            ldproxyCfg, "maplibre-styles");


            Map<String, String> collectionColors = (Map<String, String>) valueFactory.analyze(apiId);
            System.out.println("colle1" + collectionColors);

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
            Map<String, String> parameters, LdproxyCfg ldproxyCfg,
            Consumer<Result> tracker, ObjectMapper jsonMapper) {
        AutoValueFactory valueFactory =
                getValueFactories(
                        ldproxyCfg, "maplibre-styles");
        ldproxyCfg.initStore();

        Result result = new Result();

        try {
            String apiId = parameters.get("apiId");
            String name = parameters.get("name");
            String collectionColorsString = parameters.get("collectionColors");

            Map<String, String> collectionColorMap = jsonMapper.readValue(collectionColorsString, new TypeReference<Map<String, String>>(){});
            System.out.println("myMap" + collectionColorMap);

            AutoValue stylesheet = valueFactory.generate(apiId, collectionColorMap);

            ldproxyCfg.writeValue((StoredValue)stylesheet,name,apiId);
            System.out.println("Stylesheet" + stylesheet);

            Path path = ldproxyCfg.getEntitiesPath();
            Path parentPath = path.getParent();
            Path newPath = parentPath.resolve("values");

            result.success("Value was created successfully: " + newPath.toString());
        } catch (Throwable e) {
            e.printStackTrace();
            if (Objects.nonNull(e.getMessage())) {
                result.error(e.getMessage());
            }
        }

        return result;
    }
}