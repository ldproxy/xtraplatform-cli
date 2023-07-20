package de.ii.xtraplatform.cli;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class Upgrade {

  private final Path path;
  private final Map<String, Object> original;
  private final Map<String, Object> upgrade;
  private final String error;

  public Upgrade(Path path, Map<String, Object> original, Map<String, Object> upgrade) {
    this(path, original, upgrade, null);
  }

  public Upgrade(Path path, String error) {
    this(path, null, null, error);
  }

  private Upgrade(Path path, Map<String, Object> original, Map<String, Object> upgrade, String error) {
    this.path = path;
    this.original = original;
    this.upgrade = upgrade;
    this.error = error;
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
