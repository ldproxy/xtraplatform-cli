package de.ii.xtraplatform.cli;

import de.ii.xtraplatform.store.domain.Identifier;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;
import shadow.com.networknt.schema.ValidationMessage;

public class Migration extends Messages {
  public Migration(Entities.Type type, Identifier identifier, Path path) {
    super(type, identifier, path);
  }

  public Migration(Entities.Type type, Identifier identifier, Path path, String error) {
    super(type, identifier, path, error);
  }

  @Override
  protected String getSummary() {
    return String.format(
        "Migrations are available for %s configuration: %s", getType().name().toLowerCase(), getPath());
  }

  @Override
  protected boolean isWarning(ValidationMessage vm) {
    return isMigration(vm);
  }

  private static boolean isMigration(ValidationMessage vm) {
    return Objects.equals(vm.getCode(), MIGRATION);
  }

  private static final String MIGRATION = "migration";

  static ValidationMessage migration(String path, String message) {
    return new ValidationMessage.Builder()
        .code(MIGRATION)
        .path(path)
        .arguments(message)
        .format(new MessageFormat("{0}: {1}"))
        .build();
  }
}
