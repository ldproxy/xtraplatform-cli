package de.ii.xtraplatform.cli;

import java.util.*;

public class Result {

  enum Status {
    ERROR,
    WARNING,
    SUCCESS,
    INFO,
    CONFIRMATION
  }

  public static Result failure(String message) {
    return new Result(message);
  }

  private final List<Map<String, String>> results;
  private final Optional<String> failure;

  public Result() {
    this.results = new ArrayList<>();
    this.failure = Optional.empty();
  }

  private Result(String failure) {
    this.results = new ArrayList<>();
    this.failure = Optional.of(failure);
  }

  private void add(Status status, String message) {
    results.add(Map.of("status", status.name(), "message", message));
  }

  public void error(String message) {
    add(Status.ERROR, message);
  }

  public void warning(String message) {
    add(Status.WARNING, message);
  }

  public void success(String message) {
    add(Status.SUCCESS, message);
  }

  public void info(String message) {
    add(Status.INFO, message);
  }

  public void confirmation(String message) {
    add(Status.CONFIRMATION, message);
  }

  public boolean isEmpty() {
    return results.isEmpty();
  }

  public boolean has(Status status) {
    return results.stream().anyMatch(r -> Objects.equals(r.get("status"), status.name()));
  }

  public Map<String, Object> asMap() {
    if (failure.isPresent()) {
      return Map.of("error", failure.get());
    }
    return Map.of("results", results);
  }
}
