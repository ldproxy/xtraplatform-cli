package de.ii.xtraplatform.cli;

import de.ii.xtraplatform.store.domain.Identifier;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import shadow.com.networknt.schema.ValidationMessage;

public abstract class Messages {

  private final Entities.Type type;
  private final Path path;
  private final Identifier identifier;
  private final Set<ValidationMessage> validationMessages;
  private String error;

  public Messages(Entities.Type type, Identifier identifier, Path path) {
    this(type, identifier, path, null);
  }

  public Messages(Entities.Type type, Identifier identifier, Path path, String error) {
    this.type = type;
    this.identifier = identifier;
    this.path = path;
    this.error = error;
    this.validationMessages = new LinkedHashSet<>();
  }

  public Entities.Type getType() {
    return type;
  }

  public Path getPath() {
    return path;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public Optional<String> getError() {
    return Optional.ofNullable(error);
  }

  protected final void setError(String error) {
    this.error = error;
  }

  public List<String> getErrors() {
    return validationMessages.stream()
        .filter(this::isError)
        .map(this::getMessage)
        .collect(Collectors.toList());
  }

  public List<String> getWarnings() {
    return validationMessages.stream()
        .filter(this::isWarning)
        .map(this::getMessage)
        .collect(Collectors.toList());
  }

  public boolean hasErrors() {
    return !getErrors().isEmpty();
  }

  public boolean hasWarnings() {
    return !getWarnings().isEmpty();
  }

  protected Set<ValidationMessage> getMessages() {
    return validationMessages;
  }

  public void addMessage(ValidationMessage validationMessage) {
    this.validationMessages.add(validationMessage);
  }

  public void addMessages(Set<ValidationMessage> validationMessages) {
    this.validationMessages.addAll(validationMessages);
  }

  public void log(Result result, boolean verbose) {
    if (getError().isPresent()) {
      result.error(String.format("Could not read %s: %s", getPath(), getError().get()));
      return;
    }

    if (hasErrors()) {
      result.error(getSummary());
      getErrors().forEach(message -> result.error("  - " + message));
    } else if (hasWarnings()) {
      result.warning(getSummary());

      if (verbose) {
        getWarnings().forEach(message -> result.warning("  - " + message));
      }
    } else if (verbose) {
      result.success(getSummary());
    }
  }

  public void logErrors(Result result, boolean verbose) {
    if (getError().isPresent()) {
      result.error(String.format("Could not read %s: %s", getPath(), getError().get()));
      return;
    }

    if (hasErrors()) {
      result.error(getSummary());
      if (verbose) {
        getErrors().forEach(message -> result.error("  - " + message));
      }
    }
  }

  protected abstract String getSummary();

  protected abstract boolean isWarning(ValidationMessage vm);

  protected boolean isError(ValidationMessage vm) {
    return !isWarning(vm);
  }

  protected String getMessage(ValidationMessage vm) {
    return vm.getMessage();
  }
}
