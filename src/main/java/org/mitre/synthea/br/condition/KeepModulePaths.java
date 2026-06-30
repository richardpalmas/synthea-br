package org.mitre.synthea.br.condition;

import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mitre.synthea.helpers.Utilities;

/**
 * Resolves keep-module JSON files from {@code src/main/resources/keep_modules/}.
 */
public final class KeepModulePaths {

  private KeepModulePaths() {
  }

  /**
   * Resolve a keep-module path relative to {@code keep_modules/}.
   *
   * @param relativePath path within {@code keep_modules/} (e.g. {@code br/breast_cancer.json})
   * @return filesystem path usable by {@link org.mitre.synthea.engine.Module#loadFile}
   * @throws FileNotFoundException when the module resource does not exist
   */
  public static Path resolveRelative(String relativePath) throws FileNotFoundException {
    Path devPath = Paths.get("src/main/resources/keep_modules").resolve(relativePath);
    if (Files.exists(devPath)) {
      return devPath.toAbsolutePath();
    }

    java.net.URL resource = KeepModulePaths.class.getClassLoader()
        .getResource("keep_modules/" + relativePath);
    if (resource == null) {
      throw new FileNotFoundException(relativePath);
    }

    try {
      URI uri = resource.toURI();
      Utilities.enableReadingURIFromJar(uri);
      return Paths.get(uri);
    } catch (Exception e) {
      throw new FileNotFoundException(relativePath);
    }
  }

  /**
   * Check whether a keep-module resource exists without resolving to a path.
   *
   * @param relativePath path within {@code keep_modules/}
   * @return true when the resource is available
   */
  public static boolean existsRelative(String relativePath) {
    if (Files.exists(Paths.get("src/main/resources/keep_modules").resolve(relativePath))) {
      return true;
    }
    return KeepModulePaths.class.getClassLoader()
        .getResource("keep_modules/" + relativePath) != null;
  }
}
