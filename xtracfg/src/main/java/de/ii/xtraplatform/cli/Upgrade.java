package de.ii.xtraplatform.cli;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class Upgrade {

  private final Entities.Type type;
  private final Path path;
  private final Map<String, Object> original;
  private final Map<String, Object> upgrade;
  private final String error;

  public Upgrade(
      Entities.Type type, Path path, Map<String, Object> original, Map<String, Object> upgrade) {
    this(type, path, original, upgrade, null);
  }

  public Upgrade(Entities.Type type, Path path, String error) {
    this(type, path, null, null, error);
  }

  private Upgrade(
      Entities.Type type,
      Path path,
      Map<String, Object> original,
      Map<String, Object> upgrade,
      String error) {
    this.type = type;
    this.path = path;
    this.original = original;
    this.upgrade = upgrade;
    this.error = error;
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
}
