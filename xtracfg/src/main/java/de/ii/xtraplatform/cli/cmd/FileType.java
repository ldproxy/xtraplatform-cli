package de.ii.xtraplatform.cli.cmd;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.xtraplatform.cli.Result;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import shadow.com.google.common.io.Files;

public class FileType extends Common<LdproxyCfg> {

  private static final List<String> CONTENT_TYPES =
      List.of("defaults", "entities", "instances", "overrides");
  private static final List<String> ENTITY_TYPES = List.of("providers", "services");

  public final Path fullPath;

  public final Path path;
  public final String fileName;
  public final String fileExtension;

  public FileType(Map<String, Object> parameters) {
    super(parameters);
    this.fullPath = Path.of(string(parameters, "path"));

    String file = Optional.ofNullable(fullPath.getFileName()).map(Path::toString).orElse("");

    this.path = fullPath.getParent();
    this.fileName = Files.getNameWithoutExtension(file);
    this.fileExtension = Files.getFileExtension(file);
  }

  @Override
  public Result run(LdproxyCfg ldproxyCfg) {

    System.out.println(
        "FILE_TYPE " + fullPath + " - " + path + " - " + fileName + " - " + fileExtension);

    if (Objects.isNull(path)
        || path.getNameCount() < 2
        || !Objects.equals(fileExtension, "yml")
        || (!path.startsWith("entities") && !path.startsWith("store"))) {
      return Result.empty();
    }

    String type = path.getName(1).toString();

    System.out.println(
        "FILE_TYPE "
            + fullPath
            + " - "
            + path
            + " - "
            + fileName
            + " - "
            + fileExtension
            + " - "
            + type);

    // TODO: multi-file overrides
    if (path.getNameCount() >= 3
        && CONTENT_TYPES.contains(type)
        && ENTITY_TYPES.contains(path.getFileName().toString())) {
      return found(path.getFileName().toString());
    }

    // TODO: multi-file defaults
    if (Objects.equals(type, "defaults")) {
      if (ENTITY_TYPES.contains(fileName)) {
        return found(fileName);
      }
    }

    return Result.empty();
  }

  private Result found(String entityType) {
    System.out.println("FOUND entities/" + entityType);
    return Result.ok(
        "found", Map.of("path", fullPath.toString(), "fileType", "entities/" + entityType));
  }
}
