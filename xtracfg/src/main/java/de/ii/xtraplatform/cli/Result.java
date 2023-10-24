package de.ii.xtraplatform.cli;

import java.util.*;

public class Result {

  public enum Status {
    ERROR,
    WARNING,
    SUCCESS,
    INFO,
    CONFIRMATION
  }

  public static Result empty() {
    return new Result();
  }

  public static Result failure(String message) {
    return new Result(message);
  }

  public static Result ok(String message) {
    Result result = new Result();
    result.add(Status.INFO, message);
    return result;
  }

  public static Result ok(String message, Map<String, Object> details) {
    Result result = new Result();
    result.add(Status.INFO, message);
    result.details.putAll(details);
    return result;
  }

  private final List<Map<String, String>> results;
  private final Map<String, Object> details;
  private final Optional<String> failure;

  public Result() {
    this.results = new ArrayList<>();
    this.details = new LinkedHashMap<>();
    this.failure = Optional.empty();
  }

  private Result(String failure) {
    this.results = new ArrayList<>();
    this.details = new LinkedHashMap<>();
    this.failure = Optional.ofNullable(failure).or(() -> Optional.of("unknown error"));
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

  public void details(String key, Object value) {
    details.put(key, value);
  }

  public boolean isEmpty() {
    return results.isEmpty() && failure.isEmpty();
  }

  public boolean isFailure() {
    return failure.isPresent();
  }

  public boolean has(Status status) {
    return results.stream().anyMatch(r -> Objects.equals(r.get("status"), status.name()));
  }

  public Map<String, Object> asMap() {
    if (failure.isPresent()) {
      return Map.of("error", failure.get());
    }
    return Map.of("results", results, "details", details);
  }

  public Result merge(Result other) {
    if (this.failure.isPresent()) {
      return this;
    }
    if (other.failure.isPresent()) {
      return other;
    }

    Result merged = new Result();

    merged.results.addAll(this.results);
    merged.results.addAll(other.results);

    merged.details.putAll(this.details);
    merged.details.putAll(other.details);

    return merged;
  }
}
