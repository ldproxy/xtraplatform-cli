package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.entities.domain.AutoEntityFactory;
import de.ii.xtraplatform.entities.domain.EntityFactory;
import de.ii.xtraplatform.values.domain.AutoValue;
import de.ii.xtraplatform.values.domain.AutoValueFactory;
import de.ii.xtraplatform.values.domain.ValueFactories;
import de.ii.xtraplatform.values.domain.ValueFactory;


import java.util.*;
import java.util.function.Consumer;

public class AutoValueHandler {

    private static Map<String, String> collectionColors;
   // private static MbStyleStylesheetGenerator generator = new MbStyleStylesheetGenerator();





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







/*
    public static Result preCheck(Map<String, String> parameters) {
        AutoValueFactory valueFactory =
                getValueFactories(
                        ldproxyCfg, "mapLibreStyle");

     //   Result generatorPreCheckResult = valueFactory.preCheck(parameters);

        if (generatorPreCheckResult.isFailure()) {
            return generatorPreCheckResult;
        }

        return Result.empty();
    }
*/
    public static Result analyze(Map<String, String> parameters, LdproxyCfg ldproxyCfg) {
        ldproxyCfg.initStore();
        Result result = new Result();

        try {
            String apiId = parameters.get("apiId");

            AutoValueFactory valueFactory =
                    getValueFactories(
                            ldproxyCfg, "maplibre-styles");


            // Map<String, String> collectionColors = generator.analyze(apiId);
            Map<String, String> collectionColors = (Map<String, String>) valueFactory.analyze(apiId);


            // If the analyze method returns a non-empty map, add it to the result
            if (!collectionColors.isEmpty()) {
                result.success("Stylesheet information found");
                result.details("collectionColors", collectionColors);
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
        Result result = new Result();

        try {
            String apiId = parameters.get("apiId");
            AutoValueFactory valueFactory =
                    getValueFactories(
                            ldproxyCfg, "maplibre-styles");

            Map<String, String> collectionColors = (Map<String, String>) valueFactory.analyze(apiId);


            AutoValue stylesheet = valueFactory.generate(apiId, collectionColors);


            result.success("All good");
            result.details("stylesheet", stylesheet);
        } catch (Throwable e) {
            e.printStackTrace();
            if (Objects.nonNull(e.getMessage())) {
                result.error(e.getMessage());
            }
        }

        return result;
    }
}