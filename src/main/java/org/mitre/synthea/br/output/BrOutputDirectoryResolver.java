package org.mitre.synthea.br.output;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.mitre.synthea.helpers.Config;

/**
 * Resolves a timestamped run directory under {@code exporter.baseDirectory} so each generation
 * preserves prior cohort output instead of overwriting it.
 */
public final class BrOutputDirectoryResolver {

  private static final String TIMESTAMPED_RUNS_KEY = "br.output.timestamped_runs";
  private static final String BASE_DIRECTORY_KEY = "exporter.baseDirectory";
  private static final String DEFAULT_BASE = "./output/";
  private static final DateTimeFormatter RUN_FOLDER_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss", Locale.ROOT);

  private BrOutputDirectoryResolver() {
  }

  /**
   * When {@code br.output.timestamped_runs} is enabled (default), appends a timestamp subfolder
   * to the configured export root and updates {@code exporter.baseDirectory} for this run.
   *
   * @return effective output directory path used for the run
   * @throws IOException when the run directory cannot be created
   */
  public static String prepareRunDirectory() throws IOException {
    if (!Config.getAsBoolean(TIMESTAMPED_RUNS_KEY, true)) {
      return normalizeTrailingSlash(Config.get(BASE_DIRECTORY_KEY, DEFAULT_BASE));
    }

    Path rootPath = toRootPath(Config.get(BASE_DIRECTORY_KEY, DEFAULT_BASE));
    Path runDirectory = resolveUniqueRunPath(rootPath, Instant.now());
    Files.createDirectories(runDirectory);
    String resolved = normalizeTrailingSlash(runDirectory.toString());
    Config.set(BASE_DIRECTORY_KEY, resolved);
    System.out.println("Diretório de saída: " + resolved);
    return resolved;
  }

  private static Path toRootPath(String root) {
    if (root == null || root.isEmpty()) {
      return Paths.get(DEFAULT_BASE).toAbsolutePath().normalize();
    }
    return Paths.get(root).toAbsolutePath().normalize();
  }

  private static Path resolveUniqueRunPath(Path root, Instant instant) {
    ZoneId zone = ZoneId.systemDefault();
    String baseName = RUN_FOLDER_FORMAT.format(instant.atZone(zone));
    Path candidate = root.resolve(baseName);
    if (!Files.exists(candidate)) {
      return candidate;
    }
    DateTimeFormatter withMillis = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss-SSS", Locale.ROOT);
    return root.resolve(withMillis.format(instant.atZone(zone)));
  }

  private static String normalizeTrailingSlash(String path) {
    if (path == null || path.isEmpty()) {
      return DEFAULT_BASE;
    }
    String normalized = Paths.get(path).toAbsolutePath().normalize().toString();
    if (normalized.endsWith("/") || normalized.endsWith("\\")) {
      return normalized;
    }
    return normalized + File.separator;
  }
}
