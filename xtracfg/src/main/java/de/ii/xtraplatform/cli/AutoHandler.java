package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.AutoTypes.EntityType;
import de.ii.xtraplatform.cli.AutoTypes.FeatureProviderType;
import de.ii.xtraplatform.cli.AutoTypes.ProviderType;
import de.ii.xtraplatform.entities.domain.EntityFactory;
import de.ii.xtraplatform.entities.domain.Identifier;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.gml.domain.ImmutableFeatureProviderWfsData;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData;
import java.util.*;
import shadow.com.fasterxml.jackson.core.type.TypeReference;

public class AutoHandler {

  enum SubCommand {
    check,
    analyze,
    generate
  }

  private static final TypeReference<LinkedHashMap<String, Object>> AS_MAP =
      new TypeReference<LinkedHashMap<String, Object>>() {};

  static Result handle(
      Map<String, String> parameters,
      LdproxyCfg ldproxyCfg,
      Optional<String> path,
      boolean verbose,
      boolean debug) {
    SubCommand cmd;
    try {
      cmd = SubCommand.valueOf(parameters.get("subcommand"));
    } catch (Throwable e) {
      return Result.failure("Unknown subcommand for auto: " + parameters.get("subcommand"));
    }

    try {
      switch (cmd) {
        case check:
          return check(parameters, ldproxyCfg, path, verbose, debug);
        case analyze:
          return analyze(parameters, ldproxyCfg, path, verbose, debug);
        case generate:
          return generate(parameters, ldproxyCfg, path, verbose, debug);
      }
    } catch (Throwable e) {
      e.printStackTrace();
      return Result.failure("Unexpected error: " + e.getMessage());
    }

    return Result.failure("WTF");
  }

  static Result check(
      Map<String, String> parameters,
      LdproxyCfg ldproxyCfg,
      Optional<String> path,
      boolean verbose,
      boolean debug) {
    if (Objects.isNull(ldproxyCfg)) {
      return Result.failure("Not connected to store");
    }

    ldproxyCfg.initStore();

    if (!parameters.containsKey("id")) {
      return Result.failure("No id given");
    }

    String id = parameters.get("id");

    Identifier identifier = Identifier.from(id, "providers");

    if (ldproxyCfg.getEntityDataStore().has(identifier)) {
      return Result.failure("A provider with id '" + id + "' already exists");
    }

    Result result = new Result();

    FeatureProviderDataV2 featureProvider = parseFeatureProvider(parameters, ldproxyCfg);

    EntityFactory factory =
        ldproxyCfg
            .getEntityFactories()
            .get(EntityType.PROVIDERS.toString(), featureProvider.getEntitySubType());

    /* TODO
        FeatureProviderDataV2 entityData =
            (FeatureProviderDataV2) factory.autoComplete(featureProvider);
    */

    result.success("All good");

    return result;
  }

  static Result analyze(
      Map<String, String> parameters,
      LdproxyCfg ldproxyCfg,
      Optional<String> path,
      boolean verbose,
      boolean debug) {
    return Result.ok("All good", Map.of("tables", List.of("table1", "table2", "table3")));
  }

  static Result generate(
      Map<String, String> parameters,
      LdproxyCfg ldproxyCfg,
      Optional<String> path,
      boolean verbose,
      boolean debug) {
    return Result.failure("Not implemented yet");
  }

  private static FeatureProviderDataV2 parseFeatureProvider(
      Map<String, String> parameters, LdproxyCfg ldproxyCfg) {
    if (!parameters.containsKey("id")) {
      throw new IllegalArgumentException("No id given");
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
    builder
        .auto(true)
        .connectionInfoBuilder()
        .dialect(ConnectionInfoSql.Dialect.PGIS)
        .host(Optional.ofNullable(parameters.get("host")))
        .database(parameters.get("database"))
        .user(Optional.ofNullable(parameters.get("user")))
        .password(Optional.ofNullable(parameters.get("password")))
        .poolBuilder()
        .maxConnections(0);

    return builder.build();
  }

  private static FeatureProviderDataV2 parseFeatureProviderGpkg(
      ImmutableFeatureProviderSqlData.Builder builder, Map<String, String> parameters) {
    return builder.build();
  }

  private static FeatureProviderDataV2 parseFeatureProviderWfs(
      ImmutableFeatureProviderWfsData.Builder builder, Map<String, String> parameters) {
    return builder.build();
  }
}
