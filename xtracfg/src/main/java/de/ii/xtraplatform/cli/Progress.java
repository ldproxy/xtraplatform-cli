package de.ii.xtraplatform.cli;

@FunctionalInterface
public interface Progress {
  void update(String progress);
}
