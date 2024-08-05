package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.values.domain.AutoValue;
import de.ii.xtraplatform.values.domain.AutoValueFactory;
import de.ii.xtraplatform.values.domain.StoredValue;
import de.ii.xtraplatform.values.domain.ValueFactory;
import java.nio.file.Path;

import java.util.*;
import java.util.function.Consumer;

public class AutoValueHandler {

    private static Map<String, String> collectionColors;


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


            // If the analyze method returns a non-empty map, add it to the result

            if (!collectionColors.isEmpty()) {
             //   String collectionColorsString = collectionColors.toString();
             //   result.success(collectionColorsString);
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
            Consumer<Result> tracker) {
        ldproxyCfg.initStore();

        Result result = new Result();

        try {
            String apiId = parameters.get("apiId");
            String name = parameters.get("name");
            String collectionColorsString = parameters.get("collectionColors");
            System.out.println("colle" + collectionColorsString);

            AutoValueFactory valueFactory =
                    getValueFactories(
                            ldproxyCfg, "maplibre-styles");


            // Convert the collectionColorsString back to a Map
            if (collectionColorsString != null) {
                Map<String, String> collectionColors = new HashMap<>();
            String[] entries = collectionColorsString.substring(1, collectionColorsString.length() - 1).split(", ");
            for (String entry : entries) {
                String[] keyValue = entry.split("=");
                if (keyValue.length == 2) {
                    collectionColors.put(keyValue[0], keyValue[1]);
                } else {
                    System.out.println("Invalid entry: " + entry);
                }
            }
            System.out.println("collogne" + collectionColors);
                if (collectionColorsString != null) {
                    System.out.println("collectionColorsString is null");
                }
                }




                    AutoValue stylesheet = valueFactory.generate(apiId, collectionColors);
            ldproxyCfg.writeValue((StoredValue)stylesheet, name,apiId);
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