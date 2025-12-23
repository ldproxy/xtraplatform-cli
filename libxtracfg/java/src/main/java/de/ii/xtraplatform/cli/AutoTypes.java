package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.entities.domain.EntityFactory;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.tiles.domain.TileProviderData;

public class AutoTypes {

  static EntityFactory getFactory(
      LdproxyCfg ldproxyCfg,
      EntityType entityType,
      ProviderType providerType,
      FeatureProviderType featureProviderType) {
    return ldproxyCfg
        .getEntityFactories()
        .get(
            entityType.toString(),
            String.format("%s/%s", providerType.toString(), featureProviderType.toString()));
  }

  static FeatureProviderDataV2.Builder<?> getBuilder(
      LdproxyCfg ldproxyCfg,
      EntityType entityType,
      ProviderType providerType,
      FeatureProviderType featureProviderType) {
    return ((FeatureProviderDataV2.Builder<?>)
            getFactory(ldproxyCfg, entityType, providerType, featureProviderType)
                .emptyDataBuilder())
        .providerType(providerType.toString())
        .providerSubType(featureProviderType.toString());
  }

  static EntityFactory getFactory(
      LdproxyCfg ldproxyCfg, EntityType entityType, ServiceType serviceType) {
    return ldproxyCfg.getEntityFactories().get(entityType.toString(), serviceType.toString());
  }

  static OgcApiDataV2.Builder getBuilder(
      LdproxyCfg ldproxyCfg, EntityType entityType, ServiceType serviceType) {
    return ((OgcApiDataV2.Builder)
        getFactory(ldproxyCfg, entityType, serviceType).emptyDataBuilder());
  }

  static EntityFactory getFactory(
      LdproxyCfg ldproxyCfg,
      EntityType entityType,
      ProviderType providerType,
      TileProviderType tileProviderType) {
    return ldproxyCfg
        .getEntityFactories()
        .get(
            entityType.toString(),
            String.format("%s/%s", providerType.toString(), tileProviderType.toString()));
  }

  static TileProviderData.Builder<?> getBuilder(
      LdproxyCfg ldproxyCfg,
      EntityType entityType,
      ProviderType providerType,
      TileProviderType tileProviderType) {
    return ((TileProviderData.Builder<?>)
            getFactory(ldproxyCfg, entityType, providerType, tileProviderType).emptyDataBuilder())
        .providerType(providerType.toString())
        .providerSubType(tileProviderType.toString());
  }

  enum Type {
    ENTITIES,
    UNKNOWN;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  enum EntityType {
    PROVIDERS,
    SERVICES,
    UNKNOWN;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  enum ProviderType {
    FEATURE,
    TILE,
    UNKNOWN;

    @Override
    public String toString() {
      return name().toUpperCase();
    }
  }

  enum FeatureProviderType {
    PGIS("SQL"),
    GPKG("SQL"),
    WFS("WFS"),
    UNKNOWN("UNKNOWN");

    private final String subType;

    FeatureProviderType(String subType) {
      this.subType = subType;
    }

    @Override
    public String toString() {
      return subType.toUpperCase();
    }
  }

  enum ServiceType {
    OGC_API,
    UNKNOWN;

    @Override
    public String toString() {
      return name().toUpperCase();
    }
  }

  enum TileProviderType {
    FEATURES,
    UNKNOWN;

    @Override
    public String toString() {
      return name().toUpperCase();
    }
  }
}
