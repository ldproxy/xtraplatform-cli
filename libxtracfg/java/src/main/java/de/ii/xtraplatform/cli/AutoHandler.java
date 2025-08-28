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
import de.ii.xtraplatform.values.domain.Identifier;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.io.File;


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

                Map<String, Object> yamlContent = ldproxyCfg.getObjectMapper().readValue(sourceFile, Map.class);

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

                Map<String, Object> cfgJava = ldproxyCfg.getObjectMapper().readValue(
                        new File(selectedConfig),
                        Map.class
                );

                Map<String, Object> featureProviderTypeAndTypes = determineMissingParameters(parameters, cfgJava);
                types = (Map<String, List<String>>) featureProviderTypeAndTypes.get("types");
            }

            System.out.println("New types: " + types);

            FeatureProviderDataV2 featureProvider = parseFeatureProvider(parameters, ldproxyCfg, types);
            AutoEntityFactory autoFactory =
                    getAutoFactory(
                            ldproxyCfg, EntityType.PROVIDERS.toString(), featureProvider.getEntitySubType());

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
            FeatureProviderDataV2 entityData = autoFactory.generate(featureProvider, types, tracker2);

            OgcApiDataV2 ogcApi = parseOgcApi(parameters, ldproxyCfg);

            AutoEntityFactory autoFactory2 =
                    getAutoFactory(ldproxyCfg, EntityType.SERVICES.toString(), ogcApi.getEntitySubType());

            // get from provider-configuration as well? Seems to work just fine
            Map<String, List<String>> types2 =
                    Map.of("", new ArrayList<>(entityData.getTypes().keySet()));

            System.out.println("types2" + types2); // {=[flurstueck]} instead of {ave=[Flurstueck]}

            OgcApiDataV2 entityData2 = autoFactory2.generate(ogcApi, types2, ignore -> {
            });

            Map<String, Boolean> typeObject = parseTypeObject(parameters.get("typeObject"));

            System.out.println("typeObject: " + typeObject);


            try {
                boolean providerWritten = false;
                boolean serviceWritten = false;

                if (typeObject.getOrDefault("provider", true)) {
                    ldproxyCfg.writeEntity(entityData);
                    providerWritten = true;
                }

                // generate service
                if (typeObject.getOrDefault("service", true)) {
                    ldproxyCfg.writeEntity(entityData2);
                    serviceWritten = true;
                }

                List<String> newFiles = new ArrayList<>();
                if (providerWritten) {
                    newFiles.add(
                            ldproxyCfg
                                    .getDataDirectory()
                                    .relativize(ldproxyCfg.getEntityPath(entityData).normalize())
                                    .toString());
                }
                if (serviceWritten) {
                    newFiles.add(
                            ldproxyCfg
                                    .getDataDirectory()
                                    .relativize(ldproxyCfg.getEntityPath(entityData2).normalize())
                                    .toString());
                }

                result.success("All good");

                if (!newFiles.isEmpty()) {
                    result.details("new_files", newFiles);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                if (Objects.nonNull(e.getMessage())) {
                    result.error(e.getMessage());
                }
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
                    "Invalid featureProviderType: " + featureProviderTypeString + ". Expected one of: "
                            + Arrays.toString(FeatureProviderType.values()), e);
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

    private static Map<String, Object> determineMissingParameters(
            Map<String, String> parameters, Map<String, Object> yamlConfig) {

        Map<String, Object> result = new HashMap<>();

        // Determine featureProviderType
        String featureProviderType = parameters.getOrDefault("featureProviderType", "").trim();
        if (featureProviderType.isEmpty()) {
            String providerSubType = (String) yamlConfig.getOrDefault("providerSubType", "");
            Map<String, Object> connectionInfo = (Map<String, Object>) yamlConfig.get("connectionInfo");
            String dialect = (connectionInfo != null && connectionInfo.containsKey("dialect"))
                    ? (String) connectionInfo.get("dialect")
                    : "";

            if ("GPKG".equalsIgnoreCase(dialect)) {
                featureProviderType = "GPKG";
            } else if ("WFS".equalsIgnoreCase(providerSubType)) {
                featureProviderType = "WFS";
            } else {
                featureProviderType = "PGIS";
            }
        }
        parameters.put("featureProviderType", featureProviderType);

        // If featureProviderType is WFS, extract the URI and set it in parameters
        if ("WFS".equalsIgnoreCase(featureProviderType)) {
            Map<String, Object> connectionInfo = (Map<String, Object>) yamlConfig.get("connectionInfo");
            if (connectionInfo != null && connectionInfo.containsKey("uri")) {
                String uri = (String) connectionInfo.get("uri");
                parameters.put("url", uri);
            }
        }

        // If featureProviderType is PGIS, set additional parameters
        if ("PGIS".equalsIgnoreCase(featureProviderType)) {
            Map<String, Object> connectionInfo = (Map<String, Object>) yamlConfig.get("connectionInfo");
            if (connectionInfo != null) {
                parameters.put("host", (String) connectionInfo.getOrDefault("host", ""));
                parameters.put("database", (String) connectionInfo.getOrDefault("database", ""));
                parameters.put("user", (String) connectionInfo.getOrDefault("user", ""));
                String encodedPassword = (String) connectionInfo.getOrDefault("password", "");
                if (!encodedPassword.isBlank()) {
                    String decodedPassword = new String(Base64.getDecoder().decode(encodedPassword), StandardCharsets.UTF_8);
                    parameters.put("password", decodedPassword);
                }
            }
        }

        // Extract types
        Map<String, Object> yamlTypes = (Map<String, Object>) yamlConfig.get("types");
        Map<String, List<String>> extractedTypes = new HashMap<>();

        if (yamlTypes != null) {
            for (Map.Entry<String, Object> entry : yamlTypes.entrySet()) {
                String key = entry.getKey();
                Map<String, Object> typeDetails = (Map<String, Object>) entry.getValue();
                String sourcePath = (String) typeDetails.get("sourcePath");

                if (sourcePath != null) {
                    sourcePath = sourcePath.startsWith("/") ? sourcePath.substring(1) : sourcePath;

                    String namespace = sourcePath.contains(":") ? sourcePath.split(":")[0] : "";
                    String value = sourcePath.contains(":") ? sourcePath.split(":")[1] : sourcePath;
                    extractedTypes.computeIfAbsent(namespace, k -> new ArrayList<>()).add(value);
                }
            }
        }

        // Handle missing types
        if (extractedTypes.isEmpty()) {
            extractedTypes.put("", new ArrayList<>(yamlTypes != null ? yamlTypes.keySet() : List.of()));
        }

        result.put("types", extractedTypes);

        return result;
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
                File targetFile = new File(ldproxyCfg.getDataDirectory().toFile(), "entities/instances/providers/" + newId + ".yml");
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