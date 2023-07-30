package de.ii.xtraplatform.cli;

import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import java.nio.file.Path;
import java.util.*;
import shadow.com.networknt.schema.ValidationMessage;

public class Upgrade {

  private final Entities.Type type;
  private final Path path;
  private final Map<String, Object> original;
  private final Map<String, Object> upgrade;
  private final String error;
  private final Set<ValidationMessage> validationMessages;
  private final Map<Identifier, EntityData> additionalEntities;

  public Upgrade(
      Entities.Type type,
      Path path,
      Map<String, Object> original,
      Map<String, Object> upgrade,
      Set<ValidationMessage> validationMessages,
      Map<Identifier, EntityData> additionalEntities) {
    this(type, path, original, upgrade, null, validationMessages, additionalEntities);
  }

  public Upgrade(Entities.Type type, Path path, String error) {
    this(type, path, null, null, error, Set.of(), Map.of());
  }

  private Upgrade(
      Entities.Type type,
      Path path,
      Map<String, Object> original,
      Map<String, Object> upgrade,
      String error,
      Set<ValidationMessage> validationMessages,
      Map<Identifier, EntityData> additionalEntities) {
    this.type = type;
    this.path = path;
    this.original = original;
    this.upgrade = upgrade;
    this.error = error;
    this.validationMessages = validationMessages;
    this.additionalEntities = additionalEntities;
  }

  public Entities.Type getType() {
    return type;
  }

  public Path getPath() {
    return path;
  }

  public Map<String, Object> getOriginal() {
    return original;
  }

  public Optional<Map<String, Object>> getUpgrade() {
    return Optional.ofNullable(upgrade);
  }

  public Optional<String> getError() {
    return Optional.ofNullable(error);
  }

  public Set<ValidationMessage> getValidationMessages() {
    return validationMessages;
  }

  public Map<Identifier, EntityData> getAdditionalEntities() {
    return additionalEntities;
  }
}
