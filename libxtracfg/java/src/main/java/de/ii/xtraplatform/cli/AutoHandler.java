package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.cli.AutoTypes.EntityType;
import de.ii.xtraplatform.cli.AutoTypes.FeatureProviderType;
import de.ii.xtraplatform.cli.AutoTypes.ProviderType;
import de.ii.xtraplatform.cli.AutoTypes.ServiceType;
import de.ii.xtraplatform.entities.domain.AutoEntityFactory;
import de.ii.xtraplatform.entities.domain.EntityFactory;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.gml.domain.ImmutableFeatureProviderWfsData;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData;
import de.ii.xtraplatform.tiles.domain.TileProviderFeaturesData;
import de.ii.xtraplatform.values.domain.Identifier;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map;
import java.util.function.Consumer;

public class AutoHandler {
    public static Result preCheck(Map<String, String> parameters, LdproxyCfg ldproxyCfg) {
        if (Objects.isNull(ldproxyCfg)) {
            return Result.failure("Not connected to store");
        }

        ldproxyCfg.initStore();

        if (!parameters.containsKey("id")) {
            return Result.failure("No id given");
        }

        String id = parameters.get("id");

        if (id.length() < 3) {
            return Result.failure("Id has to be at least 3 characters long");
        }

        Identifier identifier = Identifier.from(id, "providers");

        if (ldproxyCfg.getEntityDataStore().has(identifier)) {
            return Result.failure("A provider with id '" + id + "' already exists");
        }

        if (parameters.containsKey("featureProviderType")
                && parameters.get("featureProviderType").equals("PGIS")
                && (!parameters.containsKey("host") || parameters.get("host").isBlank())) {
            return Result.failure("Host is required for PGIS connection");
        }

        if (parameters.containsKey("featureProviderType")
                && parameters.get("featureProviderType").equals("WFS")) {
            if (!parameters.containsKey("url") || parameters.get("url").isBlank()) {
                return Result.failure("URL is required for WFS connection");
            }
            try {
                URI uri = URI.create(parameters.get("url"));
                if (!Objects.equals(uri.getScheme(), "http") && !Objects.equals(uri.getScheme(), "https")) {
                    return Result.failure("Invalid URL scheme for WFS connection");
                }
            } catch (IllegalArgumentException e) {
                return Result.failure("Invalid URL for WFS connection");
            }
        }

        return Result.empty();
    }

    public static Result check(
            Map<String, String> parameters,
            LdproxyCfg ldproxyCfg,
            Optional<String> path,
            boolean verbose,
            boolean debug) {

        // TODO: check connectionInfo

        return Result.ok("All good");
    }

    public static Result analyze(
            Map<String, String> parameters,
            LdproxyCfg ldproxyCfg,
            Optional<String> path,
            boolean verbose,
            boolean debug) {
        Result result = new Result();

        try {
            FeatureProviderDataV2 featureProvider =
                    parseFeatureProvider(parameters, ldproxyCfg, Map.of());

            AutoEntityFactory autoFactory =
                    getAutoFactory(
                            ldproxyCfg, EntityType.PROVIDERS.toString(), featureProvider.getEntitySubType());

            Map<String, List<String>> types = autoFactory.analyze(featureProvider);

            result.success("All good");
            result.details("types", types);
        } catch (Throwable e) {
            e.printStackTrace();
            if (e instanceof RuntimeException
                    && Objects.nonNull(e.getCause())
                    && Objects.nonNull(e.getCause().getMessage())) {
                result.error(e.getCause().getMessage());
            } else if (Objects.nonNull(e.getMessage())) {
                result.error(e.getMessage());
            }
        }

        return result;
    }

    public static Result generate(
            Map<String, String> parameters,
            LdproxyCfg ldproxyCfg,
            Optional<String> path,
            boolean verbose,
            boolean debug,
            Map<String, List<String>> types,
            Consumer<Result> tracker) {
        Result result = new Result();

        System.out.println("Initial types: " + types);

        try {
            String createOption = parameters.get("createOption");
            String selectedConfig = parameters.get("selectedConfig");
            String newId = parameters.get("id");

            if ("fromScratch".equalsIgnoreCase(createOption)) {
                return generateBasicEntity(parameters, ldproxyCfg);
            }

            if ("copy".equalsIgnoreCase(createOption)) {
                if (selectedConfig == null || selectedConfig.isBlank()) {
                    return Result.failure("No selectedConfig provided in parameters");
                }
                if (newId == null || newId.isBlank()) {
                    return Result.failure("No id provided in parameters");
                }

                File sourceFile = new File(selectedConfig);
                if (!sourceFile.exists()) {
                    return Result.failure("Selected config file does not exist: " + selectedConfig);
                }

                Map<String, Object> yamlContent =
                        ldproxyCfg.getObjectMapper().readValue(sourceFile, Map.class);

                if (yamlContent.containsKey("id")) {
                    yamlContent.put("id", newId);
                }

                File targetFile = new File(new File(selectedConfig).getParent(), newId + ".yml");
                ldproxyCfg.getObjectMapper().writeValue(targetFile, yamlContent);

                result.success("Config copied successfully");
                result.details("new_files", List.of(targetFile.getPath()));
                return result;
            }

            if (types == null || types.isEmpty()) {
                if (selectedConfig == null || selectedConfig.isBlank()) {
                    return Result.failure("No selectedConfig provided in parameters");
                }

                Map<String, Object> cfgJava =
                        ldproxyCfg.getObjectMapper().readValue(new File(selectedConfig), Map.class);

                types = determineTypes(cfgJava);
            }

            System.out.println("New types: " + types);

            long count = types.values().stream().mapToLong(List::size).sum();

            Consumer<Map<String, List<String>>> tracker2 =
                    progress -> {
                        long current = Math.max(0, progress.values().stream().mapToLong(List::size).sum() - 1);
                        String currentTable =
                                progress.entrySet().stream()
                                        .skip(progress.size() - 1)
                                        .map(
                                                entry ->
                                                        entry.getKey()
                                                                + "."
                                                                + entry.getValue().get(entry.getValue().size() - 1))
                                        .findFirst()
                                        .orElse("???");

                        System.out.println("PROGRESS " + current + "/" + count + " " + currentTable);

                        Map<String, Object> details =
                                Map.of(
                                        "currentCount",
                                        current,
                                        "targetCount",
                                        count,
                                        "currentTable",
                                        currentTable,
                                        "progress",
                                        progress);

                        System.out.println("details: " + details);
                        tracker.accept(Result.ok("progress", details));
                    };

            Map<String, Boolean> typeObject = parseTypeObject(parameters.get("typeObject"));
            System.out.println("typeObject: " + typeObject);

            List<String> newFiles = new ArrayList<>();

            if (typeObject.getOrDefault("provider", true)) {
                FeatureProviderDataV2 featureProvider = parseFeatureProvider(parameters, ldproxyCfg, types);

                AutoEntityFactory autoFactory =
                        getAutoFactory(
                                ldproxyCfg, EntityType.PROVIDERS.toString(), featureProvider.getEntitySubType());

                FeatureProviderDataV2 entityData = autoFactory.generate(featureProvider, types, tracker2);

                ldproxyCfg.writeEntity(entityData);

                newFiles.add(
                        ldproxyCfg
                                .getDataDirectory()
                                .relativize(ldproxyCfg.getEntityPath(entityData).normalize())
                                .toString());
            }

            if (typeObject.getOrDefault("service", true)) {
                OgcApiDataV2 ogcApi = parseOgcApi(parameters, ldproxyCfg);

                AutoEntityFactory autoFactory2 =
                        getAutoFactory(ldproxyCfg, EntityType.SERVICES.toString(), ogcApi.getEntitySubType());

                Map<String, List<String>> types2 = types.containsKey("")
                        ? types
                        : Map.of("", new ArrayList<>(types.keySet()));

                System.out.println("types2" + types2); // {=[flurstueck]} instead of {ave=[Flurstueck]}

                OgcApiDataV2 entityData2 = autoFactory2.generate(ogcApi, types2, ignore -> {
                });

                ldproxyCfg.writeEntity(entityData2);

                newFiles.add(
                        ldproxyCfg
                                .getDataDirectory()
                                .relativize(ldproxyCfg.getEntityPath(entityData2).normalize())
                                .toString());
            }

      /*TODO
      TileProviderFeaturesData tileProvider = parseTileProvider(parameters, ldproxyCfg);

      AutoEntityFactory autoFactory3 =
          getAutoFactory(
              ldproxyCfg, EntityType.PROVIDERS.toString(), tileProvider.getEntitySubType());

      TileProviderFeaturesData entityData3 =
          autoFactory3.generate(tileProvider, types2, ignore -> {});*/

            result.success("All good");

            if (!newFiles.isEmpty()) {
                result.details("new_files", newFiles);
            }
            return result;
        } catch (Throwable e) {
            e.printStackTrace();
            return Result.failure("Unexpected error: " + e.getMessage());
        }
    }

    private static AutoEntityFactory getAutoFactory(
            LdproxyCfg ldproxyCfg, String type, Optional<String> subType) {
        EntityFactory factory = ldproxyCfg.getEntityFactories().get(type, subType);

        AutoEntityFactory autoFactory =
                factory
                        .auto()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No auto factory found for entity type " + factory.fullType()));
        return autoFactory;
    }

    private static FeatureProviderDataV2 parseFeatureProvider(
            Map<String, String> parameters, LdproxyCfg ldproxyCfg, Map<String, List<String>> types) {
        if (!parameters.containsKey("id")) {
            throw new IllegalArgumentException("No id given");
        }
        if (parameters.get("id").length() < 3) {
            throw new IllegalArgumentException("Id has to be at least 3 characters long");
        }

        String id = parameters.get("id");

        String featureProviderTypeString = parameters.get("featureProviderType");

        FeatureProviderType featureProviderType;
        try {
            featureProviderType = FeatureProviderType.valueOf(featureProviderTypeString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid featureProviderType: "
                            + featureProviderTypeString
                            + ". Expected one of: "
                            + Arrays.toString(FeatureProviderType.values()),
                    e);
        }

        System.out.println("new featureProviderType: " + featureProviderType);
        System.out.println("myParameters: " + parameters);

        FeatureProviderDataV2.Builder<?> builder =
                AutoTypes.getBuilder(
                                ldproxyCfg, EntityType.PROVIDERS, ProviderType.FEATURE, featureProviderType)
                        .id(id);

        switch (featureProviderType) {
            case PGIS:
                return parseFeatureProviderPgis(
                        (ImmutableFeatureProviderSqlData.Builder) builder, parameters, types);
            case GPKG:
                return parseFeatureProviderGpkg(
                        (ImmutableFeatureProviderSqlData.Builder) builder, parameters);
            case WFS:
                return parseFeatureProviderWfs(
                        (ImmutableFeatureProviderWfsData.Builder) builder, parameters);
        }

        return builder.build();
    }

    private static FeatureProviderDataV2 parseFeatureProviderPgis(
            ImmutableFeatureProviderSqlData.Builder builder,
            Map<String, String> parameters,
            Map<String, List<String>> types) {
        Set<String> schemas = types.keySet();

        builder
                .connectionInfoBuilder()
                .dialect(ConnectionInfoSql.Dialect.PGIS.name())
                .host(Optional.ofNullable(parameters.get("host")))
                .database(parameters.get("database"))
                .user(Optional.ofNullable(parameters.get("user")))
                .password(
                        Optional.ofNullable(parameters.get("password"))
                                .map(pw -> Base64.getEncoder().encodeToString(pw.getBytes(StandardCharsets.UTF_8))))
                .schemas(schemas)
                .poolBuilder();

        return builder.build();
    }

    private static FeatureProviderDataV2 parseFeatureProviderGpkg(
            ImmutableFeatureProviderSqlData.Builder builder, Map<String, String> parameters) {

        builder
                .connectionInfoBuilder()
                .dialect(ConnectionInfoSql.Dialect.GPKG.name())
                .database(parameters.get("database"))
                .poolBuilder();

        return builder.build();
    }

    private static FeatureProviderDataV2 parseFeatureProviderWfs(
            ImmutableFeatureProviderWfsData.Builder builder, Map<String, String> parameters) {

        System.out.println("WFS parameters: " + parameters);

        builder
                .connectionInfoBuilder()
                .uri(URI.create(parameters.get("url")))
                .user(Optional.ofNullable(parameters.get("user")))
                .password(
                        Optional.ofNullable(parameters.get("password"))
                                .map(
                                        pw -> Base64.getEncoder().encodeToString(pw.getBytes(StandardCharsets.UTF_8))));
        return builder.build();
    }

    private static OgcApiDataV2 parseOgcApi(Map<String, String> parameters, LdproxyCfg ldproxyCfg) {
        if (!parameters.containsKey("id")) {
            throw new IllegalArgumentException("No id given");
        }
        if (parameters.get("id").length() < 3) {
            throw new IllegalArgumentException("Id has to be at least 3 characters long");
        }

        String id = parameters.get("id");

        OgcApiDataV2.Builder builder =
                AutoTypes.getBuilder(ldproxyCfg, EntityType.SERVICES, ServiceType.OGC_API).id(id);

        return builder.build();
    }

    private static TileProviderFeaturesData parseTileProvider(
            Map<String, String> parameters, LdproxyCfg ldproxyCfg) {
        if (!parameters.containsKey("id")) {
            throw new IllegalArgumentException("No id given");
        }
        if (parameters.get("id").length() < 3) {
            throw new IllegalArgumentException("Id has to be at least 3 characters long");
        }

        String id = parameters.get("id");

        TileProviderFeaturesData.Builder builder =
                (TileProviderFeaturesData.Builder)
                        AutoTypes.getBuilder(
                                        ldproxyCfg, EntityType.SERVICES, AutoTypes.TileProviderType.FEATURES)
                                .id(id);

        return (TileProviderFeaturesData) builder.build();
    }

    private static Map<String, List<String>> determineTypes(
            Map<String, Object> yamlConfig) {
        // Extract types
        Map<String, Object> yamlTypes = (Map<String, Object>) yamlConfig.get("types");
        Map<String, List<String>> extractedTypes = new HashMap<>();

        if (yamlTypes != null) {
            List<String> values = new ArrayList<>(yamlTypes.keySet());
            extractedTypes.put("", values);
        }

        // Handle missing types
        if (extractedTypes.isEmpty()) {
            extractedTypes.put("", new ArrayList<>(yamlTypes != null ? yamlTypes.keySet() : List.of()));
        }

        return extractedTypes;
    }

    private static Map<String, Boolean> parseTypeObject(String typeObjectString) {
        Map<String, Boolean> typeObject = new HashMap<>();
        if (typeObjectString != null && !typeObjectString.isBlank()) {
            typeObjectString = typeObjectString.replace("{", "").replace("}", "");
            String[] entries = typeObjectString.split(",");
            for (String entry : entries) {
                String[] keyValue = entry.split("=");
                if (keyValue.length == 2) {
                    typeObject.put(keyValue[0].trim(), Boolean.parseBoolean(keyValue[1].trim()));
                }
            }
        }
        return typeObject;
    }

    private static Result generateBasicEntity(Map<String, String> parameters, LdproxyCfg ldproxyCfg) {
        Result result = new Result();

        if (parameters.containsKey("id")) {
            String newId = parameters.get("id");

            if (newId == null || newId.isBlank()) {
                return Result.failure("No id provided in parameters");
            }

            Map<String, Object> minimalConfig = new HashMap<>();
            minimalConfig.put("id", newId);
            minimalConfig.put("enabled", true);

            try {
                File targetFile =
                        new File(
                                ldproxyCfg.getDataDirectory().toFile(),
                                "entities/instances/providers/" + newId + ".yml");
                ldproxyCfg.getObjectMapper().writeValue(targetFile, minimalConfig);

                result.success("Minimal config created successfully");
                result.details("new_files", List.of(targetFile.getPath()));
            } catch (IOException e) {
                return Result.failure("Failed to write minimal config: " + e.getMessage());
            }
        }

        return result;
    }
}
