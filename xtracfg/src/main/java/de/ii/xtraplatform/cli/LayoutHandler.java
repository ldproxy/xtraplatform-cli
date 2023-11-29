package de.ii.xtraplatform.cli;

import de.ii.ldproxy.cfg.Layout;
import de.ii.xtraplatform.blobs.domain.StoreMigration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class LayoutHandler {
  static Result check(Layout layout, boolean verbose) {
    if (Objects.isNull(layout)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();

    for (StoreMigration migration : layout.migrations()) {
      if (migration.isApplicable(null)) {
        if (result.isEmpty()) {
          result.warning(
              String.format(
                  "Migrations are available for directory layout: %s", migration.getSubject()));
        }
        if (verbose) {
          result.info(
              String.format("  - %s: %s", migration.getSubject(), migration.getDescription()));
        }
      }
    }

    return result;
  }

  static Result preUpgrade(Layout layout, boolean verbose) {
    if (Objects.isNull(layout)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();

    for (StoreMigration migration : layout.migrations()) {
      if (migration.isApplicable(null)) {
        result.info("The following directories and files will be moved:");

        migration
            .getPreview()
            .forEach(
                move ->
                    result.info(
                        "  - " + String.format("%s -> %s", move.getKey(), move.getValue())));

        result.info("\nThe following directories will be deleted:");

        migration.getActualCleanups().forEach(cleanup -> result.info("  - " + cleanup.first()));
      }
    }

    if (result.has(Result.Status.INFO)) {
      result.confirmation("Are you sure?");
    }

    return result;
  }

  static Result upgrade(Layout layout, boolean verbose) {
    if (Objects.isNull(layout)) {
      return Result.failure("Not connected to store");
    }

    Result result = new Result();
    final boolean[] upgraded = {false};

    for (StoreMigration migration : layout.migrations()) {
      if (migration.isApplicable(null)) {
        migration
            .migrate()
            .forEach(
                fromTo -> {
                  try {
                    Path from = layout.info().path().resolve(fromTo.getKey());
                    Path to = layout.info().path().resolve(fromTo.getValue());

                    if (Files.isDirectory(to)) {
                      List<Path> children = new ArrayList<>();
                      try (Stream<Path> entries = Files.list(from)) {
                        entries.forEach(children::add);
                      } catch (IOException e) {
                        // ignore
                      }
                      for (Path child : children) {
                        Files.move(child, to.resolve(child.getFileName()));
                        upgraded[0] = true;
                        if (verbose) {
                          result.success(
                              String.format(
                                  "Successfully moved: %s -> %s",
                                  layout.info().path().relativize(child),
                                  layout
                                      .info()
                                      .path()
                                      .relativize(to.resolve(child.getFileName()))));
                        }
                      }
                      Files.delete(from);
                    } else {
                      to.toFile().getParentFile().mkdirs();
                      Files.move(from, to);
                      upgraded[0] = true;
                      if (verbose) {
                        result.success(
                            String.format(
                                "Successfully moved: %s -> %s", fromTo.getKey(), fromTo.getValue()));
                      }
                    }
                  } catch (IOException e) {
                    result.error(
                        String.format(
                            "Could not move: %s -> %s (%s %s)",
                            fromTo.getKey(),
                            fromTo.getValue(),
                            e.getClass().getName(),
                            e.getMessage()));
                  }
                });

        migration
            .getActualCleanups()
            .forEach(
                cleanup -> {
                  Path path = cleanup.first();
                  Path fullPath = layout.info().path().resolve(path);
                  boolean recursive = cleanup.second();

                  if (Files.isDirectory(fullPath)) {
                    if (recursive) {
                      try {
                        Files.walk(fullPath)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);

                        upgraded[0] = true;
                        if (verbose) {
                          result.success(String.format("Directory deleted: %s", path));
                        }
                      } catch (IOException e) {
                        result.error(
                            String.format(
                                "Could not delete recursively: %s (%s %s)",
                                fullPath, e.getClass().getName(), e.getMessage()));
                      }
                    } else {
                      boolean isEmpty = true;
                      try (Stream<Path> entries = Files.list(fullPath)) {
                        isEmpty = entries.findFirst().isEmpty();
                      } catch (IOException e) {
                        // ignore
                      }
                      if (isEmpty) {
                        try {
                          Files.delete(fullPath);
                          upgraded[0] = true;
                          if (verbose) {
                            result.success(String.format("Directory deleted: %s", path));
                          }
                        } catch (IOException e) {
                          result.error(
                              String.format(
                                  "Could not delete: %s (%s %s)",
                                  fullPath, e.getClass().getName(), e.getMessage()));
                        }
                      } else {
                        result.warning(
                            String.format(
                                "Directory is not empty, please cleanup manually: %s", fullPath));
                      }
                    }
                  }
                });
      }
    }

    if (result.has(Result.Status.ERROR) || result.has(Result.Status.WARNING)) {
      result.warning(
          "There were some issues upgrading the layout of "
              + layout.info().path()
              + " that you should try to fix manually");
    } else if (upgraded[0]) {
      result.success("Successfully upgraded directory layout: " + layout.info().path());
    }

    return result;
  }
}
