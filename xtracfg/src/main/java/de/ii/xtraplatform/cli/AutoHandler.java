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
import shadow.com.fasterxml.jackson.core.type.TypeReference;
import shadow.com.google.common.base.Splitter;
import shadow.com.google.common.collect.ImmutableMap;

public class AutoHandler {

  enum SubCommand {
    check,
    analyze,
    generate
  }

  private static final TypeReference<LinkedHashMap<String, Object>> AS_MAP =
      new TypeReference<LinkedHashMap<String, Object>>() {};

  public static Result handle(
      Map<String, String> parameters,
      LdproxyCfg ldproxyCfg,
      Optional<String> path,
      boolean verbose,
      boolean debug,
      Consumer<Result> tracker) {
    SubCommand cmd;
    try {
      cmd = SubCommand.valueOf(parameters.get("subcommand"));
    } catch (Throwable e) {
      return Result.failure("Unknown subcommand for auto: " + parameters.get("subcommand"));
    }

    Result result = preCheck(parameters, ldproxyCfg);

    if (result.isFailure()) {
      return result;
    }

    try {
      switch (cmd) {
        case check:
          return check(parameters, ldproxyCfg, path, verbose, debug);
        case analyze:
          return analyze(parameters, ldproxyCfg, path, verbose, debug);
        case generate:
          return generate(parameters, ldproxyCfg, path, verbose, debug, tracker);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      return Result.failure("Unexpected error: " + e.getMessage());
    }

    return Result.failure("WTF");
  }

  static Result preCheck(Map<String, String> parameters, LdproxyCfg ldproxyCfg) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    ldproxyCfg.initStore();

    if (!parameters.containsKey("id")) {
      return Result.failure("No id given");
    }

    // TODO: min 3 chars

    String id = parameters.get("id");

    Identifier identifier = Identifier.from(id, "providers");

    if (ldproxyCfg.getEntityDataStore().has(identifier)) {
      return Result.failure("A provider with id '" + id + "' already exists");
    }

    return Result.empty();
  }

  static Result check(
      Map<String, String> parameters,
      LdproxyCfg ldproxyCfg,
      Optional<String> path,
      boolean verbose,
      boolean debug) {

    // TODO: check connectionInfo

    return Result.ok("All good");
  }

  static Result analyze(
      Map<String, String> parameters,
      LdproxyCfg ldproxyCfg,
      Optional<String> path,
      boolean verbose,
      boolean debug) {
    Result result = new Result();

    try {
      FeatureProviderDataV2 featureProvider = parseFeatureProvider(parameters, ldproxyCfg);

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

  static Result generate(
      Map<String, String> parameters,
      LdproxyCfg ldproxyCfg,
      Optional<String> path,
      boolean verbose,
      boolean debug,
      Consumer<Result> tracker) {
    Result result = new Result();

    try {
      FeatureProviderDataV2 featureProvider = parseFeatureProvider(parameters, ldproxyCfg);

      AutoEntityFactory autoFactory =
          getAutoFactory(
              ldproxyCfg, EntityType.PROVIDERS.toString(), featureProvider.getEntitySubType());

      Map<String, List<String>> types =
          parameters.containsKey("types") ? parseTypes(parameters.get("types")) : Map.of();

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

            tracker.accept(Result.ok("progress", details));
          };

      FeatureProviderDataV2 entityData = autoFactory.generate(featureProvider, types, tracker2);

      ldproxyCfg.writeEntity(entityData);

      // generate service
      OgcApiDataV2 ogcApi = parseOgcApi(parameters, ldproxyCfg);

      AutoEntityFactory autoFactory2 =
          getAutoFactory(ldproxyCfg, EntityType.SERVICES.toString(), ogcApi.getEntitySubType());

      Map<String, List<String>> types2 =
          Map.of("", new ArrayList<>(entityData.getTypes().keySet()));

      OgcApiDataV2 entityData2 = autoFactory2.generate(ogcApi, types2, ignore -> {});

      ldproxyCfg.writeEntity(entityData2);

      result.success("All good");
      result.details(
          "new_files",
          List.of(
              ldproxyCfg
                  .getDataDirectory()
                  .relativize(ldproxyCfg.getEntityPath(entityData).normalize())
                  .toString(),
              ldproxyCfg
                  .getDataDirectory()
                  .relativize(ldproxyCfg.getEntityPath(entityData2).normalize())
                  .toString()));
    } catch (Throwable e) {
      e.printStackTrace();
      if (Objects.nonNull(e.getMessage())) {
        result.error(e.getMessage());
      }
    }

    return result;
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
      Map<String, String> parameters, LdproxyCfg ldproxyCfg) {
    if (!parameters.containsKey("id")) {
      throw new IllegalArgumentException("No id given");
    }
    if (parameters.get("id").length() < 3) {
      throw new IllegalArgumentException("Id has to be at least 3 characters long");
    }

    String id = parameters.get("id");

    FeatureProviderType featureProviderType =
        FeatureProviderType.valueOf(parameters.get("featureProviderType"));

    if (featureProviderType == FeatureProviderType.UNKNOWN) {
      throw new IllegalArgumentException(
          "Unknown featureProviderType: " + parameters.get("featureProviderType"));
    }

    FeatureProviderDataV2.Builder<?> builder =
        AutoTypes.getBuilder(
                ldproxyCfg, EntityType.PROVIDERS, ProviderType.FEATURE, featureProviderType)
            .id(id);

    switch (featureProviderType) {
      case PGIS:
        return parseFeatureProviderPgis(
            (ImmutableFeatureProviderSqlData.Builder) builder, parameters);
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
      ImmutableFeatureProviderSqlData.Builder builder, Map<String, String> parameters) {
    Set<String> schemas =
        parameters.containsKey("types") ? parseTypes(parameters.get("types")).keySet() : Set.of();

    builder
        .connectionInfoBuilder()
        .dialect(ConnectionInfoSql.Dialect.PGIS)
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
        .dialect(ConnectionInfoSql.Dialect.GPKG)
        .database(parameters.get("database"))
        .poolBuilder();

    return builder.build();
  }

  private static FeatureProviderDataV2 parseFeatureProviderWfs(
      ImmutableFeatureProviderWfsData.Builder builder, Map<String, String> parameters) {

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

  private static final Splitter.MapSplitter SCHEMA_SPLITTER =
      Splitter.on('|').trimResults().omitEmptyStrings().withKeyValueSeparator(':');
  private static final Splitter TABLE_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private static Map<String, List<String>> parseTypes(String types) {
    return SCHEMA_SPLITTER.split(types).entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), TABLE_SPLITTER.splitToList(entry.getValue())))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
